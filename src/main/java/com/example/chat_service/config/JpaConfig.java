// chat_service/src/main/java/com/example/chat_service/config/JpaConfig.java
package com.example.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for repository scanning.
 *
 * <p>Explicitly enables scanning for Spring Data JPA repository interfaces
 * in the infrastructure persistence layer.</p>
 *
 * <p><strong>Important:</strong> The {@code basePackages} must point to the package
 * containing your {@code @Repository} interfaces that extend {@code JpaRepository},
 * NOT the package containing your domain-layer repository implementations.
 *
 * <ul>
 *   <li>Scan: {@code infrastructure.persistence.posts.jpa} (where {@code PostCommandJpaRepository} lives)</li>
 *   <li>Scan: {@code infrastructure.persistence.members.jpa} (where {@code MemberCommandJpaRepository} lives)</li>
 * </ul>
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.example.chat_service.infrastructure.persistence.posts.jpa",
        "com.example.chat_service.infrastructure.persistence.member.jpa",
        "com.example.chat_service.infrastructure.persistence.rooms.jpa",
        "com.example.chat_service.infrastructure.persistence.messages.jpa"
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class JpaConfig {
    // Empty — annotations handle all configuration
}