package repositories;

import entities.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import utils.db.SQLQueries;

import java.util.List;
import java.util.Optional;

@Stateless
public class UserRepository {
    @PersistenceContext(unitName = "hitCheckerPU")
    private EntityManager em;

    public Optional<User> findByUsername(String username) {
        List<User> users = em.createQuery(SQLQueries.FIND_USER_BY_USERNAME, User.class)
                .setParameter("username", username)
                .getResultList();
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public boolean existsByUsername(String username) {
        Long count = em.createQuery(SQLQueries.COUNT_USERS_BY_USERNAME, Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public void persist(User user) {
        em.persist(user);
    }
}


