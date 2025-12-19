package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record PollRequestDTO(
    @JsonProperty("lastCreatedAt") String lastCreatedAtStr,
    @JsonProperty("lastId") Long lastId,
    @JsonProperty("timeoutSeconds") Integer timeoutSeconds
) {
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
