package dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record HistoryRequestDTO(
    @JsonProperty("lastCreatedAt") Long lastCreatedAtTimestamp,
    @JsonProperty("lastId") Long lastId,
    @JsonProperty("page") int page,
    @JsonProperty("pageSize") int pageSize,
    @JsonProperty("needTotalCount") boolean needTotalCount
) {
    @JsonCreator
    public HistoryRequestDTO(
            @JsonProperty("lastCreatedAt") Long lastCreatedAtTimestamp,
            @JsonProperty("lastId") Long lastId,
            @JsonProperty("page") Integer page,
            @JsonProperty("pageSize") Integer pageSize,
            @JsonProperty("needTotalCount") Boolean needTotalCount
    ) {
        this(
            lastCreatedAtTimestamp,
            lastId,
            page != null && page > 0 ? page : 1,
            pageSize != null && pageSize > 0 ? pageSize : 20,
            needTotalCount != null ? needTotalCount : false
        );
    }
    
    public LocalDateTime lastCreatedAt() {
        if (lastCreatedAtTimestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastCreatedAtTimestamp), ZoneId.systemDefault());
    }
    
    // Вспомогательные методы для конвертации в offset/limit
    public int getOffset() {
        return (page - 1) * pageSize;
    }
    
    public int getLimit() {
        return pageSize;
    }
}
