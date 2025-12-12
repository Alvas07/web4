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
    
    @JsonProperty("offset")
    private int offset = 0;
    
    @JsonProperty("needTotalCount")
    private boolean needTotalCount = false;
    
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
    
    public int offset() {
        return offset;
    }
    
    public boolean needTotalCount() {
        return needTotalCount;
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
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public void setNeedTotalCount(boolean needTotalCount) {
        this.needTotalCount = needTotalCount;
    }
}
