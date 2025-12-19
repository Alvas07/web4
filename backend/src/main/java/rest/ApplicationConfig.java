package rest;

import exceptions.GlobalExceptionMapper;
import filters.AuthFilter;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(AuthController.class);
        classes.add(PointController.class);
        classes.add(HistoryController.class);
        classes.add(JacksonObjectMapperProvider.class);
        classes.add(GlobalExceptionMapper.class);
        classes.add(AuthFilter.class);
        return classes;
    }

    @Provider
    public static class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
        private final ObjectMapper mapper;

        public JacksonObjectMapperProvider() {
            mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return mapper;
        }
    }
}
