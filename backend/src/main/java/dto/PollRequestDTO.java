package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PollRequestDTO {
    @JsonProperty("lastCreatedAt")
    private String lastCreatedAtStr;
    
    @JsonProperty("lastId")
    private Long lastId;
    
    @JsonProperty("timeoutSeconds")
    private Integer timeoutSeconds;
    
    public PollRequestDTO() {}
    
    public LocalDateTime lastCreatedAt() {
        if (lastCreatedAtStr == null || lastCreatedAtStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(lastCreatedAtStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Long lastId() {
        return lastId;
    }
    
    public Integer timeoutSeconds() {
        return timeoutSeconds;
    }
    
    // Setters for deserialization
    public void setLastCreatedAt(String lastCreatedAtStr) {
        this.lastCreatedAtStr = lastCreatedAtStr;
    }
    
    public void setLastId(Long lastId) {
        this.lastId = lastId;
    }
    
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
