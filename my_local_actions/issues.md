this is all i have (# src/main/resources/application.yml
server:
  port: ${SERVER_PORT:8005}

spring:
  application:
    name: chat_service
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE:10}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: ${SHOW_SQL:true}
    database-platform: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    root: INFO
    com.example.chat_service: ${LOG_LEVEL_APP:DEBUG}
    org.hibernate.SQL: ${LOG_LEVEL_SQL:DEBUG}
    org.hibernate.type.descriptor.sql.BasicBinder: ${LOG_LEVEL_TRACE:TRACE}

# Auth configuration (clean, structured)
auth:
  public-key-url: ${AUTH_PUBLIC_KEY_URL}
  public-key-ttl: ${AUTH_PUBLIC_KEY_TTL:300}
  service-url: ${AUTH_SERVICE_URL}
  internal-api-key: ${INTERNAL_API_KEY})(# Server Configuration
SERVER_PORT=8005

# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/chatdb1
DB_USERNAME=ishimwe
DB_PASSWORD=2025New+
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=30000

# JPA Configuration
DDL_AUTO=update
SHOW_SQL=true

# Logging Configuration
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SQL=DEBUG
LOG_LEVEL_TRACE=TRACE



AUTH_PUBLIC_KEY_URL=http://127.0.0.1:8000/zedvye_one/users/public_key/
AUTH_SERVICE_URL=http://127.0.0.1:8000/zedvye_one
INTERNAL_API_KEY=super-secret-internal-key-change-in-prod
AUTH_PUBLIC_KEY_TTL=300)(<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.0.6</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.example</groupId>
	<artifactId>chat_service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>chat_service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-websocket</artifactId>
		</dependency>

		<!-- Spring Boot Starters -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		
		<!-- PostgreSQL Driver -->
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		
		<!-- Hibernate Enhancements (for @CreationTimestamp, etc.) -->
		<dependency>
			<groupId>org.hibernate.orm</groupId>
			<artifactId>hibernate-core</artifactId>
		</dependency>
		
		<!-- Optional: Flyway for explicit migrations (instead of auto-DDL) -->
		<!--
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-database-postgresql</artifactId>
		</dependency>
		-->

		<!-- Dotenv for .env file support -->
		<dependency>
			<groupId>io.github.cdimascio</groupId>
			<artifactId>dotenv-java</artifactId>
			<version>3.0.0</version>
		</dependency>

		<!-- JWT -->
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.12.5</version>
		</dependency>

		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.12.5</version>
			<scope>runtime</scope>
		</dependency>

		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.12.5</version>
			<scope>runtime</scope>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-websocket-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>)(// src/main/java/com/example/chat_service/api/auth/AuthTestController.java
package com.example.chat_service.api.auth;

import com.example.chat_service.infrastructure.security.UserContext;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTestController {

    @GetMapping("/test")
    public Map<String, Object> testAuth() {
        return Map.of("user_id_from_jwt", UserContext.getUserId());
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "chat_service");
    }
})(// src/main/java/com/example/chat_service/infrastructure/security/JWTAuthenticationFilter.java
package com.example.chat_service.infrastructure.security;

import com.example.chat_service.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final JWTVerifier verifier;
    private final AuthConfig config;
    
    private static final String COOKIE_NAME = "access_token";
    
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/health/",
            "/actuator/health",
            "/api/v1/auth/ping"
    );

    public JWTAuthenticationFilter(JWTVerifier verifier, AuthConfig config) {
        this.verifier = verifier;
        this.config = config;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);

            if (token != null && !token.isEmpty()) {
                logger.debug("Verifying token for request: {}", requestURI);
                
                Claims claims = verifier.verify(token, config.getAuthPublicKeyUrl());
                String userId = claims.get("user_id", String.class);
                
                if (userId == null || userId.isEmpty()) {
                    throw new SecurityException("Token missing required claim: user_id");
                }
                
                UserContext.setUserId(userId);
                logger.debug("Authenticated user_id={}", userId);
            }
            
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, "Token expired");
            
        } catch (SignatureException | MalformedJwtException e) {
            logger.warn("Invalid JWT for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, "Invalid token");
            
        } catch (SecurityException e) {
            logger.warn("Security error for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during JWT verification for request {}", requestURI, e);
            sendUnauthorized(response, "Authentication failed");
            
        } finally {
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                logger.debug("Extracted token from Authorization header");
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.isEmpty()) {
                        logger.debug("Extracted token from cookie '{}'", COOKIE_NAME);
                        return token;
                    }
                }
            }
        }

        logger.debug("No authentication token found in request");
        return null;
    }

    private boolean isExcludedPath(String requestURI) {
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message.replace("\"", "\\\"")));
        response.getWriter().flush();
    }
})(// src/main/java/com/example/chat_service/infrastructure/security/UserContext.java
package com.example.chat_service.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the authenticated user ID.
 * Replaces Go's context.WithValue / Django's request.user_id pattern.
 * 
 * Usage:
 *   - Set in filter/middleware: UserContext.setUserId("uuid-string")
 *   - Access anywhere: String id = UserContext.getUserId()
 *   - Always clear in finally block to prevent memory leaks
 */
public class UserContext {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    private UserContext() {
        // Prevent instantiation - utility class only
    }

    /**
     * Store the authenticated user ID for the current request thread.
     * @param userId the user ID as a string (UUID format recommended)
     */
    public static void setUserId(String userId) {
        currentUser.set(userId);
    }

    /**
     * Retrieve the authenticated user ID for the current request thread.
     * @return the user ID string, or null if not set
     */
    public static String getUserId() {
        return currentUser.get();
    }

    /**
     * Retrieve the authenticated user ID as a UUID, if valid.
     * @return Optional containing the UUID, or empty if not set/invalid
     */
    public static Optional<UUID> getUserIdAsUuid() {
        String id = currentUser.get();
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a user ID is set for the current thread.
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        String id = currentUser.get();
        return id != null && !id.isEmpty();
    }

    /**
     * Clear the user ID for the current thread.
     * MUST be called after request processing to prevent memory leaks
     * in thread-pooled environments (Tomcat, Undertow, etc.).
     */
    public static void clear() {
        currentUser.remove();
    }
})(// src/main/java/com/example/chat_service/infrastructure/security/JWTVerifier.java
package com.example.chat_service.infrastructure.security;

import com.example.chat_service.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

@Component
public class JWTVerifier {

    private final java.net.http.HttpClient httpClient;
    private final long cacheTtlSeconds;
    
    private PublicKey cachedKey;
    private Instant lastFetchTime;

    public JWTVerifier(AuthConfig authConfig) {
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.cacheTtlSeconds = authConfig.getAuthPublicKeyTTL();
    }

    public synchronized PublicKey getPublicKey(String publicKeyUrl) throws Exception {
        Instant now = Instant.now();
        
        if (cachedKey != null && lastFetchTime != null) {
            long elapsed = java.time.Duration.between(lastFetchTime, now).getSeconds();
            if (elapsed < cacheTtlSeconds) {
                return cachedKey;
            }
        }

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(publicKeyUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch public key: HTTP " + response.statusCode());
        }

        String pem = response.body().trim();
        String keyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        cachedKey = factory.generatePublic(spec);
        lastFetchTime = now;
        
        return cachedKey;
    }

    public Claims verify(String token, String publicKeyUrl) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        if (token.split("\\.").length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        PublicKey key = getPublicKey(publicKeyUrl);

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!claims.containsKey("user_id")) {
            throw new IllegalArgumentException("Token missing required claim: user_id");
        }

        return claims;
    }
})(// src/main/java/com/example/chat_service/config/AuthConfig.java
package com.example.chat_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Value("${auth.public-key-url}")
    private String authPublicKeyUrl;

    @Value("${auth.public-key-ttl:300}")
    private long authPublicKeyTTL;

    @Value("${auth.internal-api-key}")
    private String internalApiKey;

    @Value("${auth.service-url}")
    private String authServiceUrl;

    public String getAuthPublicKeyUrl() {
        return authPublicKeyUrl;
    }

    public long getAuthPublicKeyTTL() {
        return authPublicKeyTTL;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }
}) can you see any issue in this code above that can lead to (



ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ mvn clean install
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------------< com.example:chat_service >----------------------
[INFO] Building chat_service 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:3.5.0:clean (default-clean) @ chat_service ---
[INFO] Deleting /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ chat_service ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] Copying 0 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- maven-compiler-plugin:3.14.1:compile (default-compile) @ chat_service ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 31 source files with javac [debug parameters release 21] to target/classes
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:testResources (default-testResources) @ chat_service ---
[INFO] skip non existing resourceDirectory /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.14.1:testCompile (default-testCompile) @ chat_service ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 1 source file with javac [debug parameters release 21] to target/test-classes
[INFO] 
[INFO] --- maven-surefire-plugin:3.5.5:test (default-test) @ chat_service ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.chat_service.ChatServiceApplicationTests
08:37:52.849 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.example.chat_service.ChatServiceApplicationTests]: ChatServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
08:37:53.617 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.example.chat_service.ChatServiceApplication for test class com.example.chat_service.ChatServiceApplicationTests
08:37:54.345 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.example.chat_service.ChatServiceApplicationTests]: ChatServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
08:37:54.348 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.example.chat_service.ChatServiceApplication for test class com.example.chat_service.ChatServiceApplicationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.0.6)

2026-05-11T08:37:59.716+02:00  INFO 41767 --- [chat_service] [           main] c.e.c.ChatServiceApplicationTests        : Starting ChatServiceApplicationTests using Java 21.0.10 with PID 41767 (started by ishimwe in /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service)
2026-05-11T08:37:59.718+02:00 DEBUG 41767 --- [chat_service] [           main] c.e.c.ChatServiceApplicationTests        : Running with Spring Boot v4.0.6, Spring v7.0.7
2026-05-11T08:37:59.719+02:00  INFO 41767 --- [chat_service] [           main] c.e.c.ChatServiceApplicationTests        : No active profile set, falling back to 1 default profile: "default"
2026-05-11T08:38:02.791+02:00  INFO 41767 --- [chat_service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-05-11T08:38:02.993+02:00  INFO 41767 --- [chat_service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 144 ms. Found 1 JPA repository interface.
2026-05-11T08:38:05.783+02:00  INFO 41767 --- [chat_service] [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-05-11T08:38:06.567+02:00  INFO 41767 --- [chat_service] [           main] org.hibernate.orm.core                   : HHH000001: Hibernate ORM core version 7.2.12.Final
2026-05-11T08:38:11.074+02:00  INFO 41767 --- [chat_service] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-05-11T08:38:11.323+02:00  INFO 41767 --- [chat_service] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-05-11T08:38:11.332+02:00  WARN 41767 --- [chat_service] [           main] org.hibernate.orm.jdbc                   : HHH100046: Could not obtain connection to query JDBC database metadata

java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DB_URL}
	at com.zaxxer.hikari.util.DriverDataSource.<init>(DriverDataSource.java:116) ~[HikariCP-7.0.2.jar:na]
	at com.zaxxer.hikari.pool.PoolBase.initializeDataSource(PoolBase.java:336) ~[HikariCP-7.0.2.jar:na]
	at com.zaxxer.hikari.pool.PoolBase.<init>(PoolBase.java:121) ~[HikariCP-7.0.2.jar:na]
	at com.zaxxer.hikari.pool.HikariPool.<init>(HikariPool.java:90) ~[HikariCP-7.0.2.jar:na]
	at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:111) ~[HikariCP-7.0.2.jar:na]
	at org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider.getConnection(DataSourceConnectionProvider.java:137) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator$ConnectionProviderJdbcConnectionAccess.obtainConnection(JdbcEnvironmentInitiator.java:508) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcIsolationDelegate.delegateWork(JdbcIsolationDelegate.java:48) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentUsingJdbcMetadata(JdbcEnvironmentInitiator.java:366) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironment(JdbcEnvironmentInitiator.java:143) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:120) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:80) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.boot.registry.internal.StandardServiceRegistryImpl.initiateService(StandardServiceRegistryImpl.java:133) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:260) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.service.internal.AbstractServiceRegistryImpl.initializeService(AbstractServiceRegistryImpl.java:235) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.service.internal.AbstractServiceRegistryImpl.getService(AbstractServiceRegistryImpl.java:212) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.boot.model.relational.Database.<init>(Database.java:44) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.getDatabase(InFlightMetadataCollectorImpl.java:251) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.<init>(InFlightMetadataCollectorImpl.java:203) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.complete(MetadataBuildingProcess.java:172) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.metadata(EntityManagerFactoryBuilderImpl.java:1392) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.populateSessionFactoryBuilder(EntityManagerFactoryBuilderImpl.java:1472) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1454) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:93) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:443) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:436) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:411) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:419) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1864) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1813) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:603) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201) ~[spring-beans-7.0.7.jar:7.0.7]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:977) ~[spring-context-7.0.7.jar:7.0.7]







    	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:93) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:443) ~[spring-orm-7.0.7.jar:7.0.7]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:436) ~[spring-orm-7.0.7.jar:7.0.7]
	... 110 common frames omitted

[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 26.04 s <<< FAILURE! -- in com.example.chat_service.ChatServiceApplicationTests
[ERROR] com.example.chat_service.ChatServiceApplicationTests.contextLoads -- Time elapsed: 0.027 s <<< ERROR!
java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@70ca8e72 testClass = com.example.chat_service.ChatServiceApplicationTests, locations = [], classes = [com.example.chat_service.ChatServiceApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.PropertyMappingContextCustomizer@0, org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@165b2f7f, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@38f57b3d, org.springframework.boot.test.autoconfigure.OnFailureConditionReportContextCustomizerFactory$OnFailureConditionReportContextCustomizer@64df9a61, org.springframework.test.context.support.DynamicPropertiesContextCustomizer@0, org.springframework.boot.webmvc.test.autoconfigure.WebDriverContextCustomizer@46f699d5, org.springframework.boot.web.server.context.SpringBootTestRandomPortContextCustomizer@768ccdc5, org.springframework.boot.test.context.SpringBootTestAnnotation@80adc723], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.lambda$loadContext$0(DefaultCacheAwareContextLoaderDelegate.java:195)
	at org.springframework.test.context.cache.DefaultContextCache.put(DefaultContextCache.java:214)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:160)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:128)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:200)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:139)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:242)
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:184)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:151)
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:174)
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:596)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/hibernate/autoconfigure/HibernateJpaConfiguration.class]: Unable to build Hibernate SessionFactory  [persistence unit: default] ; nested exception is java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DB_URL}
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1817)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:603)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:977)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:621)
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756)
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:321)
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$2(SpringBootContextLoader.java:155)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1465)
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:600)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:155)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:114)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:247)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.lambda$loadContext$0(DefaultCacheAwareContextLoaderDelegate.java:167)
	... 21 more
Caused by: jakarta.persistence.PersistenceException: Unable to build Hibernate SessionFactory  [persistence unit: default] ; nested exception is java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DB_URL}
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:448)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:411)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:419)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1864)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1813)
	... 41 more
Caused by: java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DB_URL}
	at com.zaxxer.hikari.util.DriverDataSource.<init>(DriverDataSource.java:116)
	at com.zaxxer.hikari.pool.PoolBase.initializeDataSource(PoolBase.java:336)
	at com.zaxxer.hikari.pool.PoolBase.<init>(PoolBase.java:121)
	at com.zaxxer.hikari.pool.HikariPool.<init>(HikariPool.java:90)
	at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:111)
	at org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider.getConnection(DataSourceConnectionProvider.java:137)
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator$ConnectionProviderJdbcConnectionAccess.obtainConnection(JdbcEnvironmentInitiator.java:508)
	at org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl.getIsolatedConnection(DdlTransactionIsolatorNonJtaImpl.java:44)
	at org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl.getIsolatedConnection(DdlTransactionIsolatorNonJtaImpl.java:37)
	at org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl.getJdbcConnection(ImprovedExtractionContextImpl.java:61)
	at org.hibernate.tool.schema.extract.spi.ExtractionContext.getQueryResults(ExtractionContext.java:41)
	at org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl.extractMetadata(SequenceInformationExtractorLegacyImpl.java:36)
	at org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl.initializeSequences(DatabaseInformationImpl.java:73)
	at org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl.<init>(DatabaseInformationImpl.java:63)
	at org.hibernate.tool.schema.extract.internal.CachingDatabaseInformationImpl.<init>(CachingDatabaseInformationImpl.java:43)
	at org.hibernate.tool.schema.internal.GroupedSchemaMigratorImpl.buildDatabaseInformation(GroupedSchemaMigratorImpl.java:112)
	at org.hibernate.tool.schema.internal.AbstractSchemaMigrator.doMigration(AbstractSchemaMigrator.java:83)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:269)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$1(SchemaManagementToolCoordinator.java:101)
	at java.base/java.util.HashMap.forEach(HashMap.java:1429)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:100)
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:35)
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:33)
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:323)
	at org.hibernate.internal.SessionFactoryRegistry.instantiateSessionFactory(SessionFactoryRegistry.java:64)
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:437)
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1456)
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:93)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:443)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:436)
	... 45 more

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   ChatServiceApplicationTests.contextLoads » IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@70ca8e72 testClass = com.example.chat_service.ChatServiceApplicationTests, locations = [], classes = [com.example.chat_service.ChatServiceApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.PropertyMappingContextCustomizer@0, org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@165b2f7f, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@38f57b3d, org.springframework.boot.test.autoconfigure.OnFailureConditionReportContextCustomizerFactory$OnFailureConditionReportContextCustomizer@64df9a61, org.springframework.test.context.support.DynamicPropertiesContextCustomizer@0, org.springframework.boot.webmvc.test.autoconfigure.WebDriverContextCustomizer@46f699d5, org.springframework.boot.web.server.context.SpringBootTestRandomPortContextCustomizer@768ccdc5, org.springframework.boot.test.context.SpringBootTestAnnotation@80adc723], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[INFO] 
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:12 min
[INFO] Finished at: 2026-05-11T08:38:18+02:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.5:test (default-test) on project chat_service: 
[ERROR] 
[ERROR] See /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 


) this is a small part of my error logs if you see nothing tell me  and i provide full logsss