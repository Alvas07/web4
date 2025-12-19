package rest;

import dto.HistoryEntryDTO;
import dto.HistoryRequestDTO;
import dto.HistoryResponseDTO;
import dto.PollRequestDTO;
import ejb.AuthBean;
import ejb.HistoryBean;
import entities.User;
import exceptions.AuthException;
import exceptions.DatabaseException;
import jakarta.ejb.EJB;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import utils.auth.JWTUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Path("/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoryController {
    private static final int MAX_OFFSET = 10_000;
    private static final int MAX_LIMIT = 100;
    private static final int MIN_LIMIT = 1;
    
    @EJB
    private HistoryBean historyBean;
    
    @EJB
    private AuthBean authBean;
    
    private User authorize(String authHeader) throws AuthException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        String username = JWTUtils.validateToken(token);
        if (username == null) {
            throw new AuthException("Invalid access token");
        }

        Optional<User> user = authBean.findUserByUsername(username);
        if (user.isEmpty()) {
            throw new AuthException("User not found");
        }

        return user.get();
    }

    @POST
    public Response getHistory(HistoryRequestDTO req, @HeaderParam("Authorization") String authHeader) {
        try {
            authorize(authHeader);
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage()).build();
        }
        
        int limit = Math.min(Math.max(req.limit(), MIN_LIMIT), MAX_LIMIT);
        int offset = Math.max(req.offset(), 0);
        
        if (offset > MAX_OFFSET) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Offset cannot exceed " + MAX_OFFSET).build();
        }
        
        List<HistoryEntryDTO> history = historyBean.getHistoryWithOffset(offset, limit);
        Long totalCount = null;
        
        if (req.needTotalCount()) {
            totalCount = historyBean.getTotalCount();
        }
        
        HistoryResponseDTO response = new HistoryResponseDTO(history, totalCount);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{username}")
    public Response deleteHistory(@PathParam("username") String username, @HeaderParam("Authorization") String authHeader) {
        try {
            User currentUser = authorize(authHeader);
            
            if (!currentUser.getUsername().equals(username)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("You can only delete your own history").build();
            }
            
            historyBean.deleteByUsername(username);
            return Response.noContent().build();
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage()).build();
        }
    }
    
    @POST
    @Path("/poll")
    public Response pollNewPoints(PollRequestDTO req, @HeaderParam("Authorization") String authHeader) {
        try {
            User user = authorize(authHeader);
            
            LocalDateTime lastCreatedAt = req.lastCreatedAt();
            Long lastId = req.lastId();
            int timeoutSeconds = Math.min(req.timeoutSeconds() != null ? req.timeoutSeconds() : 30, 30);
            
            long startTime = System.currentTimeMillis();
            long timeoutMs = timeoutSeconds * 1000L;
            
            List<HistoryEntryDTO> newPoints = null;
            
            // Long-polling: проверяем наличие новых точек каждые 500мс
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                newPoints = historyBean.getNewPoints(lastCreatedAt, lastId, user.getUsername());
                
                // Если нашли новые точки, возвращаем их сразу
                if (!newPoints.isEmpty()) {
                    return Response.ok(newPoints).build();
                }
                
                // Ждем 500мс перед следующей проверкой
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Polling interrupted").build();
                }
            }
            
            // Если таймаут истек, возвращаем пустой список
            return Response.ok(List.<HistoryEntryDTO>of()).build();
            
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage()).build();
        } catch (PersistenceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Database error during polling: " + e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid request parameters: " + e.getMessage()).build();
        }
    }
}
