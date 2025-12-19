package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record PollRequestDTO(
    @JsonProperty("lastCreatedAt") Long lastCreatedAtTimestamp,
    @JsonProperty("lastId") Long lastId,
    @JsonProperty("timeoutSeconds") Integer timeoutSeconds
) {
    public LocalDateTime lastCreatedAt() {
        if (lastCreatedAtTimestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastCreatedAtTimestamp), ZoneId.systemDefault());
    }
}
