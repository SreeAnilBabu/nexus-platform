# Sprint 1 --- Employee Service Foundation

## 1. Sprint Goal

Build the first complete backend vertical slice of the Nexus Platform
`employee-service` while learning the Spring Boot, Maven, REST API,
persistence, validation, exception-handling, DTO, database-integrity,
and automated-testing fundamentals behind it.

By the end of Sprint 1, the service provides a layered Employee REST API
backed by PostgreSQL with:

- Spring Boot application startup and auto-configuration
- PostgreSQL connectivity
- JPA/Hibernate persistence
- Spring Data JPA repository
- Controller → Service → Repository layering
- Full Employee CRUD
- Request and response DTOs
- Bean Validation
- Structured validation errors
- Custom 404 handling
- Email uniqueness handling with HTTP 409
- Database-level email uniqueness
- Automated service tests
- Spring application-context test
- Database credentials externalized through an environment variable

---

## 2. Sprint Status: COMPLETE

**Sprint 1 implementation and technical validation are complete.**

Final validation completed on 25 August 2026:

- Application starts successfully with externalized database
  credentials.
- All 8 automated tests pass.
- Final API smoke test passed:
  - `GET /api/employees` → `200 OK`
  - Valid `POST /api/employees` → `201 Created`
  - Duplicate-email `POST /api/employees` → `409 Conflict`
  - `GET /api/employees/999` → `404 Not Found`

The Sprint 1 Git checkpoint is also complete. The final changes were
reviewed, committed on a feature branch, pushed, merged into `develop`,
and pushed to `develop`. The working tree was then verified clean.

---

## 3. Technology Baseline

Technology Version / Usage

---

Java 21.0.12 LTS
Spring Boot 4.1.0
Hibernate ORM 7.4.1.Final
PostgreSQL 17.10
Maven 3.9.11
PostgreSQL JDBC Driver 42.7.11 runtime dependency
Embedded Web Server Tomcat
Default Port 8080
Database `nexus`
Main Schema `public`
API Testing Postman
Automated Testing JUnit + Mockito through Spring Boot test support

Current Java package:

```text
com.nexuslabs.employee_service
```

The underscore package name is intentionally left unchanged during
Sprint 1. A controlled package-name cleanup can be considered separately
rather than mixing it into unrelated feature work.

---

# Part I --- Project and Maven Foundation

## 4. Spring Boot Project Foundation

The Employee Service was generated as a Maven-based Spring Boot project
under:

```text
backend/employee-service/
```

Important project coordinates:

```text
groupId    : com.nexuslabs
artifactId : employee-service
version    : 0.0.1-SNAPSHOT
Java       : 21
```

The project uses a JAR-based Spring Boot application.

Important dependencies introduced during the foundation included:

- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL JDBC Driver
- Spring Boot Test
- Development tooling

---

## 5. Maven Fundamentals

### 5.1 What Maven does

Maven is the project's build and dependency-management tool.

Instead of manually downloading JAR files and compiling every source
file ourselves, Maven can:

- Resolve dependencies
- Resolve transitive dependencies
- Compile source code
- Compile test code
- Run tests
- Package the application
- Execute Maven plugins
- Run the Spring Boot application

Examples used during Sprint 1:

```bash
mvn dependency:tree
mvn spring-boot:run
mvn test
```

---

### 5.2 Direct Dependencies

A direct dependency is explicitly declared by our project in `pom.xml`.

Example concept:

```text
employee-service
    ↓
PostgreSQL JDBC Driver
```

Our project directly requests the PostgreSQL driver.

---

### 5.3 Transitive Dependencies

A transitive dependency is not necessarily declared directly by our
application. It is downloaded because another dependency requires it.

Conceptual example:

```text
employee-service
        ↓
spring-boot-starter-data-jpa
        ↓
Spring Data JPA / Hibernate related libraries
        ↓
hibernate-core
```

This is one of the major benefits of dependency management: we do not
manually locate every library required by a framework.

Command used to inspect the dependency graph:

```bash
mvn dependency:tree
```

The PostgreSQL dependency was verified with:

```powershell
mvn dependency:tree | Select-String "postgresql"
```

Observed:

```text
org.postgresql:postgresql:jar:42.7.11:runtime
```

---

### 5.4 Spring Boot Starters

A Spring Boot starter is a curated dependency bundle for a particular
application capability.

Examples:

```text
spring-boot-starter-webmvc
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-test
```

Important distinction:

```text
Starter               → which related dependencies should come together
Dependency management → which compatible versions should be used
```

A starter reduces the amount of individual framework dependencies we
need to manage manually.

---

### 5.5 Dependency Scopes

Important Maven scopes reviewed:

#### Compile

Dependency is available during compilation and normally at runtime.

#### Runtime

Dependency is primarily needed when the application executes.

The PostgreSQL JDBC driver is a good example:

```text
PostgreSQL JDBC Driver → runtime
```

Our Java code normally works against JDBC/DataSource abstractions, while
the PostgreSQL-specific driver is required when the application actually
connects to PostgreSQL.

#### Test

Dependency is required for tests but not for the production
application's runtime classpath.

Testing libraries are examples.

---

### 5.6 Parent POM and Dependency Management

Spring Boot dependency management provides tested dependency versions.

Important distinction:

```text
<dependencies>
```

actually adds a dependency to the project.

```text
<dependencyManagement>
```

primarily manages versions/configuration for dependencies when they are
used.

Without dependency management, we would have to specify and maintain
many compatible versions ourselves.

---

## 6. Maven Dependency vs Maven Plugin

This distinction was an important Sprint 1 learning point.

### Dependency

A dependency is code/library functionality used by the application.

Examples:

```text
Spring Data JPA
PostgreSQL JDBC Driver
Validation libraries
```

### Plugin

A plugin is used by Maven to perform build-related tasks.

Example:

```text
Spring Boot Maven Plugin
```

It enables operations such as:

```bash
mvn spring-boot:run
```

Simple rule:

```text
Dependency → used by the application
Plugin     → used by Maven/build process
```

---

# Part II --- Spring Boot Fundamentals

## 7. Spring Boot Application Startup

Main application class:

```java
@SpringBootApplication
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
```

---

## 8. `SpringApplication.run()`

The call:

```java
SpringApplication.run(EmployeeServiceApplication.class, args);
```

begins the Spring Boot startup process.

Conceptually it causes Spring Boot to:

1.  Bootstrap the application.
2.  Create the Spring `ApplicationContext`.
3.  Process configuration.
4.  Perform component scanning.
5.  Apply auto-configuration.
6.  Create and register Spring Beans.
7.  Configure required infrastructure.
8.  Start the embedded web server for the web application.

This is why one line can trigger a large amount of framework setup.

---

## 9. `@SpringBootApplication`

`@SpringBootApplication` conceptually combines three important ideas:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

### `@SpringBootConfiguration`

Marks the class as a Spring Boot configuration source.

### `@EnableAutoConfiguration`

Allows Spring Boot to inspect the classpath and existing configuration
and automatically configure appropriate components.

Example from this sprint:

Because JPA and PostgreSQL-related dependencies/configuration were
present, Spring Boot configured persistence-related infrastructure such
as the DataSource, JPA, Hibernate, and repository support.

### `@ComponentScan`

Spring scans packages for Spring-managed components.

Because the main class is in:

```text
com.nexuslabs.employee_service
```

and our application classes are in subpackages such as:

```text
controller
service
repository
```

Spring can discover them through component scanning.

---

# Part III --- IoC, Beans and Dependency Injection

## 10. Inversion of Control --- IoC

Without Spring, application code could manually create dependencies:

```java
EmployeeRepository repository = ...;
EmployeeService service = new EmployeeService(repository);
```

As the application grows, manually creating and connecting objects
becomes difficult.

With Spring:

```text
Spring controls object creation and lifecycle management.
```

This is the core idea of **Inversion of Control**.

---

## 11. Spring Bean

A Spring Bean is an object created and managed by the Spring
container/ApplicationContext.

Classes marked with stereotypes such as:

```java
@Service
@RestController
@Component
```

can become Spring-managed Beans when discovered through component
scanning.

---

## 12. Dependency Injection

Dependency Injection means that a class receives the dependency it needs
instead of creating that dependency itself.

Example:

```java
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
}
```

`EmployeeService` needs an `EmployeeRepository`.

It does not manually construct the repository.

Spring supplies the repository dependency.

Conceptually:

```text
IoC → Spring owns object creation/management.

DI  → Spring supplies one object's required dependencies to another object.
```

---

## 13. Constructor Injection

Constructor injection was used throughout the Employee Service.

Benefits include:

- Dependencies are explicit.
- Required dependencies can be `final`.
- Classes are easier to unit test.
- Dependencies can be replaced with mocks during testing.
- It avoids hidden dependencies.

Temporary demo classes such as greeting-related classes were created
while learning Bean creation and DI and were removed once the concepts
were understood.

---

# Part IV --- PostgreSQL and DataSource

## 14. PostgreSQL Setup

PostgreSQL 17.10 was verified locally.

Initially:

```powershell
psql --version
```

was not recognized because the PostgreSQL `bin` directory was not
available through the expected command path.

The executable was located at:

```text
C:\Program Files\PostgreSQL\17\bin\psql.exe
```

PostgreSQL service:

```text
postgresql-x64-17
```

was confirmed running.

PostgreSQL version was then verified:

```text
PostgreSQL 17.10
```

Database used:

```text
nexus
```

---

## 15. Spring DataSource Configuration

The application connects to:

```text
jdbc:postgresql://localhost:5432/nexus
```

Configuration concept:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/nexus
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

The password is **not stored directly in the committed configuration**.

---

## 16. Externalizing Database Credentials

Originally, the local database password was placed directly in
`application.properties`.

That is unsafe for Git because secrets should not be committed into
source control.

It was replaced with:

```properties
spring.datasource.password=${DB_PASSWORD}
```

The value is supplied locally through an environment variable.

PowerShell example:

```powershell
$env:DB_PASSWORD="your-local-password"
```

The real password must never be written into documentation or committed
to Git.

After this change:

- The application started successfully.
- All 8 automated tests passed.
- The API smoke test passed.

Therefore the credential externalization was successfully validated.

---

## 17. HikariCP Connection Pool

Spring Boot startup logs showed:

```text
HikariPool-1 - Starting...
HikariPool-1 - Added connection ...
HikariPool-1 - Start completed.
```

Conceptual flow:

```text
Spring Boot
    ↓
DataSource
    ↓
HikariCP
    ↓
PostgreSQL JDBC Driver
    ↓
PostgreSQL
```

A connection pool maintains reusable database connections instead of
creating a completely new physical connection for every database
operation.

---

# Part V --- JPA, Hibernate and ORM

## 18. ORM

ORM means **Object Relational Mapping**.

It maps Java objects/classes to relational database structures.

For the Employee Service:

```text
Java                          PostgreSQL
------------------------------------------------
Employee                     employees
id                           id
firstName                    first_name
lastName                     last_name
email                        email
```

Instead of manually writing JDBC code for every basic operation,
Hibernate can translate entity operations into SQL.

---

## 19. JPA vs Hibernate

This distinction is important:

```text
JPA       → specification/API
Hibernate → ORM implementation used to implement persistence behavior
```

JPA defines the standard concepts and annotations.

Hibernate provides the implementation that performs ORM work and
generates/executes SQL.

---

## 20. Employee Entity

The application contains:

```text
entity/Employee.java
```

Core mapping concept:

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
}
```

### `@Entity`

Tells JPA/Hibernate that this Java class represents a persistent entity.

### `@Table`

Maps the entity to the `employees` database table.

### `@Id`

Marks the entity's primary-key field.

### `@GeneratedValue`

Specifies that the ID is generated rather than supplied manually for
every new employee.

### `GenerationType.IDENTITY`

Uses database identity generation for the primary key.

---

## 21. Why `Long` Instead of `long` for the ID

Primitive:

```java
long
```

has a default value of:

```text
0
```

Wrapper:

```java
Long
```

can be:

```text
null
```

Before an entity is inserted, a generated ID may not exist yet.

Therefore:

```text
new Employee → id = null
saved Employee → generated id assigned
```

This makes `Long` appropriate for generated entity identifiers.

---

## 22. JPA No-Argument Constructor

JPA/Hibernate requires entities to have an accessible no-argument
constructor so the persistence framework can instantiate entity objects.

The application can still have additional constructors for convenient
object creation.

---

## 23. Hibernate DDL

Development configuration uses:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

`ddl-auto=update` allowed Hibernate to create/update the schema during
local learning.

Hibernate created the table:

```sql
create table employees (
    id bigint generated by default as identity,
    email varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    primary key (id)
)
```

Important future consideration:

```text
ddl-auto=update
```

is convenient for local learning, but production schema evolution should
later be handled using a migration tool such as Flyway or Liquibase.

---

# Part VI --- Spring Data JPA Repository

## 24. EmployeeRepository

Created:

```text
repository/EmployeeRepository.java
```

Concept:

```java
public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {
}
```

Meaning:

```text
Employee → entity managed by the repository
Long     → type of Employee's primary key
```

---

## 25. Why an Interface Works Without Writing an Implementation

We did not manually create:

```text
EmployeeRepositoryImpl
```

for the standard CRUD behavior.

Spring Data JPA:

1.  Detects the repository interface.
2.  Understands that it extends `JpaRepository`.
3.  Creates a runtime implementation/proxy.
4.  Registers it as a Spring-managed component.
5.  Injects it where required.

Startup confirmed:

```text
Found 1 JPA repository interface.
```

---

## 26. Repository Methods Used

Inherited methods used during Sprint 1 include:

```java
save(...)
findAll()
findById(...)
delete(...)
```

Spring Data JPA translates these operations into persistence behavior
handled through JPA/Hibernate.

---

# Part VII --- Layered Architecture

## 27. Architecture Implemented

The Employee Service now follows:

```text
Client / Postman
        ↓
EmployeeController
        ↓
Request DTOs
        ↓
EmployeeService
        ↓
EmployeeRepository
        ↓
Spring Data JPA
        ↓
Hibernate
        ↓
JDBC Driver
        ↓
PostgreSQL
```

Responses travel back conceptually as:

```text
PostgreSQL
    ↓
Entity
    ↓
EmployeeService
    ↓
EmployeeResponse DTO
    ↓
EmployeeController
    ↓
JSON response
```

---

## 28. Controller Responsibility

`EmployeeController` handles the HTTP/API boundary.

Responsibilities include:

- URL mappings
- HTTP methods
- Path variables
- Request bodies
- Request validation trigger
- HTTP status behavior
- Returning API response DTOs

It should not contain persistence implementation details.

---

## 29. Service Responsibility

`EmployeeService` contains application/business logic.

Responsibilities developed during Sprint 1 include:

- Creating employees
- Reading employees
- Updating employees
- Deleting employees
- Checking missing employees
- Checking duplicate email addresses
- Mapping request DTO data to entities
- Mapping entities to response DTOs

This keeps the controller thin and separates application logic from HTTP
concerns.

---

## 30. Repository Responsibility

`EmployeeRepository` handles the persistence boundary.

The service calls repository methods rather than directly using SQL or
directly controlling Hibernate.

---

# Part VIII --- REST API and CRUD

## 31. Employee REST Endpoints

Base path:

```text
/api/employees
```

Endpoints:

HTTP Method Endpoint Purpose

---

POST `/api/employees` Create employee
GET `/api/employees` Get all employees
GET `/api/employees/{id}` Get one employee
PUT `/api/employees/{id}` Update employee
DELETE `/api/employees/{id}` Delete employee

---

## 32. Controller Annotations Learned

Important annotations include:

```java
@RestController
@RequestMapping
@PostMapping
@GetMapping
@PutMapping
@DeleteMapping
@RequestBody
@PathVariable
@ResponseStatus
@Valid
```

### `@RestController`

Marks the class as a REST controller and allows returned Java values to
be serialized into HTTP response bodies.

### `@RequestMapping`

Defines a common URL path for the controller.

### HTTP mapping annotations

Map Java methods to HTTP operations.

### `@RequestBody`

Converts incoming JSON into a Java request object/DTO.

### `@PathVariable`

Reads a dynamic URL segment such as the employee ID.

### `@ResponseStatus`

Allows explicit success status codes such as:

```text
201 Created
204 No Content
```

---

## 33. HTTP Status Codes Used

Status Meaning Usage

---

200 OK Successful GET/PUT
201 Created Employee successfully created
204 No Content Employee successfully deleted
400 Bad Request Request validation failure
404 Not Found Employee ID does not exist
409 Conflict Email conflicts with an existing employee

This introduced the idea that an API should communicate not only through
JSON but also through meaningful HTTP status codes.

---

# Part IX --- CRUD Execution Flow

## 34. Create Employee

Conceptual flow:

```text
POST JSON
   ↓
CreateEmployeeRequest
   ↓
@Valid
   ↓
EmployeeService
   ↓
duplicate-email check
   ↓
Employee entity
   ↓
EmployeeRepository.save()
   ↓
Hibernate
   ↓
INSERT
   ↓
PostgreSQL
   ↓
saved Employee
   ↓
EmployeeResponse
   ↓
201 Created
```

Observed SQL:

```sql
insert into employees (email,first_name,last_name)
values (?,?,?)
```

---

## 35. Get All Employees

Flow:

```text
GET /api/employees
    ↓
Controller
    ↓
Service
    ↓
repository.findAll()
    ↓
List<Employee>
    ↓
map each Employee
    ↓
List<EmployeeResponse>
    ↓
200 OK
```

---

## 36. Get Employee by ID

Observed SQL concept:

```sql
select ...
from employees
where id=?
```

`?` is a prepared-statement parameter placeholder. The actual ID value
is bound separately rather than directly concatenated into the SQL
string.

---

## 37. Update Employee

Flow:

```text
PUT /api/employees/{id}
    ↓
UpdateEmployeeRequest
    ↓
validation
    ↓
find existing employee
    ↓
404 if missing
    ↓
check email uniqueness correctly
    ↓
update entity fields
    ↓
save/update through persistence context
    ↓
Hibernate UPDATE
    ↓
EmployeeResponse
    ↓
200 OK
```

Observed SQL:

```sql
update employees
set email=?, first_name=?, last_name=?
where id=?
```

---

## 38. Delete Employee

Flow:

```text
DELETE /api/employees/{id}
    ↓
find employee
    ↓
404 if missing
    ↓
repository.delete(...)
    ↓
Hibernate DELETE
    ↓
204 No Content
```

Observed:

```sql
delete from employees where id=?
```

---

# Part X --- Java Concepts Introduced Naturally

## 39. `Optional<T>`

Spring Data's:

```java
findById(id)
```

returns:

```java
Optional<Employee>
```

Important understanding:

```text
Employee exists    → Optional containing Employee
Employee not found → Optional.empty()
```

It does **not** mean that `findById()` simply returns `null`.

`Optional` explicitly represents the possibility that a value may or may
not exist.

---

## 40. `orElseThrow()`

Initial code using a generic:

```java
.orElseThrow()
```

caused:

```text
NoSuchElementException
500 Internal Server Error
```

for a missing employee.

It was improved to:

```java
.orElseThrow(() -> new EmployeeNotFoundException(id))
```

Now the application throws a domain-specific exception.

---

## 41. Lambda Expressions

Example:

```java
() -> new EmployeeNotFoundException(id)
```

This lambda supplies behavior that should execute only if the `Optional`
is empty.

General form:

```text
(parameters) -> behavior
```

Another lambda appeared while building validation errors:

```java
error -> errors.put(
    error.getField(),
    error.getDefaultMessage()
)
```

The important idea is that a lambda represents behavior/functionality
that can be passed and executed where needed.

---

## 42. Stream API

The GET-all operation maps entities into response DTOs using a stream.

Concept:

```java
return employeeRepository.findAll()
        .stream()
        .map(employee -> mapToResponse(employee))
        .toList();
```

Flow:

```text
List<Employee>
    ↓ stream()
Stream<Employee>
    ↓ map(...)
Stream<EmployeeResponse>
    ↓ toList()
List<EmployeeResponse>
```

Equivalent method-reference style introduced:

```java
.map(this::mapToResponse)
```

This was the first practical use of the Java Stream API in the project.

---

# Part XI --- Exception Handling

## 43. Initial Missing-Employee Problem

Originally, requesting a nonexistent employee such as:

```text
GET /api/employees/999
```

produced:

```text
500 Internal Server Error
No value present
```

because a generic `NoSuchElementException` escaped the service.

That response was technically wrong for the API.

The resource was missing, so the correct HTTP meaning is:

```text
404 Not Found
```

---

## 44. `EmployeeNotFoundException`

A custom exception was created:

```text
exception/EmployeeNotFoundException.java
```

The service throws it when the requested employee does not exist.

Concept:

```java
.orElseThrow(() -> new EmployeeNotFoundException(id))
```

---

## 45. Global Exception Handler

Created:

```text
exception/GlobalExceptionHandler.java
```

using concepts such as:

```java
@RestControllerAdvice
@ExceptionHandler
ResponseEntity
```

### Purpose

Instead of repeating error-response logic in every controller method,
exception handling is centralized.

Conceptual flow:

```text
Service throws EmployeeNotFoundException
        ↓
Spring propagates exception
        ↓
GlobalExceptionHandler matches exception
        ↓
structured JSON response
        ↓
404 Not Found
```

Example:

```json
{
  "error": "Employee not found with id 999"
}
```

---

# Part XII --- DTOs

## 46. Why DTOs Were a Major Design Improvement

DTO means **Data Transfer Object**.

Before DTO separation, the API could directly receive/return the JPA
entity:

```text
Client
  ↓
Employee Entity
  ↓
Database
```

That tightly couples the external API contract to the persistence model.

After DTO introduction:

```text
Client
  ↓
Request DTO
  ↓
Service
  ↓
Entity
  ↓
Database
```

Response direction:

```text
Database
  ↓
Entity
  ↓
Service
  ↓
Response DTO
  ↓
Client
```

This is a major architectural improvement because:

- Clients cannot automatically control every entity field.
- Persistence implementation is not directly exposed.
- Validation can be request-specific.
- Create and update contracts can evolve independently.
- Response fields can be explicitly controlled.
- Internal entity fields can later be added without automatically
  exposing them.
- API and database models can evolve separately.

Simple rule:

```text
Request DTO  → what the client may send
Entity       → how the application persists data
Response DTO → what the client may receive
```

---

## 47. DTOs Created

```text
dto/
├── CreateEmployeeRequest.java
├── UpdateEmployeeRequest.java
└── EmployeeResponse.java
```

### `CreateEmployeeRequest`

Represents the allowed input for employee creation.

### `UpdateEmployeeRequest`

Represents the allowed input for employee update.

Even when Create and Update currently contain similar fields, keeping
them separate is useful because their rules may diverge later.

### `EmployeeResponse`

Represents the API response.

The controller therefore no longer needs to expose the JPA `Employee`
entity directly.

---

## 48. DTO-to-Entity and Entity-to-DTO Mapping

Mapping is currently handled in the service layer.

Example response mapping concept:

```java
private EmployeeResponse mapToResponse(Employee employee) {
    return new EmployeeResponse(
            employee.getId(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail()
    );
}
```

For the current small service, manual mapping is clear and sufficient.

A dedicated mapper or MapStruct can be introduced later if mapping
complexity grows.

---

# Part XIII --- Bean Validation

## 49. Request Validation

Validation annotations were added to request DTO fields.

Concept:

```java
@NotBlank(message = "First name is required")
private String firstName;

@NotBlank(message = "Last name is required")
private String lastName;

@NotBlank(message = "Email is required")
@Email(message = "Email must be valid")
private String email;
```

Controller uses:

```java
@Valid @RequestBody CreateEmployeeRequest request
```

and similarly for update requests.

---

## 50. Validation Flow

```text
HTTP JSON
    ↓
Jackson converts JSON to Request DTO
    ↓
@Valid triggers Bean Validation
    ↓
Valid?
 ┌──┴──┐
Yes    No
 ↓      ↓
Service MethodArgumentNotValidException
 ↓      ↓
DB     GlobalExceptionHandler
        ↓
       400
```

A major benefit is that invalid request data is rejected before
persistence logic executes.

---

## 51. Structured Validation Errors

Instead of returning Spring's large default error response, validation
failures are converted into a clean field-to-message structure.

Example:

```json
{
  "firstName": "First name is required",
  "lastName": "Last name is required",
  "email": "Email must be valid"
}
```

HTTP status:

```text
400 Bad Request
```

This makes the API easier for frontend applications and API consumers to
use.

---

## 52. `Map` and `HashMap`

While implementing structured validation errors, the project introduced:

```text
Map
HashMap
```

Important distinction:

```text
Map     → interface
HashMap → mutable implementation
```

A mutable map is useful when validation errors are collected
dynamically.

`Map.of(...)` was also discussed as useful for small fixed immutable
maps.

---

# Part XIV --- Email Uniqueness

## 53. Why Email Uniqueness Was Added

An employee email should identify a single employee in the current
model.

Without a uniqueness rule, the database could contain:

```text
Employee 1 → john@example.com
Employee 2 → john@example.com
```

This can create ambiguous and inconsistent application data.

The uniqueness rule was therefore implemented at **two levels**:

```text
Application level
+
Database level
```

Both are important.

---

## 54. Spring Data Derived Query Method

The repository gained an email-existence query such as:

```java
boolean existsByEmail(String email);
```

Spring Data JPA derives the query from the method name.

The developer writes the method declaration, while Spring Data creates
the implementation behavior.

Observed SQL concept:

```sql
select e1_0.id
from employees e1_0
where e1_0.email=?
fetch first ? rows only
```

The method returns a boolean:

```text
true  → matching email exists
false → matching email does not exist
```

---

## 55. Duplicate Email During Create

Before saving a new employee, the service checks whether the email
already exists.

Concept:

```java
if (employeeRepository.existsByEmail(request.getEmail())) {
    throw new DuplicateEmployeeEmailException(request.getEmail());
}
```

A custom exception was created:

```text
DuplicateEmployeeEmailException
```

and handled globally.

Example API response:

```json
{
  "error": "Employee already exists with email arun@example.com"
}
```

HTTP status:

```text
409 Conflict
```

Why 409?

The request can be structurally valid, but it conflicts with existing
application state.

---

## 56. Update Email Uniqueness --- Important Edge Case

Update logic needs more care than create logic.

Suppose:

```text
Employee 7 → sony@example.com
```

and we update employee 7 while keeping:

```text
sony@example.com
```

That must succeed.

The email exists in the database, but it belongs to the **same
employee** being updated.

However, if employee 7 tries to change its email to:

```text
john@example.com
```

and another employee already owns that email, the update must fail with
`409 Conflict`.

Therefore update uniqueness conceptually asks:

```text
Does another employee already own the requested email?
```

not merely:

```text
Does this email exist anywhere?
```

Tested behaviors:

- Keep employee's existing email → `200 OK`
- Change to a new unused email → `200 OK`
- Change to another employee's email → `409 Conflict`
- Repeat update with the employee's own current email → `200 OK`

This is an important example of business-rule logic belonging in the
service layer.

---

## 57. Database-Level Unique Constraint

Application checks improve the API experience, but they are not enough
to guarantee database integrity.

A database unique constraint was added:

```text
uk_employees_email
```

PostgreSQL validation confirmed the constraint by attempting a duplicate
insert.

Observed database error:

```text
ERROR: duplicate key value violates unique constraint "uk_employees_email"
Key (email)=(john@example.com) already exists.

SQL state: 23505
```

This proves PostgreSQL itself now prevents duplicate employee emails.

---

## 58. Why Both Application and Database Validation Matter

### Application-level check

Provides:

- Friendly API behavior
- Domain-specific exception
- `409 Conflict`
- Clean JSON message

### Database-level constraint

Provides:

- Final data-integrity guarantee
- Protection if another code path bypasses the service check
- Protection against concurrent operations/race conditions that can
  pass a pre-check

Simple rule:

```text
Application check → good API/user experience
Database constraint → authoritative data integrity
```

---

# Part XV --- Automated Testing

## 59. Why Automated Tests Were Introduced

Manual Postman testing is valuable, but it has limitations:

- A person must repeat every request.
- Repeating many scenarios takes time.
- It is easy to forget a case.
- Regression testing becomes harder as the project grows.

Automated tests allow repeatable verification.

Command:

```bash
mvn test
```

Maven compiles and executes the test suite using the configured test
infrastructure.

---

## 60. Application Context Test

Existing test:

```text
EmployeeServiceApplicationTests
```

uses Spring Boot test support to verify that the Spring application
context can start.

This is different from a focused unit test.

It verifies important integration/configuration wiring such as:

- Spring configuration can load.
- Beans can be discovered/created.
- Repository infrastructure can initialize.
- Persistence configuration can initialize.

During this sprint the context test connected to the configured
PostgreSQL database.

---

## 61. EmployeeService Unit Tests

Created:

```text
service/EmployeeServiceTest
```

These tests focus on service behavior.

The real repository is replaced by a Mockito mock.

Conceptual architecture:

```text
EmployeeService
      ↓
Mock EmployeeRepository
```

instead of:

```text
EmployeeService
      ↓
Real EmployeeRepository
      ↓
Hibernate
      ↓
PostgreSQL
```

This makes service tests focused and fast.

---

## 62. JUnit

JUnit provides the testing structure and assertions.

A test generally follows:

```text
Arrange
Act
Assert
```

### Arrange

Prepare inputs and mock behavior.

### Act

Call the method being tested.

### Assert

Verify the result or exception.

---

## 63. Mockito

Mockito is used to create mock dependencies and define expected
dependency behavior.

Important concepts introduced:

- `@Mock`
- `@InjectMocks`
- `when(...).thenReturn(...)`
- `verify(...)`
- `never()`

### `@Mock`

Creates a mock repository.

### `@InjectMocks`

Creates/injects the service under test using the mock dependency.

### `when(...).thenReturn(...)`

Defines how the mock should respond.

Concept:

```java
when(employeeRepository.existsByEmail(...))
        .thenReturn(false);
```

### `verify(...)`

Checks whether an expected repository interaction happened.

### `never()`

Checks that a repository method was **not** called.

This is especially useful for duplicate-email behavior, where `save()`
must not execute after the duplicate is detected.

---

## 64. Assertions

Important assertion concepts introduced include:

```java
assertEquals(...)
assertThrows(...)
```

### `assertEquals`

Checks that actual output matches expected output.

### `assertThrows`

Checks that a particular exception is thrown for an
invalid/business-conflict scenario.

A test is therefore not successful simply because the Java code
executed.

It succeeds only if its assertions and interaction expectations are
satisfied.

---

## 65. What the Tests Actually Verify

Automated tests are executable specifications.

For example, a service test can express:

```text
Given:
email already exists

When:
createEmployee() is called

Then:
DuplicateEmployeeEmailException must be thrown
AND
repository.save() must never be called
```

If the implementation later stops throwing the exception, the test
fails.

If it incorrectly saves the employee, the Mockito verification fails.

This is how automated tests validate functionality rather than merely
compiling code.

---

## 66. Final Automated Test Result

Final Maven result:

```text
EmployeeServiceApplicationTests
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0

EmployeeServiceTest
Tests run: 7
Failures: 0
Errors: 0
Skipped: 0
```

Total:

```text
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Meaning:

```text
1 application-context test
+
7 EmployeeService tests
=
8 passing automated tests
```

---

## 67. Mockito / Java Agent Warning

During testing, Mockito displayed warnings about dynamically attaching
its inline mock-maker/Byte Buddy Java agent.

The warnings did **not** mean the tests failed.

The test result remained:

```text
BUILD SUCCESS
```

This is a future build/tooling compatibility concern rather than a
Sprint 1 functional failure.

It can be addressed later when build hardening requires it.

---

# Part XVI --- Manual API Regression Testing

## 68. Full Postman Regression Test

Postman is the preferred manual API testing tool for this project going
forward.

After DTO and validation refactoring, all major CRUD behaviors were
tested.

\# Scenario Expected / Observed

---

1 POST valid employee `201 Created`
2 POST invalid employee `400 Bad Request` + validation errors
3 GET all employees `200 OK` + response DTO array
4 GET valid employee ID `200 OK`
5 GET nonexistent employee `404 Not Found`
6 PUT valid employee `200 OK` + updated response
7 PUT invalid employee `400 Bad Request`
8 PUT nonexistent employee `404 Not Found`
9 DELETE existing employee `204 No Content`
10 DELETE nonexistent employee `404 Not Found`

Later uniqueness-specific tests also passed:

- Create with unused email → success
- Create with duplicate email → `409 Conflict`
- Update retaining own email → success
- Update to unused email → success
- Update to another employee's email → `409 Conflict`

---

## 69. Final Sprint Smoke Test

After externalizing the PostgreSQL password, a smaller final smoke test
was performed.

Results:

```text
GET /api/employees
→ 200 OK

POST /api/employees with valid employee
→ 201 Created

POST /api/employees with duplicate email
→ 409 Conflict

GET /api/employees/999
→ 404 Not Found
```

This confirmed that the final configuration change did not break core
API behavior.

---

# Part XVII --- Important Debugging Incidents

## 70. `psql` Not Recognized

Problem:

```text
psql : The term 'psql' is not recognized
```

Diagnosis:

PostgreSQL was installed and running, but the executable was not
available through the command lookup path.

Located:

```text
C:\Program Files\PostgreSQL\17\bin\psql.exe
```

Lesson:

```text
Command not recognized ≠ software necessarily missing.
```

Check installation location, executable, service status, and PATH.

---

## 71. DataSource Configuration

The application needed correct datasource configuration and the
PostgreSQL JDBC driver.

Successful startup eventually showed:

```text
Database JDBC URL: jdbc:postgresql://localhost:5432/nexus
Database driver: PostgreSQL JDBC Driver
Database version: 17.10
```

Lesson:

A Spring Data/JPA dependency does not magically tell the application
which database instance, database name, username, and password to use.

---

## 72. Port 8080 Already in Use

Startup reached the database successfully but then failed with:

```text
Web server failed to start. Port 8080 was already in use.
```

This showed that startup has multiple independent stages.

The database can connect successfully while the web server still fails.

After resolving the port conflict:

```text
Tomcat started on port 8080
Started EmployeeServiceApplication
```

---

## 73. Whitelabel Error at `/`

Opening:

```text
http://localhost:8080/
```

returned a Whitelabel 404.

This did **not** mean Spring Boot was broken.

It meant:

```text
Server running
+
No controller mapping exists for "/"
```

---

## 74. Compilation Error from `greet()`

During the DI learning exercise, a method changed from no-argument usage
to a parameterized form.

The application still called:

```java
greetingService.greet()
```

while the method required a `String`.

Maven correctly failed compilation.

Lesson:

```text
Spring/Maven cannot bypass Java compile-time type/method rules.
```

The caller and method signature must agree.

---

## 75. Generic `orElseThrow()` Caused 500

Initial missing-ID behavior produced:

```text
NoSuchElementException
500 Internal Server Error
```

This was improved through:

```text
EmployeeNotFoundException
+
GlobalExceptionHandler
+
404 Not Found
```

Lesson:

Exceptions should be translated into API semantics rather than leaking
generic implementation failures.

---

## 76. PowerShell/cURL JSON Quoting

During API testing, JSON sent through `curl.exe` from PowerShell
produced parsing errors because shell quoting/escaping altered the
request body.

This was eventually handled correctly, and Postman was selected as the
preferred API testing tool going forward.

Lesson:

A `400` response can come from multiple stages:

```text
Malformed JSON
vs
Valid JSON that fails Bean Validation
```

They are different problems.

---

# Part XVIII --- Current Project Structure

## 77. Relevant Main Source Structure

```text
backend/employee-service/
└── src/main/
    ├── java/com/nexuslabs/employee_service/
    │   ├── EmployeeServiceApplication.java
    │   ├── controller/
    │   │   └── EmployeeController.java
    │   ├── dto/
    │   │   ├── CreateEmployeeRequest.java
    │   │   ├── UpdateEmployeeRequest.java
    │   │   └── EmployeeResponse.java
    │   ├── entity/
    │   │   └── Employee.java
    │   ├── exception/
    │   │   ├── EmployeeNotFoundException.java
    │   │   ├── DuplicateEmployeeEmailException.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── repository/
    │   │   └── EmployeeRepository.java
    │   └── service/
    │       └── EmployeeService.java
    └── resources/
        └── application.properties
```

Test structure includes:

```text
src/test/java/com/nexuslabs/employee_service/
├── EmployeeServiceApplicationTests.java
└── service/
    └── EmployeeServiceTest.java
```

---

# Part XIX --- Final Configuration Principles

## 78. `application.properties`

Important configuration concepts at Sprint 1 completion include:

```properties
spring.application.name=employee-service

spring.datasource.url=jdbc:postgresql://localhost:5432/nexus
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Important rules:

### Never commit the actual PostgreSQL password

Use:

```properties
${DB_PASSWORD}
```

and provide it from the runtime environment.

### `ddl-auto=update` is currently for learning/local development

Later database schema changes should move toward controlled migrations.

### SQL logging is useful while learning

```properties
spring.jpa.show-sql=true
```

allowed us to connect repository operations to the SQL generated by
Hibernate.

---

# Part XX --- End-to-End Mental Model

## 79. Valid Create Request

```text
Postman
   ↓
POST /api/employees
   ↓
EmployeeController
   ↓
Jackson
   ↓
CreateEmployeeRequest
   ↓
@Valid
   ↓
EmployeeService
   ↓
existsByEmail(...)
   ↓
Employee entity
   ↓
EmployeeRepository.save(...)
   ↓
Spring Data JPA
   ↓
Hibernate
   ↓
PostgreSQL JDBC Driver
   ↓
PostgreSQL
   ↓
generated Employee ID
   ↓
Employee entity
   ↓
mapToResponse(...)
   ↓
EmployeeResponse
   ↓
JSON
   ↓
201 Created
```

---

## 80. Invalid Request

```text
Postman
   ↓
JSON
   ↓
Request DTO
   ↓
@Valid
   ↓
Bean Validation fails
   ↓
MethodArgumentNotValidException
   ↓
GlobalExceptionHandler
   ↓
field-error JSON
   ↓
400 Bad Request
```

Service/repository persistence logic is not supposed to proceed for this
validation failure.

---

## 81. Missing Employee

```text
GET /api/employees/999
   ↓
Controller
   ↓
Service
   ↓
repository.findById(999)
   ↓
Optional.empty()
   ↓
orElseThrow(...)
   ↓
EmployeeNotFoundException
   ↓
GlobalExceptionHandler
   ↓
404 Not Found
```

---

## 82. Duplicate Employee Email

```text
POST valid request
   ↓
Validation passes
   ↓
Service
   ↓
repository.existsByEmail(email)
   ↓
true
   ↓
DuplicateEmployeeEmailException
   ↓
GlobalExceptionHandler
   ↓
409 Conflict
```

The database unique constraint provides an additional final integrity
guarantee.

---

# Part XXI --- Sprint 1 Definition of Done

## 83. Functional Definition of Done

- [x] Spring Boot application starts.
- [x] Java 21 application builds.
- [x] PostgreSQL connection works.
- [x] `nexus` database is used.
- [x] HikariCP connection pool initializes.
- [x] JPA/Hibernate mapping works.
- [x] `employees` table exists.
- [x] Employee entity is persisted.
- [x] Spring Data repository is detected.
- [x] Controller/service/repository layering is implemented.
- [x] POST employee works.
- [x] GET all employees works.
- [x] GET employee by ID works.
- [x] PUT employee works.
- [x] DELETE employee works.
- [x] `201 Created` is returned for creation.
- [x] `204 No Content` is returned for deletion.
- [x] Missing employees return `404 Not Found`.
- [x] Create request DTO exists.
- [x] Update request DTO exists.
- [x] Response DTO exists.
- [x] JPA entity is not directly used as the controller response
      contract.
- [x] Bean Validation works.
- [x] Validation failures return `400 Bad Request`.
- [x] Structured field validation errors are returned.
- [x] Duplicate email detection works.
- [x] Duplicate email returns `409 Conflict`.
- [x] Update uniqueness handles the employee's own email correctly.
- [x] Database-level email unique constraint exists.
- [x] PostgreSQL rejects duplicate email directly.
- [x] Automated service tests exist.
- [x] Application context test exists.
- [x] All 8 automated tests pass.
- [x] Database password is externalized using `DB_PASSWORD`.
- [x] Application starts with the externalized password.
- [x] Final smoke test passes.
- [x] Sprint 1 notes finalized.

---

## 84. Repository/Git Definition of Done

The Sprint 1 Git checkpoint was completed after the implementation and
technical validation work:

- [x] Create/switch to the Sprint 1 feature branch while preserving
      current working changes.
- [x] Review `git status`.
- [x] Review `git diff`.
- [x] Confirm no database password/secret is present in staged
      changes.
- [x] Stage Sprint 1 source/test/config/documentation changes.
- [x] Commit Sprint 1.
- [x] Push the feature branch.
- [x] Merge Sprint 1 into `develop`.
- [x] Push `develop`.
- [x] Verify working tree is clean.

Therefore:

```text
Sprint 1 technical implementation → COMPLETE
Sprint 1 validation              → COMPLETE
Sprint 1 documentation           → COMPLETE
Sprint 1 Git checkpoint           → COMPLETE
```

---

# Part XXII --- Key Lessons to Remember

## 85. Short Reference

### Maven

```text
Maven manages build lifecycle and dependencies.
Dependency → application library.
Plugin → Maven/build functionality.
Transitive dependency → dependency required by another dependency.
```

### Spring Boot

```text
@SpringBootApplication
    ↓
configuration + auto-configuration + component scanning
```

### Spring IoC

```text
Spring creates and manages application objects.
```

### Dependency Injection

```text
A class receives its dependency instead of constructing it itself.
```

### JPA and Hibernate

```text
JPA       → specification
Hibernate → implementation
```

### Repository

```text
Repository abstracts persistence operations.
Spring Data creates the JpaRepository implementation at runtime.
```

### DTO

```text
Request DTO  → client input contract
Entity       → persistence model
Response DTO → client output contract
```

### Validation

```text
@Valid + Bean Validation
→ reject bad requests before business/persistence logic
```

### Optional

```text
Optional<Employee>
→ explicitly represents present or missing Employee
```

### Exception Handling

```text
Business/domain exception
→ GlobalExceptionHandler
→ correct HTTP response
```

### Email uniqueness

```text
Application check → friendly 409 response
Database UNIQUE   → final integrity guarantee
```

### Unit Testing

```text
Real service + mocked dependency
→ test service behavior in isolation
```

### Context Testing

```text
Spring context starts successfully
→ configuration and Bean wiring can initialize
```

### Postman vs Automated Tests

```text
Postman → manual end-to-end API verification
JUnit/Mockito → repeatable automated behavior verification
```

Both are useful and serve different purposes.

---

# Part XXIII --- What Sprint 1 Achieved

At the beginning of Sprint 1, `employee-service` was primarily a
generated Spring Boot project foundation.

At completion, it became a functioning backend service with a real
database and a recognizable production-style layered structure:

```text
REST API
+
DTO contracts
+
Validation
+
Service layer
+
Repository layer
+
JPA/Hibernate
+
PostgreSQL
+
Exception handling
+
HTTP semantics
+
Application uniqueness rules
+
Database integrity rules
+
Automated tests
+
Externalized credentials
```

More importantly, each layer was introduced together with its purpose
rather than simply copying code.

Sprint 1 therefore establishes the foundation on which later Nexus
Platform features can be built without immediately jumping into
security, Kafka, or microservice complexity before the core backend
fundamentals are understood.

---

# Part XXIV --- Sprint 2 Transition

The Sprint 1 Git checkpoint is complete, so Sprint 1 is formally closed
in Git. The next action is Sprint 2 planning and implementation.
