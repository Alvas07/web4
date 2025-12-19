package exceptions;

public class RateLimitException extends Exception {
    private final long waitTimeMs;
    private final String message;

    public RateLimitException(String message, long waitTimeMs) {
        super(message);
        this.message = message;
        this.waitTimeMs = waitTimeMs;
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }

    @Override
    public String getMessage() {
        return message;
    }
}


