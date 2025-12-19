package utils.rate;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Startup
public class RateLimiter {
    // Default configuration for general API rate limiting
    private static final int DEFAULT_MAX_REQUESTS = 10;
    private static final long DEFAULT_WINDOW_MS = 1000;
    
    // Auth-specific configuration
    private static final int AUTH_MAX_REQUESTS = 3;
    private static final long AUTH_WINDOW_MS = 30000;
    
    private final Map<String, RequestWindow> defaultRequests = new ConcurrentHashMap<>();
    private final Map<String, RequestWindow> authRequests = new ConcurrentHashMap<>();
    
    // Default rate limiter (10 requests per second)
    public boolean isRateLimited(String identifier) {
        return isRateLimited(identifier, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MS, defaultRequests);
    }
    
    // Auth rate limiter (3 requests per 30 seconds)
    public boolean isAuthRateLimited(String identifier) {
        return isRateLimited(identifier, AUTH_MAX_REQUESTS, AUTH_WINDOW_MS, authRequests);
    }
    
    public long getTimeUntilNextAuthRequest(String identifier) {
        return getTimeUntilNextRequest(identifier, AUTH_MAX_REQUESTS, AUTH_WINDOW_MS, authRequests);
    }
    
    // Generic rate limiting method
    private boolean isRateLimited(String identifier, int maxRequests, long windowMs, Map<String, RequestWindow> requests) {
        long currentTime = System.currentTimeMillis();
        RequestWindow window = requests.computeIfAbsent(identifier, k -> new RequestWindow());
        
        // Очищаем старые запросы (старше окна)
        window.cleanOldRequests(currentTime, windowMs);
        
        // Проверяем лимит
        if (window.getRequestCount() >= maxRequests) {
            return true; // Лимит превышен
        }
        
        // Добавляем новый запрос
        window.addRequest(currentTime);
        return false;
    }
    
    private long getTimeUntilNextRequest(String identifier, int maxRequests, long windowMs, Map<String, RequestWindow> requests) {
        long currentTime = System.currentTimeMillis();
        RequestWindow window = requests.get(identifier);
        
        if (window == null) {
            return 0;
        }
        
        window.cleanOldRequests(currentTime, windowMs);
        
        if (window.getRequestCount() < maxRequests) {
            return 0;
        }
        
        // Вычисляем время до истечения окна
        long timeElapsed = currentTime - window.getWindowStart();
        long timeRemaining = windowMs - timeElapsed;
        return Math.max(0, timeRemaining);
    }
    
    private static class RequestWindow {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        
        public void cleanOldRequests(long currentTime, long windowMs) {
            // Если прошло время окна, сбрасываем счетчик
            if (currentTime - windowStart >= windowMs) {
                requestCount.set(0);
                windowStart = currentTime;
            }
        }
        
        public int getRequestCount() {
            return requestCount.get();
        }
        
        public long getWindowStart() {
            return windowStart;
        }
        
        public void addRequest(long timestamp) {
            requestCount.incrementAndGet();
        }
    }
}

