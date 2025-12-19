package rest;

import dto.HistoryEntryDTO;
import dto.HistoryRequestDTO;
import dto.HistoryResponseDTO;
import dto.PollRequestDTO;
import ejb.HistoryBean;
import entities.User;
import exceptions.DatabaseException;
import filters.AuthContext;
import jakarta.ejb.EJB;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

@Path("/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoryController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE = 10_000;
    
    @EJB
    private HistoryBean historyBean;
    
    @Context
    private HttpServletRequest httpRequest;

    @POST
    public Response getHistory(HistoryRequestDTO req) throws IllegalArgumentException {
        User user = AuthContext.getAuthenticatedUser(httpRequest);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not authenticated").build();
        }
        
        int pageSize = Math.min(Math.max(req.pageSize(), MIN_PAGE_SIZE), MAX_PAGE_SIZE);
        int page = Math.max(req.page(), 1);
        
        if (page > MAX_PAGE) {
            throw new IllegalArgumentException("Page cannot exceed " + MAX_PAGE);
        }
        
        int offset = req.getOffset();
        int limit = req.getLimit();
        
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
    public Response deleteHistory(@PathParam("username") String username) {
        User currentUser = AuthContext.getAuthenticatedUser(httpRequest);
        if (currentUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not authenticated").build();
        }
        
        if (!currentUser.getUsername().equals(username)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("You can only delete your own history").build();
        }
        
        historyBean.deleteByUsername(username);
        return Response.noContent().build();
    }
    
    @POST
    @Path("/poll")
    public Response pollNewPoints(PollRequestDTO req) {
        User user = AuthContext.getAuthenticatedUser(httpRequest);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not authenticated").build();
        }
        
        try {
            
            LocalDateTime lastCreatedAt = req.lastCreatedAt();
            Long lastId = req.lastId();
            int timeoutSeconds = Math.min(req.timeoutSeconds() != null ? req.timeoutSeconds() : 30, 30);
            
            long startTime = System.currentTimeMillis();
            long timeoutMs = timeoutSeconds * 1000L;
            
            // Long-polling: проверяем наличие новых точек каждые 500мс
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                List<HistoryEntryDTO> newPoints = historyBean.getNewPoints(lastCreatedAt, lastId, user.getUsername());
                
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
            
        } catch (PersistenceException | IllegalArgumentException e) {
            // Исключения будут обработаны GlobalExceptionMapper
            throw e;
        }
    }
}
