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

    public List<HistoryEntryDTO> getHistory(LocalDateTime lastCreatedAt, Long lastId, int limit) {
        String queryStr;
        if (lastCreatedAt == null) {
            queryStr = "SELECT h FROM HistoryEntry h JOIN FETCH h.user ORDER BY h.createdAt DESC, h.id DESC";
        } else {
            queryStr = "SELECT h FROM HistoryEntry h JOIN FETCH h.user WHERE h.createdAt < ?1 OR (h.createdAt = ?1 AND h.id < ?2) ORDER BY h.createdAt DESC, h.id DESC";
        }
        
        TypedQuery<HistoryEntry> q = em.createQuery(queryStr, HistoryEntry.class);
        
        if (lastCreatedAt != null) {
            q.setParameter(1, lastCreatedAt);
            q.setParameter(2, lastId != null ? lastId : 0L);
        }
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
    
    /**
     * Получает новые точки, созданные после указанной даты
     * @param lastCreatedAt дата последней полученной точки
     * @param lastId ID последней полученной точки (не используется, оставлен для совместимости)
     * @param currentUsername имя текущего пользователя (чтобы исключить его собственные точки)
     * @return список новых точек
     */
    public List<HistoryEntryDTO> getNewPoints(LocalDateTime lastCreatedAt, Long lastId, String currentUsername) {
        String queryStr;
        if (lastCreatedAt == null) {
            // Если нет последней даты, получаем последние 20 точек от других пользователей
            queryStr = "SELECT h FROM HistoryEntry h JOIN FETCH h.user WHERE h.user.username != :currentUsername ORDER BY h.createdAt DESC, h.id DESC";
        } else {
            // Получаем точки, созданные после указанной даты, исключая текущего пользователя
            // Используем только createdAt для простоты, дубликаты будут отфильтрованы на фронтенде
            queryStr = "SELECT h FROM HistoryEntry h JOIN FETCH h.user WHERE h.user.username != :currentUsername AND h.createdAt > :lastCreatedAt ORDER BY h.createdAt ASC, h.id ASC";
        }
        
        TypedQuery<HistoryEntry> q = em.createQuery(queryStr, HistoryEntry.class);
        q.setParameter("currentUsername", currentUsername);
        
        if (lastCreatedAt != null) {
            q.setParameter("lastCreatedAt", lastCreatedAt);
        }
        
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
