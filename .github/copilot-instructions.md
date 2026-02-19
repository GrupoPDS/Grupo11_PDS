# Project Context: Library System (UFU - PDS)

- **Language:** Java 17+
- **Framework:** Spring Boot 3+
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **Docs:** OpenAPI (Swagger)

---

## Architectural Guidelines (Strictly Follow)

### Package Structure

| Package | Responsabilidade |
|---|---|
| `br.ufu.pds.library.core` | Domain entities, business exceptions, and functional interfaces. |
| `br.ufu.pds.library.infrastructure` | Database implementations (Repositories), external services, and concrete Service classes. |
| `br.ufu.pds.library.entrypoint` | REST Controllers, DTOs, and Exception Handlers. |
| `br.ufu.pds.library.config` | Global configurations (Swagger, CORS, Beans). |

---

## Coding Standards

### Dependency Injection
- **ALWAYS** use **Constructor Injection**.
- **NEVER** use `@Autowired` on fields.

### Entities vs DTOs
- **NEVER** expose `@Entity` in Controllers.
- Always use Java `record` as DTOs (Request/Response) in the `entrypoint` layer.

### Lombok
- Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` for Entities.

### Validation
- Use `jakarta.validation` (`@NotBlank`, `@NotNull`) inside DTOs.

### Testing
- Prioritize **JUnit 5** and **Mockito**.
- Use `MockMvc` for Integration Tests.

---

## Documentation

- All Controllers **must** have `@Operation` and `@ApiResponse` (Swagger) annotations.
- Code comments in **Portuguese (PT-BR)** are allowed for complex business logic.

---

## DevOps & Containerization

- Dockerfiles must use **Multi-Stage Builds** (Build image → Runtime image).
- Application must be runnable via `docker-compose up`.
