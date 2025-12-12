package dto;

import java.time.LocalDateTime;

public record HistoryEntryDTO(PointDTO point, boolean hit, String username,
                              LocalDateTime createdAt, double execTime) {}
