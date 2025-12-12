package utils.db;

import exceptions.DatabaseException;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Singleton
@Startup
public class DBInitializer {
    @PersistenceContext(unitName = "hitCheckerPU")
    private EntityManager em;

    @PostConstruct
    public void initialize() throws DatabaseException {
        try {
            createTables();
            createIndexes();
            System.out.println("✅ Database schema initialized successfully");
        } catch (Exception e) {
            throw new DatabaseException("Database initialization failed", e);
        }
    }

    private void createTables() throws DatabaseException {
        try {
            executeSQL(SQLQueries.CREATE_USERS_TABLE, "Users table");
            executeSQL(SQLQueries.CREATE_RESULTS_TABLE, "Results table");
            System.out.println("✅ Database tables created");
        } catch (Exception e) {
            throw new DatabaseException("Table creation warning", e);
        }

    }

    private void createIndexes() throws DatabaseException {
        try {
            executeSQL(SQLQueries.CREATE_IDX_USERS_USERNAME, "Username index");
            executeSQL(SQLQueries.CREATE_IDX_RESULTS_CREATED_AT, "CreatedAt index");
            System.out.println("✅ Database indexes created");
        } catch (Exception e) {
            throw new DatabaseException("Index creation warning", e);
        }
    }

    private void executeSQL(String sql, String operation) throws DatabaseException {
        try {
            Query query = em.createNativeQuery(sql);
            query.executeUpdate();
            System.out.println("✅ " + operation);
        } catch (Exception e) {
            throw new DatabaseException("Failed to execute SQL for: " + operation, e);
        }
    }
}
