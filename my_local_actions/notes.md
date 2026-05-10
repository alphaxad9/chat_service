mvn clean install
mvn spring-boot:run

post_service/
├── Dockerfile
├── docker-compose.yml
├── helm/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── curls/
│   ├── create_post.md
│   ├── get_post.md
│   └── health.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/postservice/
│   │   │
│   │   │       ├── PostServiceApplication.java
│   │   │       │
│   │   │       ├── shared/
│   │   │       │   ├── exceptions/
│   │   │       │   │   ├── DomainException.java
│   │   │       │   │   ├── NotFoundException.java
│   │   │       │   │   └── ValidationException.java
│   │   │       │   │
│   │   │       │   └── responses/
│   │   │       │       └── ApiResponse.java
│   │   │       │
│   │   │       ├── domain/
│   │   │       │   │
│   │   │       │   └── post/
│   │   │       │       ├── aggregates/
│   │   │       │       │   └── PostAggregate.java
│   │   │       │       │
│   │   │       │       ├── models/
│   │   │       │       │   └── Post.java
│   │   │       │       │
│   │   │       │       ├── valueobjects/
│   │   │       │       │   ├── PostId.java
│   │   │       │       │   └── PostContent.java
│   │   │       │       │
│   │   │       │       ├── exceptions/
│   │   │       │       │   └── PostDomainException.java
│   │   │       │       │
│   │   │       │       ├── repositories/
│   │   │       │       │   ├── PostCommandRepository.java
│   │   │       │       │   └── PostQueryRepository.java
│   │   │       │       │
│   │   │       │       └── commands/
│   │   │       │           ├── CreatePostCommand.java
│   │   │       │           └── DeletePostCommand.java
│   │   │       │
│   │   │       ├── application/
│   │   │       │   │
│   │   │       │   ├── dtos/
│   │   │       │   │   ├── CreatePostRequest.java
│   │   │       │   │   ├── UpdatePostRequest.java
│   │   │       │   │   ├── PostResponse.java
│   │   │       │   │   └── PostDTO.java
│   │   │       │   │
│   │   │       │   ├── services/
│   │   │       │   │   ├── PostCommandService.java
│   │   │       │   │   └── PostQueryService.java
│   │   │       │   │
│   │   │       │   ├── handlers/
│   │   │       │   │   ├── CreatePostHandler.java
│   │   │       │   │   ├── UpdatePostHandler.java
│   │   │       │   │   ├── DeletePostHandler.java
│   │   │       │   │   └── GetPostHandler.java
│   │   │       │   │
│   │   │       │   ├── interfaces/
│   │   │       │   │   └── UserServiceClient.java
│   │   │       │   │
│   │   │       │   └── factory/
│   │   │       │       └── PostFactory.java
│   │   │       │
│   │   │       ├── infrastructure/
│   │   │       │   │
│   │   │       │   ├── persistence/
│   │   │       │   │   ├── entities/
│   │   │       │   │   │   └── PostEntity.java
│   │   │       │   │   │
│   │   │       │   │   ├── mappers/
│   │   │       │   │   │   └── PostMapper.java
│   │   │       │   │   │
│   │   │       │   │   ├── jpa/
│   │   │       │   │   │   └── JpaPostRepository.java
│   │   │       │   │   │
│   │   │       │   │   └── repositories/
│   │   │       │   │       ├── PostCommandOrmRepository.java
│   │   │       │   │       └── PostQueryOrmRepository.java
│   │   │       │   │
│   │   │       │   ├── config/
│   │   │       │   │   ├── BeanConfig.java
│   │   │       │   │   ├── DatabaseConfig.java
│   │   │       │   │   ├── RedisConfig.java
│   │   │       │   │   └── SecurityConfig.java
│   │   │       │   │
│   │   │       │   ├── cache/
│   │   │       │   │   └── PostCacheService.java
│   │   │       │   │
│   │   │       │   ├── external/
│   │   │       │   │   ├── clients/
│   │   │       │   │   │   └── UserServiceHttpClient.java
│   │   │       │   │   │
│   │   │       │   │   └── responses/
│   │   │       │   │       └── UserResponse.java
│   │   │       │   │
│   │   │       │   └── security/
│   │   │       │       ├── JwtAuthenticationFilter.java
│   │   │       │       └── JwtService.java
│   │   │       │
│   │   │       └── api/
│   │   │           │
│   │   │           ├── health/
│   │   │           │   └── HealthController.java
│   │   │           │
│   │   │           ├── posts/
│   │   │           │   ├── PostCommandController.java
│   │   │           │   └── PostQueryController.java
│   │   │           │
│   │   │           └── advice/
│   │   │               └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       │
│   │       └── db/
│   │           └── migration/
│   │               └── V1__create_posts_table.sql
│
└── target/