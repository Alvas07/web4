package dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record HistoryRequestDTO(
    @JsonProperty("lastCreatedAt") String lastCreatedAtStr,
    @JsonProperty("lastId") Long lastId,
    @JsonProperty("limit") int limit,
    @JsonProperty("offset") int offset,
    @JsonProperty("needTotalCount") boolean needTotalCount
) {
    @JsonCreator
    public HistoryRequestDTO(
            @JsonProperty("lastCreatedAt") String lastCreatedAtStr,
            @JsonProperty("lastId") Long lastId,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("offset") Integer offset,
            @JsonProperty("needTotalCount") Boolean needTotalCount
    ) {
        this(
            lastCreatedAtStr,
            lastId,
            limit != null ? limit : 20,
            offset != null ? offset : 0,
            needTotalCount != null ? needTotalCount : false
        );
    }
    
    public LocalDateTime lastCreatedAt() {
        if (lastCreatedAtStr == null || lastCreatedAtStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(lastCreatedAtStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
