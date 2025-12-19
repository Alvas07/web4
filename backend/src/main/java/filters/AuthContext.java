package filters;

import entities.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

public class AuthContext {
    public static User getAuthenticatedUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (User) request.getAttribute(AuthFilter.USER_PROPERTY);
    }
    
    public static User getAuthenticatedUser(ContainerRequestContext requestContext) {
        if (requestContext == null) {
            return null;
        }
        return (User) requestContext.getProperty(AuthFilter.USER_PROPERTY);
    }
}

