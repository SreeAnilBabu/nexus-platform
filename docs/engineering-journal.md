# Engineering Journal

## Sprint 0

### Day 1

Today I learned that Git does not store folder names; it stores the repository contents.

I also understood that the JVM executes bytecode, not Java source code or machine code directly.

---

### Questions

- How does Maven resolve transitive dependencies?
- How does Spring Boot discover beans?

---

## Sprint 1 — Employee Service Foundation (completed)

- Built the Spring Boot employee-service with PostgreSQL, Spring Data JPA/Hibernate, REST CRUD, service/repository/controller layers, DTOs, Bean Validation and structured exception handling.
- Learned dependency injection, entity mapping, persistence lifecycle, `Optional.orElseThrow`, custom exceptions, HTTP statuses, and Postman verification.
- Used `DB_PASSWORD` environment variable rather than committing credentials.

## Sprint 2 — Search, Pagination, and Testing (completed)

- Added first-name search, pagination, sorting, `PageResponse<T>`, query-parameter validation, SLF4J logging and dev/prod profiles.
- Practiced H2 repository tests, service tests and MockMvc controller tests; 14 automated tests passed at closure.
- Learned to separate unit tests from database-backed tests and to validate invalid pagination/sorting requests.

## Sprint 3 — Departments and Relationships (completed)

- Added Department CRUD, department DTOs and repositories, Employee–Department `@ManyToOne`/`@OneToMany` relationship, employee assignment and department filtering.
- Learned PK/FK, normalization, owning side, referential integrity, service-level deletion protection, HTTP 404 vs 409.
- Verified 21 tests and Maven build; Git feature integrated into `develop` and Sprint 3 documentation corrected in follow-up.

## Sprint 4 — Persistence Foundations (implementation verified 2026-10-16; Git closure pending)

### Flyway and schema versioning

- Introduced Flyway and `V1__initial_schema.sql`. Learned migration vs baseline, schema history, checksums, why applied migrations should not be edited, and why Hibernate `ddl-auto=validate` is safer than `update` for controlled environments.
- Existing `nexus` database: Flyway BASELINE V1; fresh `nexus_flyway_test`: SQL V1 successfully executed. Hypothetical `joining_date` V2 was a learning illustration, not a deployed migration.
- Key learning: Flyway manages schema evolution; Hibernate maps objects and persists data; PostgreSQL stores data and enforces constraints.

### Test isolation

- Configured H2 in-memory `application-test.properties`, disabled Flyway for H2, and applied `@ActiveProfiles("test")` to application-context test.
- Maven tests no longer depend on a local PostgreSQL password when the test profile is used. H2 does not replace PostgreSQL-specific migration testing.

### Transactions

- Added explicit `@Transactional` for write business operations and `@Transactional(readOnly=true)` for read operations.
- Wrote `DepartmentTransactionTest`: insert and flush department, throw unchecked exception, verify row absent after rollback.
- Key correction: repository methods can have their own transactions, but service methods are not automatically transactional. Returning a record does not make an operation read-only; write operations can also return DTOs.
- Learned ACID, transaction boundaries, proxies, self-invocation, REQUIRED propagation, flush vs commit, and pre-commit logging limitations.

### JPA fetching and API verification

- Changed Employee → Department mapping from default EAGER to LAZY; observed employee-list SQL drop from two SELECTs to one for tested request.
- Disabled Open Session in View for dev/prod/test; verified four GET endpoints with Postman (all 200).
- Department-filter query intentionally includes an existence check before employee lookup. Lazy fetching must still be tested for future response mappings requiring related fields.

### Database constraints and exception handling

- Preserved department deletion guard; PostgreSQL FK remains the final referential integrity safeguard during concurrency.
- Added generic `DataIntegrityViolationException` → HTTP 409 handler and direct unit test. Do not confuse this with a complete concurrency or HTTP integration test.
- Removed duplicate imports from handler; preserved existing response contracts.

### Verification and next steps

- `mvn -Dtest=GlobalExceptionHandlerTest test`: 1/1 passed.
- `mvn clean verify`: **23 tests, 0 failures, 0 errors, BUILD SUCCESS**; executable JAR built (2026-10-16).
- Save updated project context, journal, and `docs/sprint-notes/sprint-4.md` **before Git staging and commit**.
- Deferred: PostgreSQL-specific integration tests, race-condition coverage, DB-level case-insensitive department uniqueness, standardized API errors, and actual V2 schema evolution.

## Sprint 5 — Employee Lifecycle and Business Rules (implementation and database verification completed 2026-10-18; Git closure pending)

- Added optional `joiningDate` (`LocalDate`) and `employmentStatus` (`ACTIVE`, `RESIGNED`, `TERMINATED`). New employees default to ACTIVE; historical employees were backfilled to ACTIVE while retaining NULL joining dates.
- Added Flyway `V2__employee_lifecycle.sql` using a data-preserving sequence: add columns, backfill, then apply default, NOT NULL, and CHECK constraint. Tested V2 on `nexus_flyway_test` before migrating original `nexus`.
- Implemented PATCH `/api/employees/{id}/status`: only ACTIVE → RESIGNED or ACTIVE → TERMINATED; reject reverse, terminal-to-terminal and same-state transitions with HTTP 409. PUT cannot change lifecycle status.
- Disabled physical employee deletion: DELETE returns HTTP 405 via `EmployeeDeletionNotAllowedException` and `GlobalExceptionHandler`; no stack trace in response. Configured `server.error.include-stacktrace=never` in dev.
- Debugged `EmployeeRepositoryTest` failing with an H2 constraint/database-closed error while the dev profile was active. Added `@ActiveProfiles("test")`, preserving H2 `create-drop`/Flyway-disabled test isolation. Do not assume the exact H2 failure cause was proven.
- Practiced why controller tests and service tests cover different boundaries; `@NotNull` vs Jackson enum parsing; `never().save()` vs JPA dirty checking; `@Transactional` vs optimistic locking (`@Version`, deferred).
- Postman verified create/read/update, future joining date rejection (400), allowed transitions, rejected transitions (409), prohibited DELETE (405), and preservation of records.
- Verified backup of original `nexus` using `pg_dump` custom archive and a separate `pg_restore` into `nexus_backup_verify` (9 employees, 1 department, Flyway V1 baseline). Original `nexus` successfully migrated to V2; post-migration counts: 9 employees, all ACTIVE, all joining dates NULL, 1 department.
- Final `mvn clean verify`: **37 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**; executable JAR built.
- Sprint 5 documentation and Git integration are the remaining closure tasks. Keep backups out of Git.
