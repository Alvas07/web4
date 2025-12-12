package utils.db;

public final class SQLQueries {
    private SQLQueries() {}

    // DDL
    public static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    public static final String CREATE_RESULTS_TABLE = """
            CREATE TABLE IF NOT EXISTS results (
                id SERIAL PRIMARY KEY,
                x DOUBLE PRECISION NOT NULL,
                y DOUBLE PRECISION NOT NULL,
                r DOUBLE PRECISION NOT NULL,
                hit BOOLEAN NOT NULL,
                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                exec_time DOUBLE PRECISION
            )
            """;

    // Indexes
    public static final String CREATE_IDX_USERS_USERNAME = """
            CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)
            """;

    public static final String CREATE_IDX_RESULTS_CREATED_AT = """
            CREATE INDEX IF NOT EXISTS idx_results_created_at ON results(created_at DESC)
            """;

    // Business logic
    public static final String FIND_USER_BY_USERNAME = """
            SELECT u FROM User u WHERE u.username = :username
            """;

    public static final String COUNT_USERS_BY_USERNAME = """
            SELECT COUNT(u) FROM User u WHERE u.username = :username
            """;

    public static final String DELETE_RESULTS_BY_USER = """
            DELETE FROM HistoryEntry h WHERE h.user.id = :userId
            """;

    public static final String GET_HISTORY_KEYSET = """
            SELECT h FROM HistoryEntry h
            JOIN FETCH h.user
            WHERE (?1 IS NULL
                    OR h.createdAt < ?1
                    OR (h.createdAt = ?1 AND h.id < ?2))
            ORDER BY h.createdAt DESC, h.id DESC
            """;
}
