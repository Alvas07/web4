package filters;

import entities.User;
import ejb.AuthBean;
import exceptions.AuthException;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import utils.auth.JWTUtils;

import java.security.Principal;
import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
    
    @EJB
    private AuthBean authBean;
    
    @Context
    private HttpServletRequest httpRequest;
    
    public static final String USER_PROPERTY = "authenticatedUser";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Пропускаем публичные эндпоинты
        String path = requestContext.getUriInfo().getPath();
        // Убираем ведущий слэш и проверяем начало пути
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.equals("auth/register") || normalizedPath.equals("auth/login") || 
            normalizedPath.equals("auth/refresh") || normalizedPath.equals("auth/logout") ||
            normalizedPath.startsWith("auth/register") || normalizedPath.startsWith("auth/login") ||
            normalizedPath.startsWith("auth/refresh") || normalizedPath.startsWith("auth/logout")) {
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid authorization header")
                    .build()
            );
            return;
        }

        String token = authHeader.substring(7);
        String username = JWTUtils.validateToken(token);
        
        if (username == null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid access token")
                    .build()
            );
            return;
        }

        Optional<User> userOpt = authBean.findUserByUsername(username);
        if (userOpt.isEmpty()) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found")
                    .build()
            );
            return;
        }

        User user = userOpt.get();
        requestContext.setProperty(USER_PROPERTY, user);
        
        // Также сохраняем в HttpServletRequest для доступа через @Context
        if (httpRequest != null) {
            httpRequest.setAttribute(USER_PROPERTY, user);
        }
        
        // Устанавливаем SecurityContext для совместимости
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> user.getUsername();
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return requestContext.getSecurityContext().isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });
    }
}

