package utils.rate;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Startup
public class RateLimiter {
    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final long WINDOW_MS = 1000;
    private final Map<String, RequestWindow> userRequests = new ConcurrentHashMap<>();
    
    public boolean isRateLimited(String username) {
        long currentTime = System.currentTimeMillis();
        RequestWindow window = userRequests.computeIfAbsent(username, k -> new RequestWindow());
        
        // Очищаем старые запросы (старше 1 секунды)
        window.cleanOldRequests(currentTime);
        
        // Проверяем лимит
        if (window.getRequestCount() >= MAX_REQUESTS_PER_SECOND) {
            return true; // Лимит превышен
        }
        
        // Добавляем новый запрос
        window.addRequest(currentTime);
        return false;
    }
    
    private static class RequestWindow {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        
        public void cleanOldRequests(long currentTime) {
            // Если прошла секунда, сбрасываем счетчик
            if (currentTime - windowStart >= WINDOW_MS) {
                requestCount.set(0);
                windowStart = currentTime;
            }
        }
        
        public int getRequestCount() {
            return requestCount.get();
        }
        
        public void addRequest(long timestamp) {
            requestCount.incrementAndGet();
        }
    }
}

