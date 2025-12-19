package ejb;

import entities.User;
import exceptions.AuthException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import repositories.UserRepository;
import utils.auth.PasswordHasher;

import java.util.Optional;

@Stateless
public class AuthBean {
    @EJB
    private UserRepository userRepository;

    private void validateUsername(String username) throws AuthException {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthException("Username cannot be empty");
        }
        
        String trimmed = username.trim();
        
        if (trimmed.length() < 3 || trimmed.length() > 20) {
            throw new AuthException("Username must be between 3 and 20 characters");
        }
        
        if (!trimmed.matches("^[a-zA-Z0-9_]+$")) {
            throw new AuthException("Username can only contain letters, numbers, and underscores");
        }
        
        // Логин не может состоять только из цифр
        if (trimmed.matches("^[0-9]+$")) {
            throw new AuthException("Username cannot consist only of digits");
        }
        
        // Логин должен содержать хотя бы одну букву
        if (!trimmed.matches(".*[a-zA-Z].*")) {
            throw new AuthException("Username must contain at least one letter");
        }
        
        // Логин не должен начинаться с цифры
        if (trimmed.matches("^[0-9].*")) {
            throw new AuthException("Username cannot start with a digit");
        }
        
        // Логин не должен начинаться или заканчиваться подчеркиванием
        if (trimmed.startsWith("_") || trimmed.endsWith("_")) {
            throw new AuthException("Username cannot start or end with an underscore");
        }
        
        // Логин не должен содержать подряд более одного подчеркивания
        if (trimmed.contains("__")) {
            throw new AuthException("Username cannot contain consecutive underscores");
        }
    }
    
    private void validatePassword(String password) throws AuthException {
        if (password == null || password.isEmpty()) {
            throw new AuthException("Password cannot be empty");
        }
        
        if (password.length() < 6 || password.length() > 50) {
            throw new AuthException("Password must be between 6 and 50 characters");
        }
        
        if (!password.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]+$")) {
            throw new AuthException("Password contains invalid characters");
        }
        
        // Проверяем, что пароль содержит хотя бы одну букву и одну цифру
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        
        if (!hasLetter || !hasDigit) {
            throw new AuthException("Password must contain at least one letter and one digit");
        }
    }

    public Optional<User> authenticate(String username, String password) throws AuthException {
        if (username == null || password == null) {
            throw new AuthException("Missing username or password");
        }
        
        validateUsername(username);
        validatePassword(password);

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
        
        validateUsername(username);
        validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new AuthException("Username already exists");
        }

        String hash = PasswordHasher.hashPassword(password);
        User user = new User(username, hash);
        userRepository.persist(user);
        return user;
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
