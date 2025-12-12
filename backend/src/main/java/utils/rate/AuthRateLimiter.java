package utils.rate;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter для аутентификации (логин/регистрация)
 * Ограничение: максимум 3 запроса в 30 секунд на IP адрес или username
 */
@Singleton
@Startup
public class AuthRateLimiter {
    // Максимальное количество запросов в окне
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    
    // Время окна в миллисекундах (30 секунд)
    private static final long WINDOW_MS = 30000;
    
    // Хранилище: identifier (IP или username) -> RequestWindow
    private final Map<String, RequestWindow> requests = new ConcurrentHashMap<>();
    
    /**
     * Проверяет, не превышен ли лимит запросов
     * @param identifier IP адрес или username
     * @return true если лимит превышен, false если можно обработать запрос
     */
    public boolean isRateLimited(String identifier) {
        long currentTime = System.currentTimeMillis();
        RequestWindow window = requests.computeIfAbsent(identifier, k -> new RequestWindow());
        
        // Очищаем старые запросы (старше окна)
        window.cleanOldRequests(currentTime);
        
        // Проверяем лимит
        if (window.getRequestCount() >= MAX_REQUESTS_PER_WINDOW) {
            return true; // Лимит превышен
        }
        
        // Добавляем новый запрос
        window.addRequest(currentTime);
        return false; // Лимит не превышен
    }
    
    /**
     * Получает время до следующего разрешенного запроса в миллисекундах
     * @param identifier IP адрес или username
     * @return время в миллисекундах до следующего разрешенного запроса, или 0 если можно запросить сейчас
     */
    public long getTimeUntilNextRequest(String identifier) {
        long currentTime = System.currentTimeMillis();
        RequestWindow window = requests.get(identifier);
        
        if (window == null) {
            return 0;
        }
        
        window.cleanOldRequests(currentTime);
        
        if (window.getRequestCount() < MAX_REQUESTS_PER_WINDOW) {
            return 0;
        }
        
        // Вычисляем время до истечения окна
        long timeElapsed = currentTime - window.getWindowStart();
        long timeRemaining = WINDOW_MS - timeElapsed;
        return Math.max(0, timeRemaining);
    }
    
    /**
     * Окно запросов для одного идентификатора
     */
    private static class RequestWindow {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        
        public void cleanOldRequests(long currentTime) {
            // Если прошло время окна, сбрасываем счетчик
            if (currentTime - windowStart >= WINDOW_MS) {
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
