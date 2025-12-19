package utils.rate;

import exceptions.RateLimitException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class RateLimitChecker {
    @EJB
    private RateLimiter rateLimiter;

    public void checkAuthRateLimit(String identifier) throws RateLimitException {
        if (rateLimiter.isAuthRateLimited(identifier)) {
            long waitTime = rateLimiter.getTimeUntilNextAuthRequest(identifier);
            throw new RateLimitException(
                "Too Many Requests. Maximum 3 requests per 30 seconds allowed. Please wait " + (waitTime / 1000) + " seconds.",
                waitTime
            );
        }
    }

    public void checkRateLimit(String identifier) throws RateLimitException {
        if (rateLimiter.isRateLimited(identifier)) {
            throw new RateLimitException(
                "Too Many Requests. Maximum 10 requests per second allowed.",
                1000
            );
        }
    }
}


