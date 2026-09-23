# Sprint 2 --- Employee Search, Pagination, API Hardening & Testing

## Sprint Goal

Extend the Nexus Platform `employee-service` beyond basic CRUD by adding
production-oriented employee search, database-level pagination and
sorting, request-parameter validation, a stable paginated API response
contract, application logging, environment-specific Spring
configuration, and automated repository/controller tests.

Sprint 2 builds directly on the CRUD, DTO, validation,
exception-handling, JPA/Hibernate, PostgreSQL, and service-unit-testing
foundation completed in Sprint 1.

------------------------------------------------------------------------

## Final Status

**Sprint 2 implementation and testing are complete.**

The Employee Service can now:

-   Search employees by partial first name, case-insensitively.
-   Paginate employee list and search results at the database level.
-   Sort employee results using approved entity properties.
-   Combine search + pagination + sorting.
-   Validate pagination, sorting, direction, and blank search
    parameters.
-   Return controlled `400 Bad Request` responses for invalid API
    parameters.
-   Return a stable custom pagination DTO instead of exposing Spring's
    `PageImpl` JSON structure.
-   Produce structured application logs with appropriate logging levels.
-   Use Spring Profiles for development and production-specific
    configuration.
-   Test repository behavior using Spring Data JPA + Hibernate + an H2
    in-memory database.
-   Test controller/web behavior using Spring MVC `MockMvc` with a
    mocked service.
-   Run the complete automated suite successfully.

Final verified test result:

``` text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

------------------------------------------------------------------------

## 1. Sprint 2 Implementation Flow

The sprint followed this sequence:

``` text
Derived Query Methods
        ↓
Employee Search API
        ↓
Pagination
        ↓
Sorting
        ↓
Combined Search + Pagination + Sorting
        ↓
API Parameter Validation
        ↓
Stable Custom Pagination Response DTO
        ↓
Application Logging
        ↓
Spring Profiles
        ↓
Repository Tests
        ↓
Controller Tests
        ↓
Full Automated Test Verification
```

The intention was to strengthen the existing Employee Service before
introducing larger topics such as Spring Security, Kafka, or
microservices.

------------------------------------------------------------------------

# 2. Employee Search with Spring Data Derived Queries

## Repository Method

Employee search was implemented through a Spring Data JPA derived query:

``` java
Page<Employee> findByFirstNameContainingIgnoreCase(
        String firstName,
        Pageable pageable
);
```

Earlier in the sprint, the method was first introduced without
pagination:

``` java
List<Employee> findByFirstNameContainingIgnoreCase(String firstName);
```

It was later evolved to return `Page<Employee>` and accept `Pageable` so
search could support pagination and sorting.

## Derived Query Meaning

Spring Data interprets the method name:

``` text
findBy
    → query operation

FirstName
    → Java entity property

Containing
    → substring / LIKE-style search

IgnoreCase
    → case-insensitive comparison
```

Important: Spring Data property names refer to **Java entity
properties**, such as `firstName`, not database column names such as
`first_name`.

Observed Hibernate SQL for the search was conceptually:

``` sql
select ...
from employees
where upper(first_name) like upper(?)
```

The exact generated SQL is Hibernate/database dependent.

## Search Endpoint

``` text
GET /api/employees/search?name=son
```

The controller uses `@RequestParam` because `name` is a filtering/search
parameter rather than the identity of a specific resource.

### `@PathVariable` vs `@RequestParam`

Use `@PathVariable` for resource identity:

``` text
GET /api/employees/7
```

Use `@RequestParam` for filtering, searching, pagination, and sorting:

``` text
GET /api/employees/search?name=son
GET /api/employees?page=0&size=5
```

## Important Search Behavior

A missing required `name` parameter is rejected by Spring MVC with
`400 Bad Request`.

A blank value is different:

``` text
/api/employees/search?name=
```

Without custom validation, `Containing("")` behaves like a broad
`LIKE '%%'` search and can match all non-null names. Sprint 2 added
explicit blank-search validation to prevent this.

------------------------------------------------------------------------

# 3. Pagination

Pagination was introduced to prevent endpoints from loading every
employee when the dataset grows.

## Core Types

### `Pageable`

`Pageable` represents pagination and sorting instructions.

It can describe:

-   page number
-   page size
-   sorting

### `PageRequest`

`PageRequest` is a concrete implementation/factory used to create a
`Pageable`.

Example:

``` java
Pageable pageable = PageRequest.of(page, size, sort);
```

Important:

> `PageRequest.of(...)` creates request instructions. It does not itself
> query the database.

### `Page<T>`

`Page<Employee>` contains:

-   current page content
-   total matching elements
-   total pages
-   page number
-   page size
-   first/last information

Spring Data's inherited repository methods already support pagination,
so no custom declaration of `findAll(Pageable)` was required.

## Zero-Based Pages

Pages are zero-based:

``` text
page=0 → first page
page=1 → second page
page=2 → third page
```

Conceptually:

``` text
offset = page × size
```

## Database-Level Pagination

Observed Hibernate SQL used database pagination:

``` sql
select ...
from employees
order by id
offset ? rows
fetch first ? rows only
```

This proves the application is not loading every employee and slicing
the result in Java.

Spring Data can also execute a count query:

``` sql
select count(...)
from employees
```

The count query supplies metadata such as:

-   `totalElements`
-   `totalPages`
-   `first`
-   `last`

The count query does **not** decide how many rows appear on the current
page. The page size controls that.

------------------------------------------------------------------------

# 4. Sorting

Sorting was added to the paginated employee endpoint.

Current API parameters:

``` text
page
size
sortBy
direction
```

Defaults:

``` text
page=0
size=5
sortBy=id
direction=asc
```

Service logic creates a `Sort`:

``` java
Sort sort = direction.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();
```

Then:

``` java
Pageable pageable = PageRequest.of(page, size, sort);
```

## Ternary Operator

The expression:

``` java
condition ? valueIfTrue : valueIfFalse
```

is a compact form of a simple `if/else`.

## Deterministic Pagination

Pagination should normally have an `ORDER BY`.

Without deterministic sorting, a database is not required to return rows
in a stable order, which can cause records to move between pages.

Defaulting to:

``` text
sortBy=id
direction=asc
```

gives the API a stable default order.

### Known Tie Limitation

If sorting only by a non-unique field such as `firstName`, duplicate
values can have an unspecified relative order.

Example:

``` text
Sony
Sony
```

A secondary tie-breaker such as `id` can be introduced later if stricter
deterministic ordering is required.

------------------------------------------------------------------------

# 5. Combined Search + Pagination + Sorting

Search was upgraded so all three behaviors work together.

## Repository

``` java
Page<Employee> findByFirstNameContainingIgnoreCase(
        String firstName,
        Pageable pageable
);
```

## Service Flow

``` text
Search name
    +
page / size
    +
sort field / direction
        ↓
validate parameters
        ↓
create Sort
        ↓
create Pageable
        ↓
Spring Data repository
        ↓
Hibernate
        ↓
PostgreSQL
```

Observed SQL conceptually combines:

``` text
WHERE       → filtering/search
ORDER BY    → sorting
OFFSET/FETCH→ pagination
COUNT       → total matching records for metadata
```

Example:

``` text
GET /api/employees/search?name=son&page=0&size=1
```

Verified behavior included:

``` text
totalElements = 2
totalPages    = 2
page          = 0
size          = 1
first         = true
last          = false
```

A no-match search returns `200 OK` with an empty page rather than `404`.

------------------------------------------------------------------------

# 6. API Parameter Validation

Before validation, invalid query parameters could leak framework
exceptions as `500 Internal Server Error` or silently fall back to
unintended behavior.

Examples observed before the fix:

``` text
page=-1
    → IllegalArgumentException / 500

sortBy=banana
    → PropertyReferenceException / 500

direction=hello
    → silently behaved like ascending

name=
    → broad blank search
```

These are client-input problems and should result in controlled
`400 Bad Request` responses.

## Custom Exception

Created:

``` text
exception/InvalidRequestParameterException.java
```

``` java
public class InvalidRequestParameterException extends RuntimeException {

    public InvalidRequestParameterException(String message) {
        super(message);
    }
}
```

## Validation Rules

Current pagination/sorting validation:

``` java
private void validatePaginationAndSorting(
        int page,
        int size,
        String sortBy,
        String direction) {

    if (page < 0) {
        throw new InvalidRequestParameterException(
                "Page number must not be less than 0"
        );
    }

    if (size < 1 || size > 100) {
        throw new InvalidRequestParameterException(
                "Page size must be between 1 and 100"
        );
    }

    if (!sortBy.equals("id")
            && !sortBy.equals("firstName")
            && !sortBy.equals("lastName")
            && !sortBy.equals("email")) {
        throw new InvalidRequestParameterException(
                "Invalid sort field: " + sortBy
        );
    }

    if (!direction.equalsIgnoreCase("asc")
            && !direction.equalsIgnoreCase("desc")) {
        throw new InvalidRequestParameterException(
                "Sort direction must be 'asc' or 'desc'"
        );
    }
}
```

Current allowed sort fields:

``` text
id
firstName
lastName
email
```

Current maximum page size:

``` text
100
```

Search additionally rejects blank names:

``` java
if (name == null || name.isBlank()) {
    throw new InvalidRequestParameterException(
            "Search name must not be blank"
    );
}
```

`String.isBlank()` detects both empty and whitespace-only strings.

## Global Exception Handling

`GlobalExceptionHandler` handles the custom exception and returns
`400 Bad Request`.

Example structure:

``` json
{
  "error": "Bad Request",
  "message": "Sort direction must be 'asc' or 'desc'"
}
```

## Status-Code Distinction

``` text
400 Bad Request
    → invalid client input

404 Not Found
    → requested resource does not exist

500 Internal Server Error
    → unexpected/unhandled server-side failure
```

## Missing vs Blank Search Parameter

These are different cases.

Missing:

``` text
GET /api/employees/search
```

Spring MVC detects that required `@RequestParam String name` is absent
and returns `400`.

Blank:

``` text
GET /api/employees/search?name=
```

The parameter exists, so the request can reach the application. Service
validation rejects it using `name.isBlank()` and the custom exception
handler returns `400`.

------------------------------------------------------------------------

# 7. Stable Custom Pagination Response DTO

Initially the API returned:

``` java
Page<EmployeeResponse>
```

directly.

Spring emitted a warning similar to:

``` text
Serializing PageImpl instances as-is is not supported...
```

Direct serialization also exposed Spring-specific implementation details
such as nested pageable/sort structures.

To keep the public API independent of Spring's internal `PageImpl` JSON
representation, Sprint 2 introduced:

``` text
dto/PageResponse.java
```

## `PageResponse<T>`

``` java
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    // constructor + getters
}
```

The public response now owns a stable contract:

``` json
{
  "content": [],
  "page": 0,
  "size": 5,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Generics

`T` is a type parameter/placeholder.

For:

``` java
PageResponse<EmployeeResponse>
```

`T` becomes:

``` java
EmployeeResponse
```

Therefore:

``` java
List<T>
```

becomes:

``` java
List<EmployeeResponse>
```

This makes the pagination DTO reusable later:

``` java
PageResponse<ProjectResponse>
PageResponse<DepartmentResponse>
```

without creating separate pagination wrapper classes.

## Mapping Internal Page to Public DTO

The repository still returns:

``` java
Page<Employee>
```

internally.

The service converts it to:

``` java
PageResponse<EmployeeResponse>
```

using:

``` text
Page<Employee>
    ↓ getContent()
List<Employee>
    ↓ stream()
Stream<Employee>
    ↓ map(...)
Stream<EmployeeResponse>
    ↓ toList()
List<EmployeeResponse>
```

A helper method centralizes the pagination mapping.

Important architectural distinction:

``` text
Spring Page<Employee>
    → internal persistence/service representation

PageResponse<EmployeeResponse>
    → public API contract
```

After this change, the `PageImpl` serialization warning disappeared.

------------------------------------------------------------------------

# 8. Java Stream API Reinforcement

Sprint 2 reinforced Java Stream usage during DTO mapping.

Example flow:

``` java
employeePage.getContent()
        .stream()
        .map(employee -> mapToResponse(employee))
        .toList();
```

Types through the pipeline:

``` text
getContent()
    → List<Employee>

stream()
    → Stream<Employee>

map(...)
    → Stream<EmployeeResponse>

toList()
    → List<EmployeeResponse>
```

`map(...)` transforms each item from one type/value to another.

This is separate from `Page.map(...)`, which can transform page content
while retaining page metadata.

------------------------------------------------------------------------

# 9. Application Logging

Proper application logging was introduced instead of relying on
`System.out.println()`.

## SLF4J Logger

`EmployeeService` uses:

``` java
private static final Logger logger =
        LoggerFactory.getLogger(EmployeeService.class);
```

SLF4J provides a common logging API/facade.

## Logging Levels

Important levels:

``` text
TRACE
DEBUG
INFO
WARN
ERROR
```

Usage introduced in Sprint 2:

``` text
DEBUG
    → detailed request/operation information useful during development

INFO
    → successful important application events

WARN
    → invalid or suspicious input that is handled

ERROR
    → unexpected serious failures
```

Examples:

``` java
logger.debug(
        "Fetching employees: page={}, size={}, sortBy={}, direction={}",
        page, size, sortBy, direction
);
```

``` java
logger.info(
        "Employee created successfully with id={}",
        savedEmployee.getId()
);
```

Invalid page, size, sort field, direction, and blank search values are
logged at `WARN` before the custom exception is thrown.

## Parameterized Logging

Preferred:

``` java
logger.debug("Fetching page={}, size={}", page, size);
```

instead of manually concatenating strings.

## Sensitive Data Rule

Do not log secrets such as:

-   passwords
-   authentication tokens
-   API keys
-   database passwords

Be cautious with personally identifiable information as well.

------------------------------------------------------------------------

# 10. Spring Profiles

Spring Profiles were introduced so the same Java application can use
environment-specific configuration.

Real projects commonly have environments such as:

``` text
development
QA
staging
production
```

Configuration should not be assumed to be identical across all
environments.

## Configuration Structure

### `application.properties`

Contains common/base configuration:

``` properties
spring.application.name=employee-service

spring.datasource.url=jdbc:postgresql://localhost:5432/nexus
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.profiles.active=dev
```

For the current learning checkpoint, `dev` is selected in the base
properties file.

In a real deployment, the active profile should normally be selected
externally through deployment/environment configuration rather than
permanently hardcoded.

### `application-dev.properties`

Current development-specific configuration:

``` properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

logging.level.com.nexuslabs.employee_service=DEBUG
```

### `application-prod.properties`

Current production-oriented configuration:

``` properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

logging.level.com.nexuslabs.employee_service=INFO
```

These files are not conceptually limited to three properties. They
should contain whatever configuration genuinely differs by environment.

## Effective Configuration

If `dev` is active:

``` text
application.properties
        +
application-dev.properties
```

are used together.

`application-prod.properties` is not active.

Profile-specific values can override base values.

## Package-Specific Logging

``` properties
logging.level.com.nexuslabs.employee_service=DEBUG
```

targets the application package rather than enabling DEBUG globally.

This prevents excessive framework/debug output while still showing
detailed Nexus application logs.

With DEBUG enabled, DEBUG/INFO/WARN/ERROR messages can be shown. With
INFO, DEBUG messages are suppressed.

## Verified Dev Behavior

After enabling the development profile, the service produced:

``` text
DEBUG ... EmployeeService :
Fetching employees: page=0, size=5, sortBy=id, direction=asc
```

Hibernate SQL was also visible because:

``` properties
spring.jpa.show-sql=true
```

## Database Password

The real PostgreSQL password remains external:

``` properties
spring.datasource.password=${DB_PASSWORD}
```

Passwords should not be committed into `application.properties`,
`application-dev.properties`, or other source-controlled configuration
files.

## `ddl-auto`

Current learning setup:

``` text
dev  → update
prod → validate
```

`update` is convenient for local learning but is not the planned
production database migration strategy.

A proper migration tool/process should be introduced later.

------------------------------------------------------------------------

# 11. Repository Testing

Sprint 2 added:

``` text
src/test/java/com/nexuslabs/employee_service/repository/
└── EmployeeRepositoryTest.java
```

## Purpose

Repository tests verify actual persistence/query behavior rather than
mocking the repository.

Architecture:

``` text
EmployeeRepositoryTest
        ↓
Spring Data JPA repository
        ↓
Hibernate
        ↓
H2 in-memory database
```

## H2 Test Dependency

H2 was added with test scope:

``` xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

`test` scope keeps H2 as a testing dependency rather than changing the
application's normal PostgreSQL runtime database.

## `@DataJpaTest`

The repository test uses Spring's JPA-focused test support.

The repository is injected with:

``` java
@Autowired
private EmployeeRepository employeeRepository;
```

This is a real Spring Data repository bean for the test context, not a
Mockito repository mock.

## H2 Verification

The logs explicitly confirmed datasource replacement:

``` text
Replacing 'dataSource' DataSource bean with embedded version
```

and:

``` text
Database JDBC URL [jdbc:h2:mem:...]
Database driver: H2 JDBC Driver
Database dialect: H2Dialect
```

Therefore repository test inserts/selects did not modify the normal
PostgreSQL `nexus` employee data.

## Repository Tests Added

Three repository tests verify:

1.  Case-insensitive partial first-name search.
2.  Pagination of matching search results.
3.  Empty page behavior when no employee matches.

Verified result:

``` text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Arrange / Act / Assert

Repository tests reinforced the common AAA structure:

``` text
Arrange
    → prepare controlled data

Act
    → execute behavior

Assert
    → verify result
```

## H2 Limitation

H2 is useful for fast isolated repository tests, but H2 and PostgreSQL
are not identical databases.

A passing H2 test does not prove every PostgreSQL-specific behavior.
PostgreSQL-backed integration testing can be introduced later when
database-specific behavior becomes important.

------------------------------------------------------------------------

# 12. Controller Testing with MockMvc

Sprint 2 added:

``` text
src/test/java/com/nexuslabs/employee_service/controller/
└── EmployeeControllerTest.java
```

The Spring Boot 4.1 MVC test annotation used by this project is:

``` java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

## Purpose

Controller tests focus on the HTTP/web layer:

``` text
MockMvc
    ↓
Spring MVC / DispatcherServlet
    ↓
EmployeeController       ← real
    ↓
EmployeeService          ← Mockito mock
```

The controller test does not need:

``` text
EmployeeRepository
Hibernate
H2
PostgreSQL
```

because those layers are outside the responsibility being tested.

## `MockMvc`

`MockMvc` simulates HTTP requests against Spring MVC.

Instead of manually sending:

``` text
GET /api/employees?page=0&size=5
```

through Postman, an automated test can perform the request and assert:

-   HTTP status
-   request parameter binding
-   default parameters
-   JSON response structure
-   response values

## `@MockitoBean`

The controller test uses a mocked service:

``` java
@MockitoBean
private EmployeeService employeeService;
```

The controller is real, but its service dependency is replaced by a
Mockito mock.

Example stubbing:

``` java
when(employeeService.getAllEmployees(
        0,
        5,
        "id",
        "asc"
)).thenReturn(pageResponse);
```

This does **not** execute the real
`EmployeeService.getAllEmployees(...)` implementation.

It tells the mock:

``` text
when these exact arguments are received
    ↓
return this predefined PageResponse
```

## JSONPath

JSONPath assertions were introduced.

Example:

``` java
jsonPath("$.content[0].firstName")
```

means:

``` text
$             → root JSON object
.content      → content property
[0]           → first array element
.firstName    → firstName property
```

## Controller Tests Added

Three controller tests verify:

1.  Paginated employee response and JSON metadata.
2.  Employee search endpoint and default sorting parameters.
3.  `400 Bad Request` when required search parameter `name` is missing.

Verified result:

``` text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Missing Parameter Behavior

For:

``` text
GET /api/employees/search
```

Spring MVC rejects the request before the service is needed because
required:

``` java
@RequestParam String name
```

is absent.

Observed:

``` text
MissingServletRequestParameterException
```

and the test correctly expected HTTP `400`.

------------------------------------------------------------------------

# 13. Testing Layers --- Final Mental Model

Sprint 2 established three different testing styles.

## Service Unit Test

``` text
EmployeeServiceTest

EmployeeService      = REAL
EmployeeRepository   = MOCK
Database             = NONE
```

Purpose:

> Verify service/business logic independently from persistence.

## Repository Test

``` text
EmployeeRepositoryTest

EmployeeRepository   = REAL Spring Data repository
Hibernate/JPA        = REAL
Database             = H2 temporary in-memory database
```

Purpose:

> Verify persistence and query behavior.

## Controller Test

``` text
EmployeeControllerTest

Spring MVC           = REAL test infrastructure
EmployeeController   = REAL
EmployeeService      = MOCK
Repository           = not involved
Database             = not involved
```

Purpose:

> Verify HTTP mapping, request binding, status codes, and JSON
> responses.

The main lesson is **test responsibility and isolation**, not merely
memorizing annotations.

A failure in an isolated controller test should not immediately be
blamed on PostgreSQL/Hibernate because those components are not part of
that test path.

------------------------------------------------------------------------

# 14. Full Test Suite

Final verified Maven test run:

``` text
EmployeeControllerTest     3 tests
EmployeeRepositoryTest     3 tests
EmployeeServiceTest        7 tests
Application context test   1 test
-----------------------------------
Total                     14 tests
```

Final result:

``` text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Important Test-Environment Observation

The full console output revealed an important distinction.

### `EmployeeRepositoryTest`

Spring replaced the normal datasource with H2:

``` text
jdbc:h2:mem:...
```

### `EmployeeServiceApplicationTests`

The broader Spring Boot application-context test connected to:

``` text
jdbc:postgresql://localhost:5432/nexus
```

because the active `dev` profile loaded the normal development
datasource.

The context test currently does not intentionally modify employee data,
but this creates an unnecessary dependency on the local PostgreSQL
development environment.

### Future Improvement

Create dedicated test configuration so automated tests do not
unnecessarily depend on the developer's normal PostgreSQL `nexus`
database.

This is a future hardening item and does not block Sprint 2 completion.

------------------------------------------------------------------------

# 15. Important Files Added or Changed in Sprint 2

## Main Code

``` text
src/main/java/com/nexuslabs/employee_service/
├── controller/
│   └── EmployeeController.java
├── dto/
│   └── PageResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── InvalidRequestParameterException.java
├── repository/
│   └── EmployeeRepository.java
└── service/
    └── EmployeeService.java
```

## Configuration

``` text
src/main/resources/
├── application.properties
├── application-dev.properties
└── application-prod.properties
```

## Tests

``` text
src/test/java/com/nexuslabs/employee_service/
├── EmployeeServiceApplicationTests.java
├── controller/
│   └── EmployeeControllerTest.java
├── repository/
│   └── EmployeeRepositoryTest.java
└── service/
    └── EmployeeServiceTest.java
```

## Build Configuration

``` text
pom.xml
```

was updated to include the H2 test dependency.

------------------------------------------------------------------------

# 16. Important Design Decisions

### 1. Search uses a Spring Data derived query

No manual JPQL/native SQL was required for the current first-name
search.

### 2. Pagination happens in the database

Do not load all employees and manually slice a Java `List`.

### 3. Pagination uses deterministic default sorting

Default:

``` text
id ASC
```

### 4. Sort fields are whitelisted

Clients cannot pass arbitrary property names directly to Spring Data.

Allowed:

``` text
id
firstName
lastName
email
```

### 5. Page size is bounded

``` text
1–100
```

### 6. Invalid client query parameters return `400`

They should not leak ordinary framework exceptions as `500`.

### 7. Public API does not expose Spring `PageImpl`

`PageResponse<T>` is the stable API contract.

### 8. Logging uses SLF4J

Do not use `System.out.println()` for application logging.

### 9. Environment-specific configuration uses Spring Profiles

Common configuration remains in the base file; differing configuration
belongs in profile-specific files.

### 10. Secrets stay outside source-controlled property files

Database password remains:

``` properties
${DB_PASSWORD}
```

### 11. Tests are layered

Controller, service, and repository tests deliberately test different
responsibilities.

------------------------------------------------------------------------

# 17. Known Limitations / Future Improvements

These are known follow-up items, not Sprint 2 failures.

### Deterministic tie-breaking

Sorting by a non-unique field such as `firstName` can leave duplicate
values with unspecified relative order.

Possible future improvement:

``` text
firstName ASC, id ASC
```

### Search scope

Current search is first-name substring search only.

Future search requirements should be designed deliberately rather than
adding many derived query combinations without need.

### Test datasource isolation

The general Spring Boot context test currently connects to local
PostgreSQL under the active `dev` profile.

A dedicated test profile/configuration should remove this dependency
later.

### H2 vs PostgreSQL

H2 repository tests do not guarantee PostgreSQL-specific SQL behavior.

Use PostgreSQL-backed integration tests later where database-specific
behavior matters.

### Production database migrations

`ddl-auto=update` is only a local development convenience.

A migration strategy/tool should be introduced before treating the
database setup as production-ready.

### Active profile selection

`spring.profiles.active=dev` is acceptable for the current learning
checkpoint.

For deployment, select the environment externally rather than hardcoding
production/development selection in source-controlled base
configuration.

### Mockito Java Agent Warning

The Maven test run currently shows a Mockito/Byte Buddy warning about
dynamic Java-agent attachment becoming restricted in future JDK
releases.

It does not fail the current Java 21 tests, but build configuration
should be revisited when required by future JDK/Mockito versions.

### Service Test Log ID

One mocked service test logs:

``` text
Employee created successfully with id=null
```

because the mocked repository result does not currently simulate a
database-generated ID.

This does not indicate a production failure. The unit-test fixture can
be improved later to return an employee with an ID if the test should
verify/log that value.

------------------------------------------------------------------------

# 18. Key Learning Summary

By the end of Sprint 2, the Employee Service progressed from basic CRUD
into a more realistic backend API.

Major concepts learned and practiced:

-   Spring Data JPA derived query methods
-   `@RequestParam`
-   Search/filter APIs
-   `Pageable`
-   `PageRequest`
-   `Page<T>`
-   Database-level pagination
-   Count queries and pagination metadata
-   Sorting and deterministic ordering
-   Combined search + pagination + sorting
-   Query-parameter validation
-   `String.isBlank()`
-   Custom runtime exceptions
-   Centralized `400 Bad Request` handling
-   Generic DTOs with `PageResponse<T>`
-   Java Streams and DTO mapping
-   Stable API contracts
-   SLF4J logging
-   Logging levels
-   Parameterized logging
-   Spring Profiles
-   Base vs environment-specific configuration
-   Package-specific log levels
-   H2 in-memory test database
-   `@DataJpaTest`
-   Repository integration-style testing
-   Arrange / Act / Assert
-   `@WebMvcTest`
-   `MockMvc`
-   `@MockitoBean`
-   Mockito stubbing
-   JSONPath assertions
-   Controller/service/repository test isolation
-   Full Maven regression testing

------------------------------------------------------------------------

# 19. Sprint 2 Completion Criteria

Sprint 2 is considered complete because:

-   [x] Employee first-name search implemented.
-   [x] Search is case-insensitive and supports substring matching.
-   [x] Pagination implemented.
-   [x] Sorting implemented.
-   [x] Search + pagination + sorting combined.
-   [x] Invalid pagination/sorting/search parameters handled as `400`.
-   [x] Stable custom pagination response DTO implemented.
-   [x] `PageImpl` serialization warning removed from public API flow.
-   [x] Application logging introduced.
-   [x] Development/production Spring profile configuration introduced.
-   [x] Development DEBUG logging verified.
-   [x] Repository tests added using H2.
-   [x] H2 datasource replacement verified from logs.
-   [x] Controller tests added using MockMvc.
-   [x] Missing required request parameter behavior tested.
-   [x] Complete automated suite passes: **14/14**.
-   [x] No failures or errors in the final Maven test run.

------------------------------------------------------------------------

# 20. Git Checkpoint

Sprint 2 work was developed on:

``` text
feature/sprint-2-employee-search
```

Before merging, verify:

``` powershell
git status
git diff --stat
```

Add the implementation and Sprint 2 notes:

``` powershell
git add .
```

Recommended Sprint 2 commit:

``` powershell
git commit -m "feat(employee): complete sprint 2 search and testing"
```

Then verify:

``` powershell
git status
git log --oneline --decorate -5
```

After confirming the feature branch is clean and tests are still
successful, merge into `develop` using the project's established Git
workflow.

------------------------------------------------------------------------

## Sprint 2 Final State

Sprint 2 leaves `employee-service` with a stronger API foundation:

``` text
HTTP / Spring MVC
        ↓
EmployeeController
        ↓
EmployeeService
        ↓
EmployeeRepository
        ↓
Spring Data JPA / Hibernate
        ↓
PostgreSQL
```

with cross-cutting support for:

``` text
DTO contracts
request validation
exception handling
search
pagination
sorting
logging
environment-specific configuration
automated layered testing
```

The next sprint should build on this foundation rather than bypass it.
