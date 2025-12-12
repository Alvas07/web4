package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistoryRequestDTO {
    @JsonProperty("lastCreatedAt")
    private String lastCreatedAtStr;
    
    @JsonProperty("lastId")
    private Long lastId;
    
    @JsonProperty("limit")
    private int limit = 20;
    
    public HistoryRequestDTO() {}
    
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
    
    public int limit() {
        return limit;
    }
    
    // Setters for deserialization
    public void setLastCreatedAt(String lastCreatedAtStr) {
        this.lastCreatedAtStr = lastCreatedAtStr;
    }
    
    public void setLastId(Long lastId) {
        this.lastId = lastId;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
