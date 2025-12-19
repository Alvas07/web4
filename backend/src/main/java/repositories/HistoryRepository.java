package repositories;

import entities.HistoryEntry;
import entities.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import utils.db.SQLQueries;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class HistoryRepository {
    @PersistenceContext(unitName = "hitCheckerPU")
    private EntityManager em;

    public List<HistoryEntry> getHistoryWithOffset(int offset, int limit) {
        TypedQuery<HistoryEntry> q = em.createQuery(SQLQueries.GET_HISTORY_WITH_OFFSET, HistoryEntry.class);
        q.setFirstResult(offset);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    public Long getTotalCount() {
        TypedQuery<Long> q = em.createQuery(SQLQueries.COUNT_HISTORY_ENTRIES, Long.class);
        return q.getSingleResult();
    }

    public void persist(HistoryEntry entry) {
        em.persist(entry);
    }

    public void persistAll(List<HistoryEntry> entries) {
        for (HistoryEntry entry : entries) {
            em.persist(entry);
        }
    }

    public void deleteByUser(User user) {
        int deleted = em.createQuery(SQLQueries.DELETE_RESULTS_BY_USER)
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.flush();
    }

    public List<HistoryEntry> getNewPointsAll(String currentUsername, int maxResults) {
        TypedQuery<HistoryEntry> q = em.createQuery(SQLQueries.GET_NEW_POINTS_ALL, HistoryEntry.class);
        q.setParameter("currentUsername", currentUsername);
        q.setMaxResults(maxResults);
        return q.getResultList();
    }

    public List<HistoryEntry> getNewPointsAfterDate(LocalDateTime lastCreatedAt, String currentUsername, int maxResults) {
        TypedQuery<HistoryEntry> q = em.createQuery(SQLQueries.GET_NEW_POINTS_AFTER_DATE, HistoryEntry.class);
        q.setParameter("lastCreatedAt", lastCreatedAt);
        q.setParameter("currentUsername", currentUsername);
        q.setMaxResults(maxResults);
        return q.getResultList();
    }
}


