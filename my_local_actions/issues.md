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
[INFO] Compiling 20 source files with javac [debug parameters release 21] to target/classes
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
12:51:49.379 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.example.chat_service.ChatServiceApplicationTests]: ChatServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
12:51:49.562 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.example.chat_service.ChatServiceApplication for test class com.example.chat_service.ChatServiceApplicationTests
12:51:49.717 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.example.chat_service.ChatServiceApplicationTests]: ChatServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
12:51:49.719 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.example.chat_service.ChatServiceApplication for test class com.example.chat_service.ChatServiceApplicationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.0.6)

2026-05-10T12:51:50.335+02:00  INFO 165523 --- [chat-service] [           main] c.e.c.ChatServiceApplicationTests        : Starting ChatServiceApplicationTests using Java 21.0.10 with PID 165523 (started by ishimwe in /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service)
2026-05-10T12:51:50.341+02:00 DEBUG 165523 --- [chat-service] [           main] c.e.c.ChatServiceApplicationTests        : Running with Spring Boot v4.0.6, Spring v7.0.7
2026-05-10T12:51:50.342+02:00  INFO 165523 --- [chat-service] [           main] c.e.c.ChatServiceApplicationTests        : No active profile set, falling back to 1 default profile: "default"
2026-05-10T12:51:51.102+02:00  INFO 165523 --- [chat-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-05-10T12:51:51.121+02:00  INFO 165523 --- [chat-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 5 ms. Found 0 JPA repository interfaces.
2026-05-10T12:51:52.067+02:00  INFO 165523 --- [chat-service] [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-05-10T12:51:52.162+02:00  INFO 165523 --- [chat-service] [           main] org.hibernate.orm.core                   : HHH000001: Hibernate ORM core version 7.2.12.Final
2026-05-10T12:51:52.912+02:00  INFO 165523 --- [chat-service] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-05-10T12:51:52.950+02:00  INFO 165523 --- [chat-service] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-05-10T12:51:53.480+02:00  INFO 165523 --- [chat-service] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@b340615
2026-05-10T12:51:53.483+02:00  INFO 165523 --- [chat-service] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-05-10T12:51:53.524+02:00  WARN 165523 --- [chat-service] [           main] org.hibernate.orm.deprecation            : HHH90000025: PostgreSQLDialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-05-10T12:51:53.563+02:00  INFO 165523 --- [chat-service] [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:5432/chatdb1]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.9
	Default catalog/schema: chatdb1/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-05-10T12:51:54.818+02:00  INFO 165523 --- [chat-service] [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-05-10T12:51:54.890+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create table posts (
        id UUID not null,
        author_id UUID not null,
        content varchar(5000) not null,
        created_at timestamp(6) not null,
        image_url varchar(2048),
        is_deleted boolean not null,
        updated_at timestamp(6) not null,
        primary key (id)
    )
Hibernate: 
    create table posts (
        id UUID not null,
        author_id UUID not null,
        content varchar(5000) not null,
        created_at timestamp(6) not null,
        image_url varchar(2048),
        is_deleted boolean not null,
        updated_at timestamp(6) not null,
        primary key (id)
    )
2026-05-10T12:51:54.900+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_author 
       on posts (author_id)
Hibernate: 
    create index idx_posts_author 
       on posts (author_id)
2026-05-10T12:51:54.907+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_deleted 
       on posts (is_deleted)
Hibernate: 
    create index idx_posts_deleted 
       on posts (is_deleted)
2026-05-10T12:51:54.913+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_created 
       on posts (created_at)
Hibernate: 
    create index idx_posts_created 
       on posts (created_at)
2026-05-10T12:51:54.919+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_updated 
       on posts (updated_at)
Hibernate: 
    create index idx_posts_updated 
       on posts (updated_at)
2026-05-10T12:51:54.925+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_author_active 
       on posts (author_id, is_deleted)
Hibernate: 
    create index idx_posts_author_active 
       on posts (author_id, is_deleted)
2026-05-10T12:51:54.931+02:00 DEBUG 165523 --- [chat-service] [           main] org.hibernate.SQL                        : 
    create index idx_posts_active_created 
       on posts (is_deleted, created_at)
Hibernate: 
    create index idx_posts_active_created 
       on posts (is_deleted, created_at)
2026-05-10T12:51:54.939+02:00  INFO 165523 --- [chat-service] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-05-10T12:51:55.120+02:00  WARN 165523 --- [chat-service] [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-05-10T12:51:55.709+02:00  INFO 165523 --- [chat-service] [           main] c.e.c.ChatServiceApplicationTests        : Started ChatServiceApplicationTests in 5.887 seconds (process running for 7.58)
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (/home/ishimwe/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.245 s -- in com.example.chat_service.ChatServiceApplicationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- maven-jar-plugin:3.4.2:jar (default-jar) @ chat_service ---
[INFO] Building jar: /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/chat_service-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot-maven-plugin:4.0.6:repackage (repackage) @ chat_service ---
[INFO] Replacing main artifact /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/chat_service-0.0.1-SNAPSHOT.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] The original artifact has been renamed to /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/chat_service-0.0.1-SNAPSHOT.jar.original
[INFO] 
[INFO] --- maven-install-plugin:3.1.4:install (default-install) @ chat_service ---
[INFO] Installing /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/pom.xml to /home/ishimwe/.m2/repository/com/example/chat_service/0.0.1-SNAPSHOT/chat_service-0.0.1-SNAPSHOT.pom
[INFO] Installing /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/chat_service-0.0.1-SNAPSHOT.jar to /home/ishimwe/.m2/repository/com/example/chat_service/0.0.1-SNAPSHOT/chat_service-0.0.1-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.487 s
[INFO] Finished at: 2026-05-10T12:51:57+02:00
[INFO] ------------------------------------------------------------------------
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ mvn spring-boot:run
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------------< com.example:chat_service >----------------------
[INFO] Building chat_service 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] >>> spring-boot-maven-plugin:4.0.6:run (default-cli) > test-compile @ chat_service >>>
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ chat_service ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] Copying 0 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- maven-compiler-plugin:3.14.1:compile (default-compile) @ chat_service ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 20 source files with javac [debug parameters release 21] to target/classes
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:testResources (default-testResources) @ chat_service ---
[INFO] skip non existing resourceDirectory /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.14.1:testCompile (default-testCompile) @ chat_service ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 1 source file with javac [debug parameters release 21] to target/test-classes
[INFO] 
[INFO] <<< spring-boot-maven-plugin:4.0.6:run (default-cli) < test-compile @ chat_service <<<
[INFO] 
[INFO] 
[INFO] --- spring-boot-maven-plugin:4.0.6:run (default-cli) @ chat_service ---
[INFO] Attaching agents: []

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.0.6)

2026-05-10T12:52:33.212+02:00  INFO 166254 --- [chat-service] [           main] c.e.chat_service.ChatServiceApplication  : Starting ChatServiceApplication using Java 21.0.10 with PID 166254 (/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/classes started by ishimwe in /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service)
2026-05-10T12:52:33.215+02:00 DEBUG 166254 --- [chat-service] [           main] c.e.chat_service.ChatServiceApplication  : Running with Spring Boot v4.0.6, Spring v7.0.7
2026-05-10T12:52:33.216+02:00  INFO 166254 --- [chat-service] [           main] c.e.chat_service.ChatServiceApplication  : No active profile set, falling back to 1 default profile: "default"
2026-05-10T12:52:33.701+02:00  INFO 166254 --- [chat-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-05-10T12:52:33.717+02:00  INFO 166254 --- [chat-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 5 ms. Found 0 JPA repository interfaces.
2026-05-10T12:52:34.416+02:00  INFO 166254 --- [chat-service] [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 8005 (http)
2026-05-10T12:52:34.432+02:00  INFO 166254 --- [chat-service] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-05-10T12:52:34.432+02:00  INFO 166254 --- [chat-service] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.21]
2026-05-10T12:52:34.481+02:00  INFO 166254 --- [chat-service] [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 1204 ms
2026-05-10T12:52:34.687+02:00  INFO 166254 --- [chat-service] [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-05-10T12:52:34.740+02:00  INFO 166254 --- [chat-service] [           main] org.hibernate.orm.core                   : HHH000001: Hibernate ORM core version 7.2.12.Final
2026-05-10T12:52:35.222+02:00  INFO 166254 --- [chat-service] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-05-10T12:52:35.256+02:00  INFO 166254 --- [chat-service] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-05-10T12:52:35.603+02:00  INFO 166254 --- [chat-service] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@6339e604
2026-05-10T12:52:35.604+02:00  INFO 166254 --- [chat-service] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-05-10T12:52:35.639+02:00  WARN 166254 --- [chat-service] [           main] org.hibernate.orm.deprecation            : HHH90000025: PostgreSQLDialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-05-10T12:52:35.663+02:00  INFO 166254 --- [chat-service] [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:5432/chatdb1]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.9
	Default catalog/schema: chatdb1/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-05-10T12:52:36.527+02:00  INFO 166254 --- [chat-service] [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-05-10T12:52:36.601+02:00  INFO 166254 --- [chat-service] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-05-10T12:52:36.703+02:00  WARN 166254 --- [chat-service] [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-05-10T12:52:37.030+02:00  INFO 166254 --- [chat-service] [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 8005 (http) with context path '/'
2026-05-10T12:52:37.037+02:00  INFO 166254 --- [chat-service] [           main] c.e.chat_service.ChatServiceApplication  : Started ChatServiceApplication in 4.379 seconds (process running for 4.845)
 i want to see if my tables are created 