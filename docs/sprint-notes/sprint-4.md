# Sprint 4 — Production-Ready Persistence and API Foundations

## Status
Implementation and verification completed in the user's local Nexus Platform workspace; documentation and Git closure pending. Latest reported verification: `mvn clean verify` — 23 tests, 0 failures, 0 errors; executable Spring Boot JAR built. This document records observed results and does not imply independent inspection of the local repository.

## Goals
- Adopt version-controlled database schema migrations using Flyway.
- Preserve the existing development database through baselining.
- Verify V1 migration on a fresh PostgreSQL database.
- Isolate automated tests from local PostgreSQL credentials.
- Define service-level transaction boundaries and prove rollback behavior.
- Optimize employee-to-department fetching and disable Open Session in View.
- Return a controlled API response for database integrity violations.

## 1. Flyway and schema versioning
Flyway executes ordered SQL migration files and tracks their execution in `flyway_schema_history`. It is responsible for database schema evolution; Hibernate/JPA maps Java entities and performs persistence; PostgreSQL stores data and enforces constraints.

Added Spring Boot managed Maven dependencies:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Created `src/main/resources/db/migration/V1__initial_schema.sql` to reproduce the existing Sprint 1–3 schema: `departments` and `employees`, identity primary keys, unique employee email constraint (`uk_employees_email`), and employee department foreign key (`fkgy4qe3dnqrm3ktd76sxp7n4c2`). Existing columns include nullable `departments.name`, and nullable `employees.email`, `first_name`, `last_name`, and `department_id`.

Development configuration uses `spring.jpa.hibernate.ddl-auto=validate`, with:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
spring.flyway.baseline-version=1
spring.flyway.validate-on-migrate=true
```
`baseline-on-migrate=true` was used **temporarily** to onboard the existing `nexus` database, then disabled. The existing database has a version 1 **BASELINE** history entry, not an executed V1 SQL migration. The separately created empty `nexus_flyway_test` database successfully executed V1 with history `version=1`, `description=initial schema`, `type=SQL`, `success=true`. Its tables were verified: `departments`, `employees`, `flyway_schema_history`. Hibernate schema validation passed on startup.

**Production scenario:** For a future `joining_date` employee field, add a new `V2__add_employee_joining_date.sql` migration with `ALTER TABLE employees ADD COLUMN joining_date DATE;`, and update the entity/application code. Do not rewrite an already applied V1 migration. New columns are nullable unless an explicit backfill and constraint plan is designed. This V2 was discussed as an example; **it was not implemented in Sprint 4**.

## 2. Isolated automated testing
Added `src/test/resources/application-test.properties` and configured H2 in-memory storage:
```properties
spring.datasource.url=jdbc:h2:mem:nexus_test;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
```
The test-only profile intentionally uses Hibernate `create-drop` and disables PostgreSQL-specific Flyway migrations. The application context test was annotated `@ActiveProfiles("test")`. The user ran `mvn clean verify` after removing `DB_PASSWORD` and `SPRING_DATASOURCE_URL` from the terminal and reported 21 passing tests at that stage. Repository tests already used H2. Do not run the H2 test configuration against production data.

## 3. Transactions and rollback
Service write operations (`createEmployee`, `updateEmployee`, `deleteEmployee`, `createDepartment`, `updateDepartment`, `deleteDepartment`) use `@Transactional`. Read operations (`getAllEmployees`, `getEmployeeById`, `searchEmployeesByFirstName`, `getDepartmentById`, `getAllDepartments`) use `@Transactional(readOnly = true)`.

A service method is **not automatically transactional** merely because it has `@Service`; many inherited Spring Data JPA repository operations have their own transaction behavior. Service-level transactions span the complete business operation and DTO mapping. `readOnly=true` is an optimization hint, not an access-control mechanism. Spring's proxy-based transaction interception generally applies when calls enter through the Spring proxy; self-invocation bypasses that interception. Default `REQUIRED` propagation joins an existing transaction or creates one.

The existing `deleteDepartment` method loads the department, checks `employeeRepository.existsByDepartmentId(id)`, raises `DepartmentHasEmployeesException` when occupied, otherwise calls `departmentRepository.delete(department)`. It is transactional. The log message was refined to `Department deletion requested for id={}` because SQL flushing or transaction commit may happen later.

Created `DepartmentTransactionTest`, using `@SpringBootTest`, `@ActiveProfiles("test")`, `TransactionTemplate`, and `departmentRepository.saveAndFlush(...)`. The test deliberately throws `IllegalStateException` after a database INSERT is flushed, then asserts the department is absent after rollback. This demonstrates that **flush is not commit**. Focused test passed; full suite increased to 22 passing tests.

ACID: Atomicity (all-or-nothing changes), Consistency (constraints/invariants), Isolation (controlled concurrent visibility), Durability (committed data survives failure).

## 4. JPA relationship fetching and OSIV
Original `Employee.department` mapping used default eager fetching:
```java
@ManyToOne
@JoinColumn(name = "department_id")
private Department department;
```
One observed `GET /api/employees?page=0&size=20&sortBy=id&direction=asc` produced an employee SELECT followed by a department SELECT. Changed mapping to:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id")
private Department department;
```
The repeated request then produced only the employee SELECT. The API maps `employee.getDepartment().getId()` to `departmentId`, which Hibernate can normally read without initializing the full proxy. This is an **observed improvement from two SELECTs to one**, not proof that every future query avoids N+1.

Configured `spring.jpa.open-in-view=false` in development, production, and test profiles. This prevents relying on the web-request scope to lazily load entities after the service transaction. Read-only service transactions cover repository access and DTO mapping. An unexpected `LazyInitializationException` should prompt review of transaction/fetch boundaries, not automatic re-enabling of OSIV.

Postman checks after changes:
- `GET /api/employees?page=0&size=20` → 200, one employee SELECT.
- `GET /api/employees/1` → 200, one employee SELECT.
- `GET /api/departments` → 200, one department SELECT.
- `GET /api/employees?departmentId=1` → 200, department existence COUNT then filtered employee SELECT.

The two queries for department filtering are intentional: the existence check distinguishes nonexistent departments (404) from valid departments with zero employees (200 empty result). They are not evidence of N+1.

## 5. Database integrity and error handling
PostgreSQL's employee-to-department foreign key protects referential integrity even if an application-level precheck races with another transaction. Service checks improve business error messages but do not replace database constraints or eliminate races. A database violation can surface as `DataIntegrityViolationException`.

Added to `GlobalExceptionHandler`:
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
        DataIntegrityViolationException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Conflict");
    error.put("message",
        "The requested operation conflicts with existing database data.");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
}
```
Import `org.springframework.dao.DataIntegrityViolationException`. Removed duplicate `Map`/`HashMap` imports. Existing specific business exception handlers remain unchanged. The generic response avoids exposing SQL or constraint internals; it does not distinguish every unique or foreign-key violation.

Created `GlobalExceptionHandlerTest` as a focused unit test that calls the handler with a simulated `DataIntegrityViolationException` and verifies HTTP 409 and both response fields. **This does not simulate a real PostgreSQL race or verify all possible transaction-commit exception wrappers.** Focused test: 1 passed. Full `mvn clean verify`: **23 passed, 0 failures, 0 errors, BUILD SUCCESS**, executable JAR built.

## 6. Key lessons and interview-ready rules
1. Use Flyway for versioned, reviewable schema changes; use Hibernate `validate` rather than uncontrolled production schema updates.
2. Baseline existing populated databases; run initial migrations on genuinely empty databases to verify reproducibility.
3. Keep test databases isolated from developer and production databases.
4. Put transactions around complete service-layer business operations.
5. `flush()` sends SQL; `commit()` finalizes the transaction; rollback can undo flushed writes.
6. Prefer `LAZY` for relationships that are not always needed; inspect SQL before optimizing and consider targeted fetches for richer responses.
7. Disable OSIV for predictable service-layer persistence boundaries and map entities to DTOs within transactions.
8. Combine application-level validation with database constraints. Map expected integrity violations to safe, meaningful API responses.
9. A green unit test verifies its specific behavior, not untested concurrency or production deployment scenarios.

## 7. Verification history
- Existing `nexus`: Flyway version 1 BASELINE succeeded; historical records preserved.
- Empty `nexus_flyway_test`: V1 SQL applied, Hibernate validation succeeded.
- After H2 test isolation: `mvn clean verify` — 21 tests passed.
- After transaction rollback test: `mvn clean verify` — 22 tests passed.
- After lazy fetch, OSIV, and service transaction changes: 22 tests passed; four Postman GET checks returned 200.
- After integrity handler test: `mvn -Dtest=GlobalExceptionHandlerTest test` — 1 passed; `mvn clean verify` — 23 tests passed, executable JAR built.

## 8. Known limitations and future improvements
- V1 was baselined on the existing database, so its history entry does not verify equivalence between old schema and V1 beyond Hibernate's startup validation; a fresh migration test separately succeeded.
- H2 tests do not replace PostgreSQL integration tests for vendor-specific SQL, constraints, locking, and concurrency.
- The generic integrity handler test does not verify HTTP routing or actual DB concurrency; add PostgreSQL-backed integration tests when warranted.
- Department names are checked for duplicates in application code, but database-enforced case-insensitive uniqueness remains future work.
- Future schema changes should use new V2+ migrations with appropriate data backfills and compatibility planning.
- Avoid claiming a deployment-ready production system solely from the passing local tests.

## 9. Before committing Sprint 4
1. Confirm all expected source files, profile settings, imports, and tests match the implementation described here.
2. Verify no plaintext DB passwords or generated `target/` artifacts are staged.
3. Update `docs/project-context.md`, `docs/engineering-journal.md`, and any sprint index/status to accurately reflect Sprint 4.
4. Re-run `mvn clean verify` after final code/config edits; ensure 23 tests pass.
5. Review `git status` and `git diff`; only then stage, commit, push, and merge per the project's branch workflow.

**Documentation status:** Drafted from the user's reported implementation and logs; local repository content has not been independently inspected. No commit hash is recorded because Git closure is pending.
