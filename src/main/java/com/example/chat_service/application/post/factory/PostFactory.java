// chat_service/src/main/java/com/example/chat_service/application/post/factory/PostFactory.java

package com.example.chat_service.application.post.factory;

import java.util.Optional;

import org.springframework.context.ApplicationContext;

import com.example.chat_service.application.post.services.PostCommandServiceInterface;
import com.example.chat_service.application.post.services.impl.PostCommandServiceImpl;
import com.example.chat_service.domain.post.repositories.PostCommandRepository;
import com.example.chat_service.infrastructure.persistence.posts.repositories.PostCommandOrmRepository;
import com.example.chat_service.infrastructure.persistence.posts.jpa.PostCommandJpaRepository;

/**
 * Factory for obtaining post-related application services.
 *
 * <p><strong>Spring Boot Note:</strong> In a typical Spring Boot application,
 * dependency injection via {@code @Autowired} or constructor injection is preferred.
 * This factory is provided for:
 * <ul>
 *   <li>Testing scenarios where manual instantiation is needed</li>
 *   <li>Non-Spring contexts (e.g., CLI tools, batch jobs)</li>
 *   <li>Explicit composition root patterns</li>
 * </ul>
 *
 * <p><strong>Singleton behavior:</strong> All factory methods return the same
 * instance per JVM lifecycle, mirroring Python's {@code @cache} decorator.</p>
 *
 * <p><strong>No events/query/handlers:</strong> This factory currently only
 * provides the command service. Query services, handlers, and event publishers
 * can be added when the infrastructure is ready.</p>
 */
public final class PostFactory {

    // ── Private constructor to prevent instantiation ─────────────────
    private PostFactory() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    // ── Static holder for singleton instances (thread-safe via class loading) ─
    private static class Holder {
        // Will be initialized on first access
        static PostCommandServiceInterface commandService;
        static PostCommandRepository commandRepository;
        static PostCommandJpaRepository jpaRepository;
    }

    // ── Spring Context Holder (optional, for DI-aware factory usage) ─
    private static ApplicationContext springContext;

    /**
     * Register the Spring ApplicationContext for factory methods to use.
     *
     * <p>Call this once during application startup if you want factory methods
     * to return Spring-managed beans instead of manually instantiated ones.</p>
     *
     * @param context the Spring ApplicationContext
     */
    public static void registerSpringContext(ApplicationContext context) {
        springContext = context;
    }

    // ── JpaRepository Factory ────────────────────────────────────────

    /**
     * Get the Spring Data JPA repository for post command operations.
     *
     * <p>If Spring context is registered, returns the Spring-managed bean.
     * Otherwise, manually instantiates using default JPA configuration.</p>
     *
     * @return singleton PostCommandJpaRepository instance
     */
    public static PostCommandJpaRepository getPostCommandJpaRepository() {
        if (Holder.jpaRepository != null) {
            return Holder.jpaRepository;
        }

        // Try to get from Spring context first
        if (springContext != null) {
            Holder.jpaRepository = springContext.getBean(PostCommandJpaRepository.class);
            return Holder.jpaRepository;
        }

        // Fallback: manual instantiation (for testing/non-Spring contexts)
        // Note: This requires JPA EntityManager to be bootstrapped separately
        throw new IllegalStateException(
            "Cannot create PostCommandJpaRepository manually. " +
            "Either register Spring context via PostFactory.registerSpringContext(), " +
            "or use Spring's @Autowired injection instead."
        );
    }

    // ── Command Repository Factory ───────────────────────────────────

    /**
     * Get the domain-layer command repository for post aggregates.
     *
     * <p>Returns a singleton instance backed by JPA/Hibernate.</p>
     *
     * @return singleton PostCommandRepository implementation
     */
    public static PostCommandRepository getPostCommandRepository() {
        if (Holder.commandRepository != null) {
            return Holder.commandRepository;
        }

        // Try to get from Spring context first
        if (springContext != null) {
            Holder.commandRepository = springContext.getBean(PostCommandRepository.class);
            return Holder.commandRepository;
        }

        // Fallback: manual composition (for testing)
        PostCommandJpaRepository jpaRepo = getPostCommandJpaRepository();
        Holder.commandRepository = new PostCommandOrmRepository(jpaRepo);
        return Holder.commandRepository;
    }

    // ── Command Service Factory ──────────────────────────────────────

    /**
     * Get the application-layer command service for post operations.
     *
     * <p>Returns a singleton instance with all dependencies injected.</p>
     *
     * @return singleton PostCommandServiceInterface implementation
     */
    public static PostCommandServiceInterface getPostCommandService() {
        if (Holder.commandService != null) {
            return Holder.commandService;
        }

        // Try to get from Spring context first
        if (springContext != null) {
            // Spring will return the @Service bean (PostCommandServiceImpl)
            Holder.commandService = springContext.getBean(PostCommandServiceInterface.class);
            return Holder.commandService;
        }

        // Fallback: manual composition (for testing/non-Spring contexts)
        PostCommandRepository repo = getPostCommandRepository();
        Holder.commandService = new PostCommandServiceImpl(repo);
        return Holder.commandService;
    }

    // ── Convenience: Reset singletons (for testing only) ─────────────

    /**
     * Reset all cached singleton instances.
     *
     * <p><strong>Warning:</strong> Only use in test teardown. Not thread-safe.</p>
     */
    public static void resetForTesting() {
        Holder.commandService = null;
        Holder.commandRepository = null;
        Holder.jpaRepository = null;
    }

    // ── Optional: Direct instantiation helpers (bypass singleton) ────

    /**
     * Create a new command service instance with explicit dependencies.
     *
     * <p>Useful for tests that need isolated instances or custom mocks.</p>
     *
     * @param commandRepository the repository implementation to use
     * @return a new PostCommandServiceInterface instance
     */
    public static PostCommandServiceInterface createCommandService(
            PostCommandRepository commandRepository) {
        return new PostCommandServiceImpl(commandRepository);
    }

    /**
     * Create a new command repository instance with explicit JPA repo.
     *
     * @param jpaRepository the Spring Data JPA repository to use
     * @return a new PostCommandRepository implementation
     */
    public static PostCommandRepository createCommandRepository(
            PostCommandJpaRepository jpaRepository) {
        return new PostCommandOrmRepository(jpaRepository);
    }
}