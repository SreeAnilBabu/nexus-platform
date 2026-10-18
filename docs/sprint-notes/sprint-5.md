# Sprint 5 — Employee Lifecycle: State Machines, Validation, Flyway V2 and API Safety

**Nexus Platform — Employee Service | Expanded learning edition | 18 October 2026**

> **Purpose:** Learn the engineering concepts behind the feature, not just memorize which endpoints were added. This chapter builds on Sprints 1–4. It uses verified Sprint 5 code snippets, SQL, logs and Postman outcomes from the supplied notes and conversation. Illustrative snippets are explicitly labeled and should be compared against the local source before copying. The original Sprint 5 record is preserved in an appendix. Git closure is still pending.

## Learning objectives

Explain business state transitions as a finite-state machine; distinguish entity, DTO and database constraints; use Java enums and `LocalDate` safely; understand Jackson deserialization versus Bean Validation; implement controlled PATCH semantics and prohibit deletion; design a data-preserving Flyway migration; test service, controller, repository and real PostgreSQL behavior; understand the limits of transactions without optimistic locking.

## 1. Why an employee lifecycle is more than CRUD

A CRUD API can create, read, update and delete rows. But real workforce records have a **lifecycle**: an employee can be active, resign or be terminated. Allowing arbitrary field updates could turn a resigned employee active again without business authorization or audit intent. Physically deleting employees can remove history referenced by other workflows. Sprint 5 therefore introduces **business invariants**, not merely two new database columns.

```text
HTTP request
    ↓
EmployeeController — route, JSON binding, @Valid
    ↓
EmployeeService — state transition rules and transaction
    ↓
EmployeeRepository — JPA persistence
    ↓
Hibernate → PostgreSQL (NOT NULL / CHECK / DEFAULT)
```

## 2. State machine fundamentals

### 2.1 States and transitions

A **state** describes current business condition. A **transition** is an allowed change between states. Sprint 5 has three states:

```java
public enum EmploymentStatus {
    ACTIVE,
    RESIGNED,
    TERMINATED
}
```

Allowed transitions:

```text
                  ┌───────────────┐
                  │    ACTIVE     │
                  └──────┬────────┘
                         │
                ┌────────┴─────────┐
                ▼                  ▼
           RESIGNED            TERMINATED
           (terminal)          (terminal)
```

**Terminal** means no outgoing transition is allowed under Sprint 5 requirements. `ACTIVE → ACTIVE` is also rejected because this API represents an actual state change, not an idempotent set operation.

### 2.2 Complete transition matrix

| Current | Requested | Allowed? | HTTP outcome |
|---|---|---|---|
| ACTIVE | RESIGNED | Yes | 200 |
| ACTIVE | TERMINATED | Yes | 200 |
| ACTIVE | ACTIVE | No | 409 |
| RESIGNED | ACTIVE | No | 409 |
| RESIGNED | RESIGNED | No | 409 |
| RESIGNED | TERMINATED | No | 409 |
| TERMINATED | ACTIVE | No | 409 |
| TERMINATED | RESIGNED | No | 409 |
| TERMINATED | TERMINATED | No | 409 |

The transition rules belong in the **service**, not only in controller request validation: other callers of the service must obey the same domain rules.

### 2.3 A transition guard

**Illustrative service logic reflecting the verified rules (not verbatim source):**

```java
@Transactional
public EmployeeResponse updateEmploymentStatus(Long id, EmploymentStatus target) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new EmployeeNotFoundException(id));
    EmploymentStatus current = employee.getEmploymentStatus();

    if (current != EmploymentStatus.ACTIVE || target == EmploymentStatus.ACTIVE) {
        throw new InvalidEmploymentStatusTransitionException(
            "Cannot change employment status from " + current + " to " + target);
    }

    employee.setEmploymentStatus(target);
    // The managed entity can be persisted by JPA dirty checking at flush/commit.
    return mapToResponse(employee);
}
```

The exact project method name, exception constructor and mapping helper must be checked in the local repository. The invariant is what matters: only two transitions are valid. `null` is rejected earlier by request validation when the PATCH DTO is validated.

### 2.4 Why HTTP 409?

`409 Conflict` says the request conflicts with the resource's current state. The JSON status value may be well-formed, yet the transition is prohibited. Contrast with `400 Bad Request` for missing/invalid input and `404 Not Found` for a nonexistent employee.

## 3. Java enum persistence and database representation

### 3.1 Enum advantages

Enums constrain Java values to a known set and improve type safety compared with unrestricted strings. Code can compare `EmploymentStatus.ACTIVE` without spelling errors or magic literals.

### 3.2 `@Enumerated(EnumType.STRING)` — project implementation

```java
@Enumerated(EnumType.STRING)
@Column(name = "employment_status", nullable = false, length = 20)
private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;
```

`STRING` stores `ACTIVE`, `RESIGNED`, `TERMINATED` in PostgreSQL. By contrast, `ORDINAL` stores integer positions; reordering enum constants could silently reinterpret old data. String persistence is easier to inspect and safer when constants are added, though renaming a constant still requires a deliberate migration/compatibility plan.

### 3.3 Three layers of protection

| Layer | Sprint 5 rule | Protects against |
|---|---|---|
| Java entity default | `= EmploymentStatus.ACTIVE` | New entities constructed by application without explicit status |
| Service transition guard | Only ACTIVE→terminal | Invalid business transitions through API/service |
| PostgreSQL DEFAULT + NOT NULL + CHECK | Valid non-null stored values | Inserts or updates outside application and missing status values |

**Important:** PostgreSQL CHECK allows any of the three values; it **does not** enforce allowed *transitions* from an old value to a new value. The state machine rule is in application logic. A DB trigger or more advanced concurrency control would be required for DB-enforced transition semantics.

## 4. Joining date and Java date types

### 4.1 `LocalDate` versus timestamp

A joining date is a calendar date, not an instant in time. `java.time.LocalDate` models year/month/day without timezone or clock time. PostgreSQL `DATE` is a natural fit.

```java
@Column(name = "joining_date")
private LocalDate joiningDate;
```

`LocalDateTime` would add a time component the business does not need. `Instant` is for a precise moment on the UTC timeline, such as an audit event timestamp.

### 4.2 Why nullable for old employees?

The existing nine employees had no recorded joining date. Inventing one would corrupt historical accuracy. Sprint 5 keeps the field nullable, so `NULL` explicitly means **unknown/not recorded**, not an invented default.

### 4.3 Request validation — project implementation

```java
@PastOrPresent(message = "Joining date cannot be in the future")
private LocalDate joiningDate;
```

Used in create and update request DTOs. `@PastOrPresent` permits `null`; to require a value, combine it with `@NotNull`. The sprint intentionally did **not** require a joining date. The rule is evaluated against the validation clock at request time.

### 4.4 JSON mapping

**Illustrative request fragment:**

```json
{
  "firstName": "Alex",
  "lastName": "Thomas",
  "email": "alex.thomas@example.test",
  "joiningDate": "2026-10-01"
}
```

Spring/Jackson maps the ISO date string to `LocalDate`. A future date such as `2099-12-31` passed as a syntactically valid date but failed `@PastOrPresent`, producing 400 in the observed tests.

## 5. DTOs as the public API boundary

### 5.1 Entity versus DTO

The `Employee` entity is a persistence model, containing database mappings and relationships. Request DTOs define which client fields are accepted. Response DTOs define what the client sees. Returning entities directly can expose internal fields, create lazy-loading problems, and allow accidental coupling to schema changes.

### 5.2 Sprint 5 DTO changes

- `CreateEmployeeRequest` and `UpdateEmployeeRequest`: optional `LocalDate joiningDate` with `@PastOrPresent`.
- `EmployeeResponse`: adds `joiningDate` and `employmentStatus`.
- `UpdateEmploymentStatusRequest`: contains `@NotNull EmploymentStatus status`.

**Illustrative DTO shape:**

```java
public class UpdateEmploymentStatusRequest {
    @NotNull(message = "Employment status is required")
    private EmploymentStatus status;
    // getters/setters required by the chosen DTO style
}
```

The annotation's exact message is illustrative; check the local source for the literal text. The actual confirmed requirement is that status is non-null.

### 5.3 Why PUT must not change employment status

The ordinary employee update is for editable details (e.g., name, joining date, department). If it accepted a status field, a caller could bypass the dedicated transition guard. Sprint 5's PUT request DTO does **not** grant status editing. In Postman, sending `"employmentStatus":"ACTIVE"` in a PUT for a RESIGNED employee changed the permitted last name but left persisted status RESIGNED.

This demonstrates **field allowlisting through DTO design**. A request containing an unexpected property being ignored is not the same as explicitly authorizing it; stricter unknown-property rejection could be a separate API policy decision.

## 6. PATCH versus PUT versus DELETE

### 6.1 Resource update semantics

`PUT /api/employees/{id}` updates the permitted employee representation under the project's current update rules. `PATCH /api/employees/{id}/status` expresses a targeted lifecycle command. The PATCH request carries only the desired status, reducing ambiguity and centralizing transition logic.

```http
PATCH /api/employees/7/status
Content-Type: application/json

{"status":"RESIGNED"}
```

Expected on ACTIVE employee: HTTP 200 with updated response. Repeating the same PATCH is rejected with 409 **by this domain rule**. PATCH is not inherently idempotent; individual API contracts determine repeated-call behavior.

### 6.2 Why employee deletion is prohibited

Employee records can have historical meaning. Sprint 5 rejects `DELETE /api/employees/{id}` with **405 Method Not Allowed** and retains the record. The endpoint deliberately throws a domain-specific exception rather than calling repository delete.

**Project controller implementation:**

```java
@DeleteMapping("/{id}")
public void deleteEmployee(@PathVariable Long id) {
    throw new EmployeeDeletionNotAllowedException(
        "Physical deletion of employee records is not permitted"
    );
}
```

An explicit handler returns a predictable error. A DELETE route existing but rejecting all calls is a project design choice; HTTP 405 means the method is not allowed for that target resource. This does **not** create soft deletion; records are retained unchanged.

### 6.3 Error response — project implementation

```java
@ExceptionHandler(EmployeeDeletionNotAllowedException.class)
public ResponseEntity<Map<String, String>> handleEmployeeDeletionNotAllowed(
        EmployeeDeletionNotAllowedException ex) {
    return ResponseEntity
        .status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(Map.of(
            "error", "Method Not Allowed",
            "message", ex.getMessage()
        ));
}
```

Postman observed:

```json
{
  "error": "Method Not Allowed",
  "message": "Physical deletion of employee records is not permitted"
}
```

JSON key ordering is not a contract. The controller test also checked `verifyNoInteractions(employeeService)` to confirm the DELETE route did not invoke service logic.

## 7. Jackson parsing versus Bean Validation

### 7.1 Request processing pipeline

```text
HTTP JSON
   ↓
Jackson deserializes JSON into DTO
   ↓
Bean Validation (@Valid / @NotNull / @PastOrPresent)
   ↓
Controller method
   ↓
Service business rules
```

An invalid enum string may fail **during Jackson conversion**, before Bean Validation runs. A `null` enum may deserialize successfully but fail `@NotNull`. A valid enum such as `RESIGNED` can still fail the **service** transition guard if the current state is TERMINATED.

| Payload | Failure stage | Intended response category |
|---|---|---|
| `{"status":null}` | Bean Validation | 400 |
| `{"status":"UNKNOWN"}` | Jackson enum conversion | 400 |
| `{"status":"ACTIVE"}` when current=RESIGNED | Business rule | 409 |
| `{"status":"RESIGNED"}` when current=ACTIVE | Valid request | 200 |

The notes confirm the distinction was taught; they do not independently prove every specific invalid-enum HTTP body was exercised in Postman. Do not claim that all handlers have identical JSON shapes.

### 7.2 Why separate error categories matter

A client can fix a malformed payload; it cannot fix a business-state conflict merely by changing JSON syntax. Meaningful status codes make retries and UI messages more predictable.

## 8. Flyway V2 — real SQL, line by line

### 8.1 Migration file

**Project implementation:** `src/main/resources/db/migration/V2__employee_lifecycle.sql`

```sql
-- Sprint 5: Employee lifecycle and joining date
-- Preserve all existing employee records.
ALTER TABLE employees ADD COLUMN joining_date DATE;
ALTER TABLE employees ADD COLUMN employment_status VARCHAR(20);
UPDATE employees SET employment_status = 'ACTIVE' WHERE employment_status IS NULL;
ALTER TABLE employees ALTER COLUMN employment_status SET DEFAULT 'ACTIVE';
ALTER TABLE employees ALTER COLUMN employment_status SET NOT NULL;
ALTER TABLE employees ADD CONSTRAINT chk_employees_employment_status
    CHECK (employment_status IN ('ACTIVE', 'RESIGNED', 'TERMINATED'));
```

### 8.2 Step 1: add nullable joining date

```sql
ALTER TABLE employees ADD COLUMN joining_date DATE;
```

Existing employees gain a `NULL` joining date. The SQL does not infer a historical date. This is intentional backward-compatible schema evolution.

### 8.3 Step 2: add nullable status

```sql
ALTER TABLE employees ADD COLUMN employment_status VARCHAR(20);
```

Initially allowing nulls lets existing rows survive while we populate a valid value. Adding a NOT NULL status immediately to a populated table without default/backfill would fail or require another migration strategy.

### 8.4 Step 3: backfill

```sql
UPDATE employees SET employment_status = 'ACTIVE'
WHERE employment_status IS NULL;
```

**Backfill** means populating data in existing rows so they satisfy the new model. The `WHERE ... IS NULL` clause only targets rows missing status. On the original `nexus` database, all nine existing employees became ACTIVE.

### 8.5 Step 4: default for future inserts

```sql
ALTER TABLE employees ALTER COLUMN employment_status SET DEFAULT 'ACTIVE';
```

This default applies when an INSERT omits the column; it does **not** mean an explicit `NULL` is accepted. The Java entity also initializes status ACTIVE. Application and database defaults protect different entry paths.

### 8.6 Step 5: enforce non-null

```sql
ALTER TABLE employees ALTER COLUMN employment_status SET NOT NULL;
```

Now every stored employee must have a status. The backfill must happen before this constraint is enforced for historical records.

### 8.7 Step 6: enforce allowed values

```sql
ALTER TABLE employees ADD CONSTRAINT chk_employees_employment_status
    CHECK (employment_status IN ('ACTIVE', 'RESIGNED', 'TERMINATED'));
```

PostgreSQL rejects strings outside the enum vocabulary. The check does not prevent RESIGNED→ACTIVE if someone manually issues valid SQL values; transition rules remain a separate concern.

### 8.8 Why order matters

```text
Add columns (allow old rows)
     ↓
Backfill old rows
     ↓
Set default for future inserts
     ↓
Enforce NOT NULL
     ↓
Enforce allowed-value CHECK
```

This is the **expand → migrate data → constrain** pattern. In larger distributed deployments, schema and application compatibility may require additional phased releases; Sprint 5 was a local development migration.

### 8.9 Flyway baseline V1 versus V2 SQL

The original `nexus` database already had Flyway **baseline version 1** from Sprint 4. Its V1 SQL did not execute there. V2 was a real SQL migration executed on that database in Sprint 5. Scratch `nexus_flyway_test` had executed both V1 and V2 as SQL migrations.

## 9. Database safety and verification

### 9.1 Scratch-first testing

During Sprint 5, the application was launched against `nexus_flyway_test` using a run-scoped datasource override:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://localhost:5432/nexus_flyway_test"
```

The default `application.properties` still pointed at `nexus`; a command-line override changes that **one process**, not the project file. This matters because Flyway applies pending migrations on startup.

### 9.2 Original database snapshot

Before V2, the original `nexus` had 9 employees, 1 department, V1 baseline, and neither new column. A custom-format backup was made, listed using `pg_restore --list`, then successfully restored to `nexus_backup_verify`. The restored database had 9 employees, 1 department, and the V1 baseline. The extra restore exercise established a usable local recovery snapshot; it was not required as an everyday substitute for pgAdmin's Backup UI.

**Backup path in the original notes:** `database/backups/nexus_before_sprint5_20261018_145122.dump`. Keep `/database/backups/` excluded from Git; database dumps may contain private records.

### 9.3 Real migration logs

On 18 October 2026 the application started with active `dev` profile and datasource `jdbc:postgresql://localhost:5432/nexus`. Flyway reported current schema version 1, migrated to `2 - employee lifecycle`, and successfully applied one migration. Hibernate initialized its EntityManagerFactory and Tomcat started on port 8080.

### 9.4 Post-migration SQL and actual results

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT COUNT(*) AS total_employees,
       COUNT(*) FILTER (WHERE employment_status = 'ACTIVE') AS active_employees,
       COUNT(*) FILTER (WHERE joining_date IS NULL) AS null_joining_dates
FROM employees;

SELECT COUNT(*) AS total_departments FROM departments;
```

| Verification | Observed result |
|---|---|
| Flyway history | V1 baseline success; V2 employee lifecycle success |
| Total employees | 9 |
| ACTIVE employees | 9 |
| NULL joining dates | 9 |
| Departments | 1 |

These counts confirm preservation at the level checked. They are not a row-by-row cryptographic comparison, but they match the verified pre-migration snapshot and the expected backfill.

## 10. JPA transactions and why concurrency is a future concern

### 10.1 Dirty checking after a transition

When a service loads a managed Employee inside a transaction and calls `employee.setEmploymentStatus(...)`, Hibernate can persist that change at flush/commit even without a separate repository `save` call. Therefore a Mockito assertion that `save()` was never invoked is **not enough** to prove the database is unchanged.

### 10.2 Why `@Transactional` is not `@Version`

`@Transactional` controls a unit of work and commit/rollback. It does not automatically detect that two requests read the same old status and then both write different terminal states. `@Version` enables optimistic-lock conflict detection when properly mapped and backed by a version column.

**Illustrative race (not observed as a Sprint 5 failure):**

```text
Request A reads ACTIVE          Request B reads ACTIVE
Request A chooses RESIGNED      Request B chooses TERMINATED
Request A commits               Request B commits later
```

Without explicit concurrency protection, a last-write-wins outcome may violate the intended one-way lifecycle policy under concurrent requests. Optimistic locking with a version column, or a conditional atomic UPDATE, is a future hardening option. **Sprint 5 did not add `@Version` or concurrency tests.**

### 10.3 `verify(..., never()).save(...)` properly interpreted

In a mocked service test, `verify(repository, never()).save(...)` verifies the interaction with that mock. It does not simulate a real persistence context. Combine service unit tests with integration/DB observations when checking state preservation.

## 11. Testing strategy — what each layer proves

| Layer | Typical tooling | Primary concern | Limitation |
|---|---|---|---|
| Service unit test | JUnit + Mockito | Transition rules, repository calls, exceptions | Mocks do not prove real SQL |
| Controller/web test | MockMvc and Spring MVC test support | Routing, JSON, validation, HTTP codes | Mocked service may hide persistence defects |
| Repository test | `@DataJpaTest` + H2 | JPA mappings and derived queries | H2 differs from PostgreSQL |
| Manual API test | Postman + running app | End-to-end behavior on chosen datasource | Limited scenario coverage |
| PostgreSQL SQL verification | `psql` | Stored state and migration history | Does not automatically prove every API path |

### 11.1 Service tests

Test allowed transitions, all disallowed transitions, nonexistent employee and no unintended mutation. **Illustrative Mockito test shape:**

```java
when(employeeRepository.findById(7L)).thenReturn(Optional.of(resignedEmployee));
assertThrows(InvalidEmploymentStatusTransitionException.class,
    () -> employeeService.updateEmploymentStatus(7L, EmploymentStatus.ACTIVE));
verify(employeeRepository, never()).save(any(Employee.class));
```

The exact service signature may differ; this example teaches interaction assertions. A stronger service test can also assert that the mocked entity's status field remains RESIGNED.

### 11.2 Controller tests

Controller tests focus on HTTP behavior rather than the full database. Sprint 5 added a DELETE test asserting HTTP 405, the structured error fields and `verifyNoInteractions(employeeService)`. The final suite increased from 36 to **37 tests** after this test was added.

### 11.3 Repository test profile failure and fix

An initial run had H2-related failures while `EmployeeRepositoryTest` used the wrong active profile. The effective fix was adding `@ActiveProfiles("test")`, aligning the repository test with the H2 test properties (`create-drop`, Flyway disabled). Focused repository tests passed 4/4, followed by full suite success. The notes mention an H2 CHECK constraint and database-closed error; they do **not** establish a specific underlying H2 engine bug, so avoid claiming one.

### 11.4 Final Maven result

```text
mvn clean verify
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The executable Spring Boot JAR was generated. This result predates documentation-only edits; it does not need to be described as a new build performed while writing these notes.

## 12. Postman exercise book — verified Sprint 5 scenarios

The following results were observed against **scratch PostgreSQL** during the sprint, not newly executed while preparing this document.

| # | Request/scenario | Observed status | Learning objective |
|---|---|---|---|
| 1 | POST valid employee with joining date | 201 | New employee defaults ACTIVE |
| 2 | GET newly created employee | 200 | Response exposes new fields |
| 3 | PATCH ACTIVE→RESIGNED | 200 | Allowed transition persists |
| 4 | PATCH RESIGNED→ACTIVE | 409 | No reactivation |
| 5 | PATCH RESIGNED→RESIGNED | 409 | Same-state rejected |
| 6 | DELETE employee | 405 | No physical deletion |
| 7 | GET after rejected DELETE | 200 | Record preserved |
| 8 | PUT RESIGNED employee including status ACTIVE | 200 | Allowed details update; status unchanged |
| 9 | POST joining date `2099-12-31` | 400 | Create validation |
| 10 | PUT joining date `2099-12-31` | 400 | Update validation |
| 11 | POST second employee | 201 | New ACTIVE state |
| 12 | PATCH ACTIVE→TERMINATED | 200 | Second allowed transition |
| 13 | PATCH TERMINATED→RESIGNED | 409 | Terminal state immutable |

**Reproduce in Postman:** choose method and `http://localhost:8080/api/employees` or `/api/employees/{id}/status`, select Body → raw → JSON, send payload and compare HTTP status and response. Avoid reusing IDs from historical examples without first creating test records in a scratch database.

### 12.1 Why GET after DELETE matters

A 405 alone proves an error response; it does not prove the employee still exists. The follow-up GET returned 200 and showed the same RESIGNED status, validating the no-delete behavior at the API level.

### 12.2 Why PUT with status matters

The PUT test attempted to sneak `employmentStatus: ACTIVE` into a normal details update. The name changed, but DB status remained RESIGNED. This is a test of **mass-assignment protection** via the DTO/service boundary.

### 12.3 Why database verification matters

An HTTP 200 or 409 is not the whole story. Reading back the record and querying the DB distinguishes a correct response from accidental persistence or rollback errors.

## 13. Error handling and safe responses

### 13.1 Expected failures are not application crashes

`InvalidEmploymentStatusTransitionException` maps to 409; `EmployeeDeletionNotAllowedException` maps to 405. The application can log that a custom exception was resolved, even at WARN level, while still returning the intended response. Such a warning does not automatically mean an unhandled server failure.

### 13.2 Avoid exposing stack traces

The initial DELETE implementation used a default error path that produced a lengthy body including stack trace information. Sprint 5 replaced that with a dedicated exception/handler and added:

```properties
server.error.include-stacktrace=never
```

The resulting Postman response was concise and structured. Do not confuse this configuration with removing server-side logs; logs remain useful for diagnosis.

### 13.3 HTTP status quick reference

| Code | Meaning in this project |
|---|---|
| 200 | Successful GET/PUT/PATCH |
| 201 | Employee created |
| 400 | Invalid request data or date |
| 404 | Employee not found |
| 405 | Physical employee deletion forbidden |
| 409 | Invalid lifecycle transition / business conflict |

## 14. Debugging lessons and corrective actions

**Issue A — repository tests failed:** observed wrong profile and H2 constraint/closed-database symptoms. Action: add `@ActiveProfiles("test")` to `EmployeeRepositoryTest`; rerun focused and full suites. Do not diagnose a specific engine defect without evidence.

**Issue B — DELETE response exposed too much detail:** replaced the generic error path with custom `EmployeeDeletionNotAllowedException` and global handler; set `server.error.include-stacktrace=never`; verified clean 405 JSON and employee retention.

**Issue C — risk of migrating wrong DB:** scratch-first migration used run-scoped JDBC URL override; the original DB was backed up and restored separately; only then was the default `nexus` datasource intentionally started to apply V2.

**Issue D — tests may overclaim:** Mockito no-save assertions are useful but cannot replace DB-state checks due to JPA dirty checking.

**Issue E — migration history ambiguity:** existing `nexus` V1 is a BASELINE, whereas V2 is executed SQL. The SQL migration history and preserved record counts are both needed to explain what happened.

## 15. Interview-ready Q&A

**Q1. What is a state machine?** A finite set of states plus explicit rules governing allowed transitions; Sprint 5 permits only ACTIVE→RESIGNED and ACTIVE→TERMINATED.

**Q2. Why not expose employmentStatus in PUT?** It would allow callers to bypass the lifecycle transition guard. A dedicated PATCH command makes the business rule explicit.

**Q3. Why use `EnumType.STRING`?** Stored values are readable and stable against enum reordering; ORDINAL persistence is fragile when enum order changes.

**Q4. Does `@PastOrPresent` require a date?** No, null is valid unless combined with `@NotNull`.

**Q5. Why add nullable columns before NOT NULL?** Existing records need a compatible intermediate state and backfill before the stronger constraint is enforced.

**Q6. What does the CHECK constraint guarantee?** Status belongs to the allowed vocabulary; it does not enforce transitions between those values.

**Q7. Why does an unknown enum fail before `@NotNull`?** Jackson cannot construct the DTO value from an unrecognized string; Bean Validation runs after successful deserialization.

**Q8. What is the difference between 400 and 409?** 400 indicates invalid input; 409 indicates a well-formed request that conflicts with current resource state.

**Q9. Does `@Transactional` stop concurrent lost updates?** Not by itself. Optimistic locking (`@Version`) or conditional updates can protect against stale concurrent writes.

**Q10. Why test the migration on both scratch and existing DBs?** Scratch validates reproducibility; existing DB validates preservation/backfill against historical rows.

**Q11. Why is `verify(repository, never()).save()` insufficient?** Hibernate can persist managed-entity changes via dirty checking without an explicit save call.

**Q12. Why is a successful 405 insufficient to prove no deletion?** Read back the record or inspect the database to verify retention.

## 16. Self-check exercises

1. Draw the nine-cell state transition matrix without looking above.
2. Explain why `RESIGNED→RESIGNED` is rejected despite being the same value.
3. Explain the distinction between JSON parsing, Bean Validation, and service validation.
4. Walk through every statement of V2 and predict what happens if NOT NULL precedes backfill.
5. Describe how a direct SQL UPDATE could bypass service-level transition rules.
6. Design a future concurrency test with two requests attempting different terminal states.
7. Explain the difference between PostgreSQL CHECK, Java enum, and service guard.
8. Reconstruct the proof that the original nine employees survived V2.

## 17. Verified outcomes and what remains

**Completed:** V2 on scratch and original PostgreSQL, nine employees retained, nine ACTIVE statuses, nine NULL joining dates, one department retained; Postman business scenarios passed; 37 automated tests passed; clean custom DELETE response.

**Not yet claimed:** local documentation integrated, repository diff reviewed, Sprint 5 Git commit/push/merge performed, optimistic locking implemented, or production deployment audited.

**Deferred:** Security, Kafka, microservices, frontend, `@Version`, PostgreSQL-backed concurrency tests, comprehensive audit history and standardized error schema.

## 18. Key takeaways

- Model business status as a guarded state machine, not a free-form editable field.
- Separate entity persistence from request and response contracts.
- Use LocalDate for date-only values and validate at the API boundary.
- Backfill existing data before enforcing NOT NULL.
- Database CHECK protects vocabulary; service logic protects transitions.
- Distinguish Jackson parsing failures from Bean Validation and business conflicts.
- Use targeted PATCH for lifecycle commands and reject physical DELETE.
- Test HTTP behavior, business logic, database state and migration history at their proper layers.
- Recognize concurrency limitations instead of assuming transactions solve everything.

---

## Appendix A — Original Sprint 5 record (preserved for audit)

The following is the supplied historical Sprint 5 note, retained verbatim so no reported implementation detail or verification result is lost.

# Sprint 5 — Employee Lifecycle and Business Rules

**Project:** Nexus Platform / `backend/employee-service`
**Status:** Implementation, scratch and original PostgreSQL migration, API verification, and automated tests complete; documentation integration and Git closure pending.
**Verification date:** 2026-10-18.
**Branch:** `feature/sprint-5-employee-lifecycle` (verify locally before committing).

## Goals and scope

Introduce a safe, auditable employee lifecycle without physical deletion: optional joining date, employment status, controlled state transitions, request validation, Flyway V2 schema evolution, and tests. Kafka, security, microservices, and frontend work are outside this sprint.

## Design and business rules

- `EmploymentStatus` enum: `ACTIVE`, `RESIGNED`, `TERMINATED`.
- New employees default to `ACTIVE`; existing employees are backfilled to `ACTIVE` by V2.
- `joiningDate` is nullable for historical records; new/updated dates must be in the past or present (`@PastOrPresent`).
- Allowed lifecycle changes: `ACTIVE -> RESIGNED` or `ACTIVE -> TERMINATED` only.
- Reject reversal, changing between final statuses, and repeating the current status with HTTP 409.
- `PUT /api/employees/{id}` updates permitted employee details but must not change employment status, even if JSON includes `employmentStatus`.
- `PATCH /api/employees/{id}/status` changes lifecycle status through the controlled business method.
- `DELETE /api/employees/{id}` is explicitly rejected with HTTP 405; records remain accessible.

## Implementation map

- `src/main/java/com/nexuslabs/employee_service/.../EmploymentStatus.java` — enum (confirm exact package from source).
- `Employee.java` — `LocalDate joiningDate` and `@Enumerated(EnumType.STRING) EmploymentStatus employmentStatus`, default `ACTIVE`.
- `CreateEmployeeRequest`, `UpdateEmployeeRequest` — nullable `joiningDate` with `@PastOrPresent`.
- `EmployeeResponse` — includes `joiningDate` and `employmentStatus`.
- `UpdateEmploymentStatusRequest` — `@NotNull` status.
- `EmployeeService` — create/update mappings and guarded lifecycle transitions; no physical employee delete.
- `EmployeeController` — PATCH status and DELETE prohibition.
- `InvalidEmploymentStatusTransitionException` — rejected transition, mapped to HTTP 409.
- `EmployeeDeletionNotAllowedException` — rejected deletion, mapped to HTTP 405.
- `GlobalExceptionHandler` — consistent JSON `{ "error": "...", "message": "..." }` for lifecycle and deletion errors.
- `application-dev.properties` — `server.error.include-stacktrace=never` to avoid leaking stack traces in default HTTP error responses.

## Flyway V2 — data-preserving schema migration

**File:** `src/main/resources/db/migration/V2__employee_lifecycle.sql`

```sql
ALTER TABLE employees ADD COLUMN joining_date DATE;
ALTER TABLE employees ADD COLUMN employment_status VARCHAR(20);
UPDATE employees SET employment_status = 'ACTIVE' WHERE employment_status IS NULL;
ALTER TABLE employees ALTER COLUMN employment_status SET DEFAULT 'ACTIVE';
ALTER TABLE employees ALTER COLUMN employment_status SET NOT NULL;
ALTER TABLE employees ADD CONSTRAINT chk_employees_employment_status
    CHECK (employment_status IN ('ACTIVE', 'RESIGNED', 'TERMINATED'));
```

**Why this order matters:** Add nullable columns first to preserve historical rows, backfill the new required status, then enforce default/NOT NULL/check constraint. A DB default covers inserts that omit the column; Java initializes new entities to ACTIVE too. The SQL check guards against invalid values even outside the application.

**Migration safety:** Never rewrite an applied V1/V2 migration. Existing `nexus` was previously baselined at V1; `nexus_flyway_test` executed V1 as SQL on an empty DB and later V2. Hibernate dev configuration uses `ddl-auto=validate`, not automatic schema updates. Flyway validation and history track migration state.

## Database verification — 2026-10-18

- Scratch database `nexus_flyway_test`: V1/V2 validated, schema at version 2; Hibernate initialized successfully; invalid/NULL statuses rejected during scratch migration testing; old employee preserved with ACTIVE and NULL joining date.
- Default datasource in `application.properties`: `jdbc:postgresql://localhost:5432/nexus`; profile `dev`. Explicit `spring-boot.run.arguments` URL override was used to test scratch DB without changing defaults.
- Original `nexus` before migration: 9 employees, 1 department, V1 baseline, no `joining_date` or `employment_status` columns.
- Backup: `database/backups/nexus_before_sprint5_20261018_145122.dump` (6,460 bytes, PostgreSQL custom format); `pg_restore --list` succeeded. Restored to separate `nexus_backup_verify`: 9 employees, 1 department, V1 baseline. **Do not commit backups; keep `/database/backups/` ignored by Git.**
- Original `nexus` migration: Flyway reported successful application of `2 - employee lifecycle`; Hibernate entity manager initialized; Tomcat started on port 8080.
- Post-migration `nexus`: V1 baseline and V2 both successful; 9 total employees, 9 ACTIVE, 9 NULL joining dates, 1 department. No historical rows lost in observed checks.
- Backup verification proves a restore to a separate DB worked; it does not constitute production recovery planning.

## Postman verification — scratch PostgreSQL

| Test | Observed |
| --- | --- |
| POST employee with valid joining date | 201; `employmentStatus=ACTIVE` |
| GET employee | 200; new fields included |
| PATCH ACTIVE -> RESIGNED | 200; persisted |
| PATCH RESIGNED -> ACTIVE | 409 |
| PATCH RESIGNED -> RESIGNED | 409 |
| DELETE employee | 405; custom JSON error, no stack trace |
| GET after rejected DELETE | 200; employee retained, RESIGNED |
| PUT with `employmentStatus=ACTIVE` on RESIGNED employee | 200; permitted last name changed, status remained RESIGNED in DB |
| POST future joining date | 400; `joiningDate: Joining date cannot be in the future` |
| PUT future joining date | 400 |
| POST new employee | 201; ACTIVE |
| PATCH ACTIVE -> TERMINATED | 200; persisted |
| PATCH TERMINATED -> RESIGNED | 409; persisted status remains TERMINATED |

Representative error responses:

```json
{"error":"Conflict","message":"Cannot change employment status from TERMINATED to RESIGNED"}
```

```json
{"error":"Method Not Allowed","message":"Physical deletion of employee records is not permitted"}
```

A server-side WARN about a *resolved* custom exception is expected and is not an HTTP failure. Error JSON key order is not guaranteed.

## Automated testing and debugging

- Final reported `mvn clean verify`: **37 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Executable `target/employee-service-0.0.1-SNAPSHOT.jar` produced.
- Service tests cover transition rules and rejected persistence calls; controller tests cover request/response behavior, including DELETE 405; repository tests verify persistence semantics; validation tests cover date constraints.
- `EmployeeRepositoryTest` initially failed on H2 with `CHK_EMPLOYEES_EMPLOYMENT_STATUS` and nested database-closed error while running with the `dev` profile. Added `@ActiveProfiles("test")` to the repository test, aligning it with the existing test-only H2 config (`create-drop`, Flyway disabled). Focused repository tests 4/4 passed; full suite subsequently passed. **Do not assert a specific H2 engine bug as the root cause; test-profile isolation was the confirmed effective fix.**
- H2 tests do not verify PostgreSQL-specific Flyway SQL; PostgreSQL migrations were separately exercised on scratch and original development DBs.
- A mocked `verify(repository, never()).save(...)` assertion proves no explicit save call; it does not by itself prove DB state unchanged because JPA dirty checking may persist managed entity changes. Postman GET/DB checks help verify persistence outcomes.

## Key concepts learned

1. **Controller vs service tests:** Controller tests check HTTP mapping, JSON conversion, validation and status codes; service tests check business rules and repository interactions.
2. **`@NotNull` vs Jackson enum parsing:** `null` may deserialize and then fail Bean Validation; an unknown enum string fails Jackson deserialization first. Both should be mapped to useful 400 responses.
3. **Lifecycle transition guard:** Treat terminal statuses as immutable and reject same-state transitions, not just reversals.
4. **`@Transactional` vs `@Version`:** Transactions define commit/rollback boundaries; optimistic locking detects stale concurrent writes. `@Version` is not implemented in Sprint 5; it would require a future migration and tests.
5. **Database integrity:** Enforce important invariants in both application logic and PostgreSQL constraints; preserve old records through backfill before `NOT NULL`.
6. **Safe error responses:** Custom exceptions and `@RestControllerAdvice` keep expected failures structured; `server.error.include-stacktrace=never` avoids exposing traces from default error handling.
7. **Configuration precedence:** `spring.datasource.url` defaults to `nexus`; a command-line override for `nexus_flyway_test` lasts only for that application run.
8. **Backups:** A readable archive list is weaker than a successful restore; the latter was verified before migrating the existing DB.

## Issues and resolutions

- **Repository tests used wrong profile:** Explicit `@ActiveProfiles("test")` fixed test isolation and the observed H2 test failures.
- **DELETE returned a long default error body with stack trace:** Replaced `ResponseStatusException` with `EmployeeDeletionNotAllowedException` handled in `GlobalExceptionHandler`, retained HTTP 405, and set `server.error.include-stacktrace=never`.
- **Risk of accidentally migrating default DB:** Used explicit scratch URL override during tests, backed up and restored the original database before intentionally starting against `nexus`.

## Known limitations / future work

- No concurrency/optimistic-locking tests or `@Version` field yet.
- Not all error handlers use the same JSON shape; standardization can be a separate improvement.
- H2 is not a substitute for PostgreSQL-specific integration tests.
- No physical deletion endpoint by design; consider future retention/audit requirements rather than changing this rule casually.
- Security, Kafka, microservices, frontend, and deployment hardening are deferred.

## Closure checklist

- [x] Feature implementation and custom DELETE error handler
- [x] Postman checks against scratch DB
- [x] `mvn clean verify` — 37 passing tests
- [x] Backup and restore verification
- [x] Flyway V2 on original `nexus` and preservation checks
- [ ] Place/update `docs/sprint-notes/sprint-5.md`, `docs/engineering-journal.md`, and `docs/project-context.md` in local workspace
- [ ] Confirm `.gitignore` excludes `/database/backups/` and inspect working tree
- [ ] Confirm documentation reflects actual source and Git state; remove or archive outdated `docs/sprint-5-handoff.md` only after checking references
- [ ] Git stage/commit/push/merge (only after documentation is complete)

**Source note:** These notes reflect user-provided code, terminal logs, SQL outputs, and Postman responses, not an independent scan of the current local repository.
