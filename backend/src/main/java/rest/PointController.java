package rest;

import dto.*;
import ejb.AuthBean;
import ejb.HistoryBean;
import ejb.PointCheckBean;
import entities.HistoryEntry;
import entities.Point;
import entities.User;
import exceptions.AuthException;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import utils.auth.JWTUtils;
import utils.rate.RateLimiter;

import java.util.List;
import java.util.Optional;

@Path("/points")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PointController {
    @EJB
    private PointCheckBean pointCheckBean;

    @EJB
    private HistoryBean historyBean;

    @EJB
    private AuthBean authBean;
    
    @EJB
    private RateLimiter rateLimiter;

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
    @Path("/form")
    public Response checkPointsFromForm(PointsRequestDTO req, @HeaderParam("Authorization") String authHeader) {
        try {
            long startTime = System.nanoTime();
            User user = authorize(authHeader);
            
            // Проверка rate limit
            if (rateLimiter.isRateLimited(user.getUsername())) {
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .entity("Too Many Requests. Maximum 10 requests per second allowed.")
                        .build();
            }
            List<CheckResultDTO> results = pointCheckBean.checkPointsFromForm(req.points());
            double execTime = (System.nanoTime() - startTime) / 1e6;
            List<HistoryEntry> entries = results.stream()
                    .map(r -> {
                        HistoryEntry entry = new HistoryEntry(
                                new Point(r.point().x(), r.point().y(), r.point().r()),
                                r.hit(),
                                user
                        );
                        entry.setExecTime(execTime);
                        return entry;
                    })
                    .toList();
            historyBean.save(entries);
            List<HistoryEntryDTO> entriesDTO = entries.stream()
                    .map(e -> new HistoryEntryDTO(
                            new PointDTO(e.getPoint().getX(), e.getPoint().getY(), e.getPoint().getR()),
                            e.isHit(),
                            user.getUsername(),
                            e.getCreatedAt(),
                            e.getExecTime()
                    ))
                    .toList();
            
            return Response.ok(entriesDTO).build();
        } catch (AuthException | IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/graph")
    public Response checkPointsFromGraph(PointsGraphRequestDTO req, @HeaderParam("Authorization") String authHeader) {
        try {
            long startTime = System.nanoTime();
            User user = authorize(authHeader);
            
            // Проверка rate limit
            if (rateLimiter.isRateLimited(user.getUsername())) {
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .entity("Too Many Requests. Maximum 10 requests per second allowed.")
                        .build();
            }
            List<CheckResultDTO> results = pointCheckBean.checkPointsFromGraph(req.points(), req.graphXMin(), req.graphXMax(), req.graphYMin(), req.graphYMax());
            double execTime = (System.nanoTime() - startTime) / 1e6;
            List<HistoryEntry> entries = results.stream()
                    .map(r -> {
                        HistoryEntry entry = new HistoryEntry(
                                new Point(r.point().x(), r.point().y(), r.point().r()),
                                r.hit(),
                                user
                        );
                        entry.setExecTime(execTime);
                        return entry;
                    })
                    .toList();
            historyBean.save(entries);
            List<HistoryEntryDTO> entriesDTO = entries.stream()
                    .map(e -> new HistoryEntryDTO(
                            new PointDTO(e.getPoint().getX(), e.getPoint().getY(), e.getPoint().getR()),
                            e.isHit(),
                            user.getUsername(),
                            e.getCreatedAt(),
                            e.getExecTime()
                    ))
                    .toList();
            
            return Response.ok(entriesDTO).build();
        } catch (AuthException | IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
