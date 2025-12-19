package rest;

import dto.*;
import entities.User;
import exceptions.RateLimitException;
import filters.AuthContext;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import services.PointProcessingService;
import utils.rate.RateLimitChecker;

import java.util.List;

@Path("/points")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PointController {
    @EJB
    private PointProcessingService pointProcessingService;
    
    @EJB
    private RateLimitChecker rateLimitChecker;
    
    @Context
    private HttpServletRequest httpRequest;

    @POST
    @Path("/form")
    public Response checkPointsFromForm(PointsRequestDTO req) throws RateLimitException {
        User user = AuthContext.getAuthenticatedUser(httpRequest);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not authenticated").build();
        }
        
        // Проверка rate limit
        rateLimitChecker.checkRateLimit(user.getUsername());
        
        List<HistoryEntryDTO> entriesDTO = pointProcessingService.processPointsFromForm(req.points(), user);
        
        return Response.ok(entriesDTO).build();
    }

    @POST
    @Path("/graph")
    public Response checkPointsFromGraph(PointsGraphRequestDTO req) throws RateLimitException {
        User user = AuthContext.getAuthenticatedUser(httpRequest);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not authenticated").build();
        }
        
        // Проверка rate limit
        rateLimitChecker.checkRateLimit(user.getUsername());
        
        List<HistoryEntryDTO> entriesDTO = pointProcessingService.processPointsFromGraph(
                req.points(), req.graphXMin(), req.graphXMax(), req.graphYMin(), req.graphYMax(), user);
        
        return Response.ok(entriesDTO).build();
    }
}
