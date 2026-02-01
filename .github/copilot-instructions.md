# Copilot Instructions for Clickr

This is a high-performance URL shortener built with Quarkus, GraalVM Native Image, PostgreSQL, and reactive programming patterns.

## Build, Test, and Run Commands

### Development
```bash
# Start dependencies (PostgreSQL + Redis)
docker-compose up -d

# Run in dev mode with hot reload
./mvnw quarkus:dev

# Access Swagger UI at http://localhost:8080/q/swagger-ui
```

### Testing
```bash
# Run unit tests
./mvnw test

# Run integration tests (requires Docker for Testcontainers)
./mvnw verify

# Run specific test class
./mvnw test -Dtest=UrlServiceTest

# Run specific test method
./mvnw test -Dtest=UrlServiceTest#testCreateShortUrl
```

### Building
```bash
# Build JVM version
./mvnw clean package

# Build native executable (requires GraalVM)
./mvnw package -Pnative

# Run native executable
./target/clickr-1.0.0-runner
```

### Code Formatting
```bash
# Check code formatting (Google Java Format)
./mvnw fmt:check

# Apply code formatting
./mvnw fmt:format
```

### Database Migrations
```bash
# Run Flyway migrations (requires Docker Compose running)
./mvnw flyway:migrate

# Clean database (dev only)
./mvnw flyway:clean

# Get migration info
./mvnw flyway:info
```

## Architecture Overview

### Tech Stack
- **Framework:** Quarkus 3.x with RESTEasy Reactive (fully reactive/non-blocking)
- **Database:** PostgreSQL with reactive Vert.x SQL client (no JPA/Hibernate)
- **Java Version:** Java 21+ (uses modern Java features)
- **Build Tool:** Maven
- **Testing:** JUnit 5, REST Assured, Quarkus Test framework

### Key Architecture Patterns

**Reactive All the Way Down**
- All operations return `Uni<T>` (reactive single-value stream from Mutiny)
- Never block - use `.flatMap()`, `.map()`, `.invoke()` for composition
- Database operations use reactive Vert.x SQL client (`io.vertx.mutiny.sqlclient.Pool`)
- HTTP responses are fully reactive with RESTEasy Reactive

**Layered Architecture**
```
resource/ (REST endpoints)
    ↓
service/ (Business logic)
    ↓
repository/ (Data access with raw SQL)
    ↓
domain/ (Entities and DTOs)
```

**No ORM**
- Uses raw SQL queries with parameterized statements (`Tuple.of(...)`)
- Entity mapping from database rows done manually via `Url.from(Row row)`
- SQL queries stored as multi-line text blocks (`"""..."""`) in repository classes
- PostgreSQL sequence (`urls_id_seq`) drives Base62 short code generation

**Base62 Encoding**
- Short codes generated from PostgreSQL sequence IDs
- Encoding: numeric ID → Base62 string (0-9, A-Z, a-z)
- Collision-free: sequence guarantees unique IDs
- See `ShortCodeGenerator.encode(long id)` for implementation

## Code Conventions

### Dependency Injection
- Use constructor injection with `@Inject` (not field injection)
- Services are `@ApplicationScoped`
- Always make injected fields `final`

Example:
```java
@ApplicationScoped
public class UrlService {
    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    
    @Inject
    public UrlService(UrlRepository urlRepository, ShortCodeGenerator shortCodeGenerator) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }
}
```

### Reactive Programming
- **Never** use blocking calls (no `.await().indefinitely()` in production code)
- Chain reactive operations with `.flatMap()` for dependent operations
- Use `.map()` for transformations
- Use `.invoke()` for side effects (logging)
- Return `Uni<T>` from service and repository methods

Example pattern:
```java
public Uni<UrlResponse> createShortUrl(CreateUrlRequest request) {
    return urlRepository.getNextSequenceValue()
        .flatMap(id -> {
            String shortCode = shortCodeGenerator.encode(id);
            return urlRepository.create(request.url(), shortCode);
        })
        .map(url -> UrlResponse.from(url, baseUrl))
        .invoke(response -> log.info("Created: {}", response));
}
```

### SQL Queries
- Store queries as constants using text blocks (`"""..."""`)
- Use parameterized queries with `Tuple.of(...)` for SQL injection prevention
- Schema name is `core` (all tables in core schema)
- Never concatenate user input into SQL strings

Example:
```java
private static final String FIND_QUERY = """
    SELECT id, short_code, original_url 
    FROM urls 
    WHERE short_code = $1
    """;

public Uni<Optional<Url>> findByShortCode(String shortCode) {
    return client.preparedQuery(FIND_QUERY)
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() 
            ? Optional.of(Url.from(iterator.next())) 
            : Optional.empty());
}
```

### Configuration
- Environment variables override `application.properties`
- Base URL configured via `clickr.base-url` property
- Database connection: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Use `@ConfigProperty` for injection in services

### OpenAPI Documentation
- Add `@Tag`, `@Operation`, `@APIResponses` to REST resources
- Document request/response schemas
- Swagger UI available at `/q/swagger-ui` in dev mode

### Logging
- Use SLF4J with `LoggerFactory.getLogger(Class.class)`
- Use `.trace()` for request/response logging
- Use `.info()` for business events (created, deleted)
- Use `.warn()` for expected errors (not found, validation)
- Use `.error()` with exception for unexpected errors

### Entity Mapping
- Domain entities have static `from(Row row)` factory methods
- DTOs use Java records for immutability
- Response DTOs have static factory methods (e.g., `UrlResponse.from(Url, String baseUrl)`)

## Project Structure

```
src/main/java/io/clickr/
├── domain/
│   ├── Url.java              # Entity with Row mapping
│   └── dto/
│       ├── CreateUrlRequest.java  # Request DTO (record)
│       └── UrlResponse.java       # Response DTO (record)
├── repository/
│   └── UrlRepository.java    # Data access with raw SQL
├── resource/
│   └── UrlResource.java      # REST endpoints (/api/v1/urls)
└── service/
    ├── UrlService.java       # Business logic
    └── ShortCodeGenerator.java  # Base62 encoding

src/main/resources/
├── application.properties    # Quarkus configuration
└── db/migration/
    ├── V0000__baseline_schema.sql
    └── V0001__create_tables_urls.sql  # Flyway migrations
```

## Important Notes

- **Schema:** All tables use `core` schema (configured in Flyway and JDBC URL)
- **Native Image:** Built with GraalVM for 20ms startup and 30MB memory footprint
- **Testing:** Tests use `@QuarkusTest` for integration tests with real Quarkus lifecycle
- **Security:** URL validation prevents SSRF (localhost, private IPs, javascript: URIs blocked)
- **Performance:** PostgreSQL sequence + Redis caching (planned) for sub-10ms redirects
