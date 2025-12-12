package utils.rate;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Простой rate limiter для ограничения количества запросов от пользователя
 * Ограничение: максимум 10 запросов в секунду на пользователя
 */
@Singleton
@Startup
public class RateLimiter {
    // Максимальное количество запросов в секунду (увеличено для точек)
    private static final int MAX_REQUESTS_PER_SECOND = 10;
    
    // Время окна в миллисекундах
    private static final long WINDOW_MS = 1000;
    
    // Хранилище: username -> (timestamp -> количество запросов)
    private final Map<String, RequestWindow> userRequests = new ConcurrentHashMap<>();
    
    /**
     * Проверяет, не превышен ли лимит запросов для пользователя
     * @param username имя пользователя
     * @return true если лимит превышен, false если можно обработать запрос
     */
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
        return false; // Лимит не превышен
    }
    
    /**
     * Окно запросов для одного пользователя
     */
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

