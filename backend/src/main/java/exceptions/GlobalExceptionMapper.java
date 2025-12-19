package exceptions;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        // Обработка специфичных исключений
        if (exception instanceof AuthException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(exception.getMessage())
                    .build();
        }

        if (exception instanceof RateLimitException) {
            RateLimitException rateLimitEx = (RateLimitException) exception;
            long waitTimeSeconds = rateLimitEx.getWaitTimeMs() / 1000;
            String message = rateLimitEx.getMessage() != null
                    ? rateLimitEx.getMessage()
                    : "Too Many Requests. Please wait " + waitTimeSeconds + " seconds.";
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(message)
                    .build();
        }

        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(exception.getMessage())
                    .build();
        }

        if (exception instanceof PersistenceException) {
            logger.log(Level.SEVERE, "Database error", exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Database error: " + exception.getMessage())
                    .build();
        }

        if (exception instanceof DatabaseException) {
            logger.log(Level.SEVERE, "Database error", exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exception.getMessage())
                    .build();
        }

        // Обработка всех остальных исключений
        logger.log(Level.SEVERE, "Unexpected error", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Internal server error: " + exception.getMessage())
                .build();
    }
}


