# Sprint 3 â€” Department Management and JPA Relationships

**Project:** Nexus Platform
**Backend module:** `backend/employee-service`
**Branch:** `feature/sprint-3-department-management`
**Status:** COMPLETED — Final Maven verification successful, 21 automated tests passed, committed and merged into develop.
**Latest automated result:** 21 tests passed, 0 failures, 0 errors (`mvn test`, 12 October 2026).

> These notes capture the implementation and learning checkpoints completed during Sprint 3. They describe verified behavior from our development sessions, not an independent line-by-line audit of the current local source files.

## 1. Sprint goal

Extend the existing Employee API into a realistic workforce-management domain by introducing Departments and connecting Employees to Departments using relational database relationships. Preserve the existing API capabilitiesâ€”CRUD, validation, pagination, sorting, and structured errorsâ€”while introducing relationship-aware operations and deletion protection.

**Outcomes:** Department CRUD; Employeeâ€“Department many-to-one and one-to-many mappings; optional Employee Department assignment; Department-based Employee filtering; protection against deleting occupied Departments; Postman verification and layered automated tests.

## 2. Starting point and architecture

Sprint 2 already provided Employee CRUD, DTO-based API contracts, validation, exception handling, first-name search, pagination and sorting, PostgreSQL persistence, and repository/service/controller tests.

The service follows this flow:

```text
HTTP request â†’ Controller â†’ Service â†’ Spring Data Repository
             â†’ Hibernate / JPA â†’ JDBC â†’ PostgreSQL
```

**Tech used:** Java 21, Spring Boot 4.1.0, Spring Data JPA / Hibernate, PostgreSQL (development), H2 (`@DataJpaTest`), Maven, JUnit, Mockito, MockMvc, and Postman.

The Department domain stays **inside the existing `employee-service`**, not in a new microservice. This keeps the domain and its database relationship simple while we learn the fundamentals.

## 3. Relational database concepts learned

### 3.1 Normalization, primary keys, and foreign keys

Rather than storing Department names repeatedly in every Employee row, we use two tables:

```text
departments
  id BIGINT PRIMARY KEY
  name VARCHAR(...)

employees
  id BIGINT PRIMARY KEY
  first_name VARCHAR(...)
  last_name VARCHAR(...)
  email VARCHAR(...)
  department_id BIGINT NULL REFERENCES departments(id)
```

A **primary key (PK)** identifies a row. A **foreign key (FK)** refers to a row in another table and allows the database to enforce referential integrity. Here `employees.department_id` references `departments.id`.

Example: Alex has `department_id = 1`, which references Engineering (`departments.id = 1`). A `NULL` department ID means the Employee is currently unassigned. We intentionally allowed nulls because existing Employees had no Department when this feature was introduced.

### 3.2 Cardinality and ownership

- **Many-to-One:** many Employees can belong to one Department.
- **One-to-Many:** one Department can have many Employees; this is the reverse view of the same FK relationship.
- **One-to-One:** each side has at most one corresponding row.
- **Many-to-Many:** usually modeled through a junction table, e.g., later Employeeâ€“Project assignments.

The **owning side** is `Employee.department`, because it defines `@JoinColumn` and maps the actual FK. `Department.employees` is the inverse side. Ownership is determined by mapping/FK configuration, **not** simply by which side is â€œmany.â€

### 3.3 JOINs and object relationships

Java stores an object reference (`Employee.department`), while the relational database stores the FK value (`employees.department_id`). SQL JOINs can combine data across tables, but a filter on the FK alone often does not require a JOIN.

## 4. Department domain and CRUD

### 4.1 Entity

```java
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // No-argument constructor, name constructor, getters and setter
}
```

`@Entity` makes the class persistent; `@Table` chooses the table; `@Id` identifies the PK; `IDENTITY` uses database-generated IDs. A no-argument constructor is needed by JPA. A `Long` ID can remain `null` until persisted.

### 4.2 Repository and derived queries

```java
public interface DepartmentRepository
        extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
```

Spring Data generates query implementations from method names. The first method checks for a duplicate name ignoring case. The second excludes the row being updated, allowing an unchanged name on the same Department.

### 4.3 DTO contracts and validation

- `CreateDepartmentRequest`: name with `@NotBlank`.
- `UpdateDepartmentRequest`: name with `@NotBlank`.
- `DepartmentResponse`: `id`, `name`.

We kept entities separate from API DTOs. Request DTOs use a no-argument constructor and getters/setters; response DTOs use a parameterized constructor and getters. Names are trimmed before storage. The API does not serialize JPA entities directly.

### 4.4 Department endpoints

| Method | Path                    | Behavior                                        |
| ------ | ----------------------- | ----------------------------------------------- |
| POST   | `/api/departments`      | Create; `201 Created`                           |
| GET    | `/api/departments`      | List Departments; `200 OK`                      |
| GET    | `/api/departments/{id}` | Fetch one; `200` or `404`                       |
| PUT    | `/api/departments/{id}` | Update; `200`, `400`, `404`, or `409`           |
| DELETE | `/api/departments/{id}` | Delete empty Department; `204`, `404`, or `409` |

The list endpoint returns a simple list because Departments are small reference data and useful for future dropdowns; Employee listing retains pagination.

### 4.5 Exceptions and HTTP semantics

- `DepartmentNotFoundException` â†’ **404 Not Found**.
- `DuplicateDepartmentNameException` â†’ **409 Conflict**.
- `DepartmentHasEmployeesException` â†’ **409 Conflict**.
- Invalid blank name (`@Valid` / `@NotBlank`) â†’ **400 Bad Request**.

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to structured error bodies, using the established project convention:

```json
{
  "error": "Conflict",
  "message": "Cannot delete department with id: 1 because employees are assigned to it"
}
```

Controllers return DTOs directly and use `@ResponseStatus` for special success statuses; exception handlers use `ResponseEntity`.

## 5. Mapping Employee â†” Department

### 5.1 Owning side: `@ManyToOne`

```java
// Employee.java
@ManyToOne
@JoinColumn(name = "department_id")
private Department department;

public Department getDepartment() { return department; }
public void setDepartment(Department department) {
    this.department = department;
}
```

Hibernate added `employees.department_id` and an FK referencing `departments`. Existing Employee rows remained intact with `NULL` Department IDs. The initial mapping deliberately did not force Department assignment.

### 5.2 Inverse side: `@OneToMany`

```java
// Department.java
@OneToMany(mappedBy = "department")
private List<Employee> employees = new ArrayList<>();

public List<Employee> getEmployees() { return employees; }
```

`mappedBy = "department"` names the **Java field `Employee.department`**, not the database column `department_id`. This creates a bidirectional Java navigation model without adding a second FK or a join table. Initializing the collection avoids a null collection. We intentionally did not add cascading deletes, orphan removal, or extra helper methods at this stage.

**Key lesson:** Bidirectional Java references do not imply two database relationships.

### 5.3 DTO boundary prevents accidental recursive JSON

`DepartmentResponse` exposes `id` and `name`, not `employees`; `EmployeeResponse` exposes `departmentId`, not a nested Department entity. This keeps responses stable and avoids entity graph serialization and possible recursion.

## 6. Relationship-aware Employee operations

### 6.1 Employee DTOs

`CreateEmployeeRequest` and `UpdateEmployeeRequest` gained an optional `Long departmentId`. `EmployeeResponse` gained `departmentId` as a fifth constructor field. Existing tests were updated to use the new constructor; no compatibility-only overloaded constructor was added.

The response mapping uses a null-safe ternary:

```java
Long departmentId = employee.getDepartment() != null
        ? employee.getDepartment().getId()
        : null;
```

This demonstrates the Java conditional operator and avoids dereferencing a null Department.

### 6.2 Resolve Department IDs in the service

```java
private Department resolveDepartment(Long departmentId) {
    if (departmentId == null) return null;

    return departmentRepository.findById(departmentId)
            .orElseThrow(() ->
                    new DepartmentNotFoundException(departmentId));
}
```

`createEmployee()` resolves the Department before saving. `updateEmployee()` resolves and sets the Department while preserving existing email uniqueness behavior. With current **PUT** semantics, a null `departmentId` clears the assignment. A nonexistent Department produces 404 rather than storing an invalid reference.

**Observed Postman results:** Alex (Employee 12) was created with Engineering (Department 1), reassigned to Finance (then Department 3), cleared using null, and reassigned to Engineering. Invalid Department 999 returned 404. Finance was later deleted in the Department deletion tests.

## 7. Department filtering with existing pagination

### 7.1 Derived repository query

```java
Page<Employee> findByDepartmentId(
        Long departmentId, Pageable pageable);
```

Spring Data traverses `Employee.department.id`; the Employee entity does **not** need a separate Java `departmentId` field.

### 7.2 Controller and service behavior

The existing endpoint gained an optional parameter:

```http
GET /api/employees?departmentId=1&page=0&size=2&sortBy=firstName&direction=asc
```

```java
@RequestParam(required = false) Long departmentId
```

Service logic:

```java
if (departmentId == null) {
    employeePage = employeeRepository.findAll(pageable);
} else {
    if (!departmentRepository.existsById(departmentId)) {
        throw new DepartmentNotFoundException(departmentId);
    }
    employeePage = employeeRepository.findByDepartmentId(
            departmentId, pageable);
}
```

The existing pagination validation, sorting, DTO mapping, and `PageResponse<EmployeeResponse>` contract were retained.

**Semantics:**

| Request                               | Expected behavior                |
| ------------------------------------- | -------------------------------- |
| No `departmentId`                     | Original paginated Employee list |
| Existing Department with Employees    | Filtered paginated list          |
| Existing Department without Employees | `200 OK` and empty page          |
| Nonexistent Department                | `404 Not Found`                  |

### 7.3 Observed SQL and lessons

For Engineering, Hibernate checked Department existence, then queried `employees where department_id=?` with sorting and page limits. Because the FK is already in `employees`, that filter does not require a JOIN. In our logs, accessing the Department relationship during response mapping triggered an additional Department SELECT; this is a real example to revisit when learning fetching strategies and N+1 queries.

**Postman checks passed:** no filter (9 total Employees, page size 5), Engineering filter (Alex only), empty Finance filter (empty page), invalid Department 999 (404), and combined filtering + pagination + first-name ascending sort.

## 8. Protecting Department deletion

### 8.1 Business rule

**Do not delete a Department while Employees are assigned to it.** Do not cascade-delete Employees or silently unassign them.

```java
// EmployeeRepository.java
boolean existsByDepartmentId(Long departmentId);
```

The derived query checks for at least one assigned Employee without loading every matching Employee.

### 8.2 Service implementation

```java
@Transactional
public void deleteDepartment(Long id) {
    Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new DepartmentNotFoundException(id));

    if (employeeRepository.existsByDepartmentId(id)) {
        throw new DepartmentHasEmployeesException(id);
    }

    departmentRepository.delete(department);
    logger.info("Department deleted successfully with id={}", id);
}
```

`DepartmentService` now uses constructor injection for both `DepartmentRepository` and `EmployeeRepository`.

`@Transactional` groups the operations into a transaction, **but does not eliminate races**: another transaction could assign an Employee after the existence check. The database FK remains the final safeguard. Future hardening should translate a late FK constraint violation into an appropriate API response and consider transactional/concurrency strategies.

### 8.3 Verified outcomes

- DELETE Engineering (ID 1) with Alex assigned â†’ **409 Conflict**, no DELETE executed.
- DELETE empty Finance (ID 3) â†’ **204 No Content**, DB row deletion verified.
- DELETE missing Department 999 â†’ **404 Not Found**.

The Hibernate existence query selected a limited Employee ID (`fetch first ? rows only`), showing an efficient existence check. A DELETE statement appeared adjacent to a later test log; do not attribute SQL to a particular HTTP request without its timestamps/context.

## 9. Automated testing â€” final 21/21

### 9.1 Testing layers

| Layer                                | What is real                   | What is mocked      | Main purpose                                             |
| ------------------------------------ | ------------------------------ | ------------------- | -------------------------------------------------------- |
| Repository (`@DataJpaTest`)          | Spring Data JPA, Hibernate, H2 | None for repository | Verify generated queries and relationship mapping        |
| Service (JUnit + Mockito)            | Service logic                  | Repositories        | Verify business branches and interactions                |
| Controller (`@WebMvcTest` + MockMvc) | MVC binding/controller         | Service             | Verify HTTP parameter binding, response JSON, delegation |

### 9.2 New Sprint 3 tests

- Repository: `findByDepartmentId` with a persisted Department and Employee.
- Service: filter existing Department, reject nonexistent Department; verify no Employee query when Department is missing.
- Controller: `?departmentId=1` binds to `Long 1L` and is forwarded to the service.
- DepartmentService: reject deletion when Employees exist; allow deletion when empty; reject deletion of nonexistent Department and verify no EmployeeRepository interactions.

**Final test distribution (as observed):**

```text
EmployeeRepositoryTest    4
EmployeeServiceTest       9
DepartmentServiceTest     3
Other tests               5
---------------------------
TOTAL                    21
Failures                  0
Errors                    0
```

**Result:** `mvn test` â†’ `BUILD SUCCESS` on 12 October 2026. A final `mvn clean verify` is still recommended before commit.

### 9.3 Mockito concepts learned

- `mock(...)` creates a test double.
- `when(...).thenReturn(...)` stubs behavior.
- `verify(...)` checks interactions.
- `never()` verifies a method was not called.
- `verifyNoInteractions(...)` confirms an unused dependency was not touched.
- `eq(...)`, `any(...)`, and `anyLong()` match arguments.
- `assertThrows(...)` checks exceptions.
- `Optional.empty()` simulates a missing record.

One initial test run failed with a `NullPointerException` because `departmentRepository` was null in the test instance. Initializing both repository mocks in `@BeforeEach` **before** constructing `EmployeeService` resolved it. No production null-check workaround was needed.

A mock-based success log (`Department deleted successfully`) does not mean a real database row was deleted. Similarly, the `Employee created successfully with id=null` unit-test log comes from a mocked persistence fixture, not necessarily a production defect.

## 10. Java and Spring revision checklist

- **Java objects and constructors:** no-arg JPA constructors, constructor injection, nullable `Long` IDs.
- **Collections and Streams:** `List`, `ArrayList`, `.stream().map(this::mapToResponse).toList()`.
- **Method references:** `this::mapToResponse` instead of a simple mapping lambda.
- **Lambdas and Optional:** `orElseThrow(() -> new DepartmentNotFoundException(id))`.
- **Ternary operator:** safely convert optional Department reference to a nullable response ID.
- **Annotations:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `@Valid`, `@NotBlank`, `@RequestParam`, `@Transactional`, `@ExceptionHandler`.
- **Dependency injection:** services receive repositories through constructors; tests provide mocks manually.
- **REST semantics:** `201` create, `200` read/update, `204` delete, `400` invalid input, `404` missing resource, `409` business conflict.
- **Data access:** derived repository method names, FK filtering, pagination, transaction boundaries, database referential integrity.
- **Testing:** separate controller, service, and repository concerns.

## 11. Issues, limitations, and future improvements

1. **Database migrations:** `spring.jpa.hibernate.ddl-auto=update` is useful locally, but production schema changes should use versioned migrations (planned: Flyway).
2. **Fetching / N+1:** examine `@ManyToOne` fetch behavior, lazy loading, transaction boundaries, and the Spring `open-in-view` warning. Avoid accidentally loading entire entity graphs.
3. **Concurrent deletion:** the existence check is not atomic protection against new Employee assignments; DB FK constraints and proper error translation remain necessary.
4. **Case-insensitive Department uniqueness:** service-level `existsByNameIgnoreCase` is not enough to guarantee uniqueness under concurrent writes; use a suitable database constraint/index in a later migration.
5. **Test isolation:** the broad Spring context test currently relies on local PostgreSQL under the dev profile; introduce an isolated test profile/test database.
6. **H2 vs PostgreSQL:** H2 repository tests are useful but cannot prove all PostgreSQL-specific behavior.
7. **Pagination stability:** consider a secondary ID sort when sorting on nonunique names.
8. **Profile configuration:** externalize the active profile instead of hardcoding `dev` for all environments.
9. **API error hardening:** review default framework errors (including 405 responses) for safe, consistent production responses.
10. **Test fixture quality:** improve mock fixtures that log null generated IDs.
11. **No premature stack expansion:** postpone security, Kafka, microservices, and frontend decisions until persistence/API foundations are reliable.

## 12. Sprint completion and Git checklist

The implementation and tests are ready for final review, but Sprint 3 should only be marked **merged/closed** after these checkpoints:

```powershell
# At repository root
git branch --show-current
git status
git diff --check

# At backend/employee-service
mvn clean verify

# Back at repository root
git add backend/employee-service docs/sprint-notes/sprint-3.md
git diff --cached --stat
git diff --cached --check
git status
git commit -m "feat(department): complete sprint 3 department management and relationships"
git push -u origin feature/sprint-3-department-management
git switch develop
git merge --ff-only feature/sprint-3-department-management
git push origin develop
git status
```

Do not stage credentials, generated build artifacts, or unrelated files. If the fast-forward merge fails, inspect branch history rather than forcing it.

## 13. Key takeaways

1. Model relationships with normalized tables and enforced foreign keys; represent them as object references in JPA.
2. The owning side controls the FK mapping; `mappedBy` points to the other entity's Java field.
3. Keep JPA entities separate from API DTOs to protect contracts and avoid recursive serialization.
4. Validate referenced IDs and express business conflicts with meaningful HTTP status codes.
5. Extend existing pagination/filter infrastructure instead of creating duplicate API paths.
6. Use repository tests for persistence behavior, service tests for decisions, and controller tests for HTTP binding.
7. Treat the database as the final integrity authority, especially when concurrency is involved.

**Next recommended sprint:** Sprint 4 â€” Production-ready persistence and API foundations (Flyway migrations, isolated tests, JPA fetching/N+1, transactions, and database constraint handling).

---

## Sprint 3 — Final Closure Record

### Final Build Verification

- Command: `mvn clean verify`
- Result: BUILD SUCCESS
- Automated tests: 21
- Failures: 0
- Errors: 0
- Executable Spring Boot JAR successfully generated.

### Git Closure

- Feature branch: `feature/sprint-3-department-management`
- Target branch: `develop`
- Commit: `1d9429f`
- Commit message: `feat(department): complete sprint 3 department management and relationships`
- Merge strategy: Fast-forward
- Remote branch: `origin/develop`
- Final working tree: Clean

### Lessons Learned

- PowerShell environment variables set using `$env:` are specific to the current process and its child processes.
- The application-context test currently requires the PostgreSQL `DB_PASSWORD` environment variable.
- Future improvements should isolate integration tests from the local development database.

### Sprint Status

Sprint 3 is officially completed and merged into develop.
