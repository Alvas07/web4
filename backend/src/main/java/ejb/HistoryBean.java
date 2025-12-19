package ejb;

import dto.HistoryEntryDTO;
import dto.PointDTO;
import entities.HistoryEntry;
import entities.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import utils.db.SQLQueries;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Stateless
public class HistoryBean {
    @PersistenceContext(unitName = "hitCheckerPU")
    private EntityManager em;

    public List<HistoryEntryDTO> getHistoryWithOffset(int offset, int limit) {
        TypedQuery<HistoryEntry> q = em.createQuery(SQLQueries.GET_HISTORY_WITH_OFFSET, HistoryEntry.class);
        q.setFirstResult(offset);
        q.setMaxResults(limit);

        List<HistoryEntry> entries = q.getResultList();

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
        TypedQuery<Long> q = em.createQuery(SQLQueries.COUNT_HISTORY_ENTRIES, Long.class);
        return q.getSingleResult();
    }

    public void save(List<HistoryEntry> entries) {
        for (HistoryEntry entry : entries) {
            em.persist(entry);
        }
    }

    public void deleteByUsername(String username) {
        try {
            User user = em.createQuery(SQLQueries.FIND_USER_BY_USERNAME, User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            int deleted = em.createQuery(SQLQueries.DELETE_RESULTS_BY_USER)
                    .setParameter("userId", user.getId())
                    .executeUpdate();
            em.flush();
        } catch (NoResultException ignored) {}
    }
    
    public List<HistoryEntryDTO> getNewPoints(LocalDateTime lastCreatedAt, Long lastId, String currentUsername) {
        TypedQuery<HistoryEntry> q;
        if (lastCreatedAt == null) {
            // Если нет последней даты, получаем последние 20 точек от других пользователей
            q = em.createQuery(SQLQueries.GET_NEW_POINTS_ALL, HistoryEntry.class);
        } else {
            // Получаем точки, созданные после указанной даты, исключая текущего пользователя
            // Используем только createdAt для простоты, дубликаты будут отфильтрованы на фронтенде
            q = em.createQuery(SQLQueries.GET_NEW_POINTS_AFTER_DATE, HistoryEntry.class);
            q.setParameter("lastCreatedAt", lastCreatedAt);
        }
        
        q.setParameter("currentUsername", currentUsername);
        
        if (lastCreatedAt == null) {
            q.setMaxResults(20); // Ограничиваем для начальной загрузки
        } else {
            q.setMaxResults(100); // Больше для long-polling
        }

        List<HistoryEntry> entries = q.getResultList();

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
