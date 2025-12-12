package rest;

import dto.AuthResponseDTO;
import dto.UserRequestDTO;
import ejb.AuthBean;
import entities.User;
import exceptions.AuthException;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import utils.auth.JWTUtils;
import utils.rate.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {
    private static final Logger logger = Logger.getLogger(AuthController.class.getName());
    
    @EJB
    private AuthBean authBean;
    
    @EJB
    private AuthRateLimiter authRateLimiter;
    
    @Context
    private HttpServletRequest request;
    
    private String getClientIdentifier() {
        // Используем IP адрес клиента
        String ip = request.getRemoteAddr();
        // Если есть X-Forwarded-For, используем его
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ip = forwardedFor.split(",")[0].trim();
        }
        return ip;
    }

    private NewCookie createRefreshTokenCookie(String refreshToken) {
        return new NewCookie(
                "refreshToken", refreshToken,
                "/", null, null,
                (int) (JWTUtils.REFRESH_EXP_MS / 1000),
                true, true
        );
    }

    @POST
    @Path("/register")
    public Response register(UserRequestDTO req) {
        logger.info("Received registration request: " + req);
        
        // Проверка rate limit
        String clientId = getClientIdentifier();
        if (authRateLimiter.isRateLimited(clientId)) {
            long waitTime = authRateLimiter.getTimeUntilNextRequest(clientId);
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity("Too Many Requests. Maximum 3 requests per 30 seconds allowed. Please wait " + (waitTime / 1000) + " seconds.")
                    .build();
        }
        
        try {
            User user = authBean.register(req.username(), req.password());

            String accessToken = JWTUtils.generateAccessToken(user.getUsername());
            String refreshToken = JWTUtils.generateRefreshToken(user.getUsername());

            logger.info("User registered successfully: " + user.getUsername());
            return Response.status(Response.Status.CREATED)
                    .cookie(createRefreshTokenCookie(refreshToken))
                    .entity(new AuthResponseDTO(accessToken))
                    .build();
        } catch (AuthException e) {
            logger.log(Level.WARNING, "Registration failed: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/login")
    public Response login(UserRequestDTO req) {
        // Проверка rate limit
        String clientId = getClientIdentifier();
        if (authRateLimiter.isRateLimited(clientId)) {
            long waitTime = authRateLimiter.getTimeUntilNextRequest(clientId);
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity("Too Many Requests. Maximum 3 requests per 30 seconds allowed. Please wait " + (waitTime / 1000) + " seconds.")
                    .build();
        }
        
        try {
            Optional<User> userOpt = authBean.authenticate(req.username(), req.password());
            if (userOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid credentials")
                        .build();
            }

            String username = userOpt.get().getUsername();
            String accessToken = JWTUtils.generateAccessToken(username);
            String refreshToken = JWTUtils.generateRefreshToken(username);

            return Response.ok(new AuthResponseDTO(accessToken)).cookie(createRefreshTokenCookie(refreshToken)).build();
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        }
        }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam("refreshToken") String refreshToken) {
        if (refreshToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No refresh token").build();
        }

        String username = JWTUtils.validateToken(refreshToken);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid refresh token").build();
        }

        String newAccessToken = JWTUtils.generateAccessToken(username);
        String newRefreshToken = JWTUtils.generateRefreshToken(username);

        return Response.ok(new AuthResponseDTO(newAccessToken)).cookie(createRefreshTokenCookie(newRefreshToken)).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie deleteCookie = new NewCookie("refreshToken", "", "/", null, null, 0, true, true);
        return Response.noContent().cookie(deleteCookie).build();
    }
}
