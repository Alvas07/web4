package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record HistoryResponseDTO(
    @JsonProperty("entries") List<HistoryEntryDTO> entries,
    @JsonProperty("totalCount") Long totalCount
) {}
