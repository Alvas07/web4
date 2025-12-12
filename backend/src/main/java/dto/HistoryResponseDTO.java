package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HistoryResponseDTO {
    @JsonProperty("entries")
    private List<HistoryEntryDTO> entries;
    
    @JsonProperty("totalCount")
    private Long totalCount;
    
    public HistoryResponseDTO() {}
    
    public HistoryResponseDTO(List<HistoryEntryDTO> entries, Long totalCount) {
        this.entries = entries;
        this.totalCount = totalCount;
    }
    
    public List<HistoryEntryDTO> getEntries() {
        return entries;
    }
    
    public void setEntries(List<HistoryEntryDTO> entries) {
        this.entries = entries;
    }
    
    public Long getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}

