# Backend Package - Java Source Code

**Parent:** `../../AGENTS.md` | **Files:** 108 Java | **Packages:** 13

---

## OVERVIEW

Core Spring Boot backend application implementing student union HR management with AI capabilities. Layered architecture: Controller → Service → Mapper (MyBatis-Plus).

---

## STRUCTURE

```
com.redmoon2333/
├── Main.java              # Application entry point
├── annotation/            # 2 custom annotations (@RequireMemberRole, @RequireMinisterRole)
├── aspect/                # 1 AOP aspect (PermissionAspect for logging)
├── config/                # 16 configuration classes → SEE config/AGENTS.md
├── controller/            # 10 REST controllers (Auth, AI, Activity, Material, User, etc.)
├── dto/                   # 28 data transfer objects (Request/Response wrappers)
├── entity/                # 9 JPA entities (User, Activity, Material, DailyImage, etc.)
├── enums/                 # Enumerations (Role, Status, FileType, etc.)
├── exception/             # GlobalExceptionHandler + custom exceptions
├── mapper/                # 9 MyBatis-Plus mappers (XML-based queries)
├── service/               # 12 business services → SEE service/AGENTS.md
├── util/                  # 10 utility classes (JwtUtil, FileUtil, OssUtil, etc.)
└── validation/            # Custom JSR-303 validators
```

---

## WHERE TO LOOK

| Task | Location | Pattern |
|------|----------|---------|
| **Add REST endpoint** | `controller/` | Extend existing controller or create new one |
| **Add business logic** | `service/impl/` | Create service interface + implementation |
| **Add database table** | `entity/` + `mapper/` + `resources/mapper/*.xml` | Entity + Mapper + XML |
| **Add data validation** | `dto/` + `validation/` | Use @Valid + custom constraint annotations |
| **Add role-based auth** | `annotation/` | Use @RequireMemberRole or @RequireMinisterRole |
| **Modify AI behavior** | `service/AiService.java` + `resources/prompttemplate/` | Service logic + YAML prompts |
| **Configure Redis** | `config/RedisConfig.java` | Connection, vector store, cache patterns |
| **Add file upload** | `service/FileService.java` + `util/OssUtil.java` | Local or OSS storage |

---

## LAYER ARCHITECTURE

**Controller Layer:**
- RESTful endpoints with `@RestController`
- Input validation with `@Valid`
- Role-based authorization with `@RequireMemberRole` / `@RequireMinisterRole`
- Return unified response format via `GlobalExceptionHandler`

**Service Layer:**
- Business logic encapsulation
- Transaction management with `@Transactional`
- Service interfaces + implementations (separation of concerns)
- Key services: `AiService`, `RagService`, `ActivityService`, `MaterialService`

**Mapper Layer (MyBatis-Plus):**
- Interface-based mappers extending `BaseMapper<T>`
- XML files for complex queries (`resources/mapper/*.xml`)
- Built-in CRUD operations from MP
- Logical delete support (`deleted` field)

**Entity Layer:**
- JPA entities with `@TableName`
- Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Logical delete field: `@TableLogic` on `deleted` column
- Auto-fill fields: `@TableField(fill = FieldFill.INSERT)` for timestamps

---

## KEY PATTERNS

### Role-Based Authorization

```java
@RequireMemberRole("Access materials")
public ResponseEntity<?> getMaterials() { }

@RequireMinisterRole("Delete activity")
public ResponseEntity<?> deleteActivity(@PathVariable Long id) { }
```

### MyBatis-Plus with Logical Delete

```java
@TableName("user")
public class User {
    @TableLogic
    private Integer deleted;  // 0 = active, 1 = deleted
}
```

### Global Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(400).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }
}
```

### JWT Authentication Filter

```java
// In SecurityConfig.java
http.addAuthenticationFilter(new JwtAuthenticationFilter(jwtUtil));
```

### AI Streaming Response

```java
@ApiOperation("AI 流式对话")
public void chatStream(@RequestBody ChatRequest request, ServletOutputStream output) {
    aiService.streamChat(request, output);  // SSE-based streaming
}
```

---

## DEPENDENCIES (Key)

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.3.5 | Core framework |
| Spring AI | 1.0.0 | AI abstraction layer |
| MyBatis-Plus | 3.5.12 | ORM with built-in pagination |
| Spring Security | 6.x | Authentication & authorization |
| Redis + Jedis | 7.x | Cache + vector storage |
| JWT (jjwt) | 0.11.5 | Token-based auth |
| Hutool | 5.8.22 | Java utilities |
| Lombok | 1.18.38 | Boilerplate reduction |
| Aliyun OSS | 3.17.2 | Object storage (optional) |
| Apache POI | 5.2.5 | Excel processing |
| PDFBox | 2.0.30 | PDF processing |

---

## NOTES

- **No PageHelper plugin** - Use MyBatis-Plus built-in `Page<T>` API
- **Logical delete** - All entities have `deleted` field (0 = active, 1 = deleted)
- **Transaction management** - Use `@Transactional` on service methods
- **Error codes** - Unified error response format with business-specific codes
- **AOP logging** - `PermissionAspect` logs all permission-protected method calls
- **No backend tests yet** - `spring-boot-starter-test` configured but no test files

---

## HIERARCHY

- **Parent:** `../../AGENTS.md` (root)
- **Children:**
  - `config/AGENTS.md` - Configuration deep-dive
  - `service/AGENTS.md` - Business logic details