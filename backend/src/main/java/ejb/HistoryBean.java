package ejb;

import dto.HistoryEntryDTO;
import dto.PointDTO;
import entities.HistoryEntry;
import entities.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import repositories.HistoryRepository;
import repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class HistoryBean {
    @EJB
    private HistoryRepository historyRepository;
    
    @EJB
    private UserRepository userRepository;

    public List<HistoryEntryDTO> getHistoryWithOffset(int offset, int limit) {
        List<HistoryEntry> entries = historyRepository.getHistoryWithOffset(offset, limit);

        return entries.stream()
                .map(h -> new HistoryEntryDTO(
                        new PointDTO(h.getPoint().getX(), h.getPoint().getY(), h.getPoint().getR()),
                        h.isHit(),
                        h.getUser().getUsername(),
                        h.getCreatedAt(),
                        h.getExecTime()
                ))
                .toList();
    }
    
    public Long getTotalCount() {
        return historyRepository.getTotalCount();
    }

    public void save(List<HistoryEntry> entries) {
        historyRepository.persistAll(entries);
    }

    public void deleteByUsername(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            historyRepository.deleteByUser(user);
        });
    }
    
    public List<HistoryEntryDTO> getNewPoints(LocalDateTime lastCreatedAt, Long lastId, String currentUsername) {
        List<HistoryEntry> entries;
        
        if (lastCreatedAt == null) {
            // Если нет последней даты, получаем последние 20 точек от других пользователей
            entries = historyRepository.getNewPointsAll(currentUsername, 20);
        } else {
            // Получаем точки, созданные после указанной даты, исключая текущего пользователя
            entries = historyRepository.getNewPointsAfterDate(lastCreatedAt, currentUsername, 100);
        }

        return entries.stream()
                .map(h -> new HistoryEntryDTO(
                        new PointDTO(h.getPoint().getX(), h.getPoint().getY(), h.getPoint().getR()),
                        h.isHit(),
                        h.getUser().getUsername(),
                        h.getCreatedAt(),
                        h.getExecTime()
                ))
                .toList();
    }
}
