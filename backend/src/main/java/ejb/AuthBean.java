package ejb;

import entities.User;
import exceptions.AuthException;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import utils.auth.PasswordHasher;
import utils.db.SQLQueries;

import java.util.List;
import java.util.Optional;

@Stateless
public class AuthBean {
    @PersistenceContext(unitName = "hitCheckerPU")
    private EntityManager em;

    public Optional<User> authenticate(String username, String password) throws AuthException {
        if (username == null || password == null) {
            throw new AuthException("Missing username or password");
        }

        Optional<User> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }

    public User register(String username, String password) throws AuthException {
        if (username == null || password == null) {
            throw new AuthException("Missing username or password");
        }

        Long count = em.createQuery(SQLQueries.COUNT_USERS_BY_USERNAME, Long.class)
                .setParameter("username", username).getSingleResult();
        if (count > 0) {
            throw new AuthException("Username already exists");
        }

        String hash = PasswordHasher.hashPassword(password);
        User user = new User(username, hash);
        em.persist(user);
        return user;
    }

    public Optional<User> findUserByUsername(String username) {
        List<User> users = em.createQuery(SQLQueries.FIND_USER_BY_USERNAME, User.class)
                .setParameter("username", username).getResultList();
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
}
