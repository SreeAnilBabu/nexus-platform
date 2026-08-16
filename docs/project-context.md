# Nexus Platform — Persistent Project Context & Learning Handoff

> **Purpose:** This file is the authoritative handoff document for the Nexus Platform project and its associated learning journey. It is intended to preserve the important technical state, decisions, Git history, learning progress, corrections, environment details, and teaching rules across ChatGPT sessions.
>
> **Rule:** When a major project milestone, decision, correction, or learning milestone is completed, update this file. Do not rely only on chat history.

---

## 1. Project Identity

- **Project:** Nexus Platform
- **GitHub repository:** `https://github.com/SreeAnilBabu/nexus-platform.git`
- **Local workspace:** `F:\Workspace\nexus-platform`
- **Current backend service:** `employee-service`
- **Current Maven coordinates:**
  - `groupId`: `com.nexuslabs`
  - `artifactId`: `employee-service`
  - `version`: `0.0.1-SNAPSHOT`
- **Current Java:** Amazon Corretto JDK 21
- **Current Spring Boot generated version:** 4.1.0
- **Database:** PostgreSQL 17
- **Database name:** `nexus`

### Project purpose

This is a practical, production-oriented learning/portfolio project. The goal is not merely to finish an application, but to gain real project experience by implementing and understanding:

- Java fundamentals and modern Java
- Important Java 8 features
- Java 17+
- Java 21
- Maven
- Spring Boot
- Spring MVC
- RESTful APIs
- Validation
- JPA
- Hibernate
- PostgreSQL
- Authentication
- Authorization
- Kafka
- Microservices
- Docker/Docker Compose
- Testing
- Exception handling
- Logging
- Configuration
- API design
- Git/branching
- Production-oriented engineering practices

The project should evolve gradually. Do not introduce microservice/distributed-system complexity before the underlying monolithic/backend concepts are understood.

---

# 2. User Learning Profile and Teaching Requirements

## Java background — important correction

Do **NOT** describe the user as already having strong Java 8 knowledge.

The user explicitly corrected this assumption:

> The user does not have much exposure to Java 8.

The user has some Java knowledge but wants to strengthen fundamentals and learn Java properly through the project.

### Java 8 must still be taught

Important Java 8 concepts should be deliberately incorporated where appropriate:

- Lambda expressions
- Functional interfaces
- Stream API
- Method references
- Optional
- Default/static interface methods
- Java Time API
- Other relevant Java 8 improvements

Then build toward Java 17/21 features.

### Preferred project Java

Use **Java 21** for the actual project unless there is a specific compatibility reason to use another version.

---

## Teaching philosophy

Teach from the perspective of the real project.

For each technology/concept:

1. Explain what it is.
2. Explain why it exists.
3. Explain the problem it solves.
4. Explain the basic internal flow at a useful conceptual level.
5. Implement it in Nexus Platform.
6. Test it.
7. Debug it.
8. Commit it using the Git workflow.
9. Document important decisions.

Do not treat generated files, Maven dependencies, Spring Boot annotations, auto-configuration, or framework behavior as "magic."

The user likes the approach of being asked questions/exercises and receiving corrections/explanations before proceeding.

---

# 3. Target Technology Stack

## Core

- Java 21
- Maven
- Git
- IntelliJ IDEA

## Spring/backend

- Spring Boot
- Spring MVC
- RESTful APIs
- Spring Data JPA
- Hibernate
- Jakarta Persistence
- Bean Validation
- Spring Security
- Authentication
- Authorization
- JWT or an appropriate authentication mechanism

## Database

- PostgreSQL

## Messaging

- Apache Kafka

## Architecture

- Modular monolith initially
- REST
- Event-driven communication
- Kafka
- Microservices later
- API Gateway later if justified

## Infrastructure

- Docker
- Docker Compose

## Testing

- Unit testing
- Integration testing
- Repository testing
- Controller/API testing
- Spring Boot testing

---

# 4. Development Environment — Completed and Verified

## OS

Windows 11

## Java

Amazon Corretto:

```text
openjdk version "21.0.12" 2026-07-21 LTS
OpenJDK Runtime Environment Corretto-21.0.12.8.1
OpenJDK 64-Bit Server VM Corretto-21.0.12.8.1
```

Command used:

```powershell
java -version
```

Java 21 is already configured in IntelliJ IDEA.

---

## Maven

Version:

```text
Apache Maven 3.9.11
```

Installation:

```text
C:\Program Files\Apache Software Foundation\apache-maven-3.9.11
```

Verified:

```powershell
mvn -version
```

Maven is using:

```text
Java 21.0.12
vendor: Amazon.com Inc.
runtime: C:\Program Files\Amazon Corretto\jdk21.0.12_8
```

---

## Git

Verified:

```text
git version 2.37.0.windows.1
```

---

## IntelliJ IDEA

Installed.

Java 21 is already configured.

The generated Spring Boot project has already been imported successfully.

The IntelliJ terminal shows the same Git branch/state as the normal terminal.

---

## Docker Desktop

Installed and working.

Verified:

```text
Docker version 29.6.2, build dfc4efb
Docker Compose version v5.3.1
```

`docker info` confirmed:

- Client/server working
- Context: `desktop-linux`
- Docker Desktop server running
- WSL2 backend
- Linux containers
- 8 CPUs
- ~2.85 GiB Docker memory at the time checked

The Docker CLI initially was not recognized, but after PATH/terminal refresh it was working.

Do not revisit Docker installation unless a later project requirement needs it.

---

## PostgreSQL

Installed:

```text
PostgreSQL 17.10
```

A database named:

```text
nexus
```

has already been created.

The PostgreSQL installer was used with mostly/default options.

---

# 5. Workspace Structure

Local root:

```text
F:\Workspace\nexus-platform
```

Current intended structure:

```text
nexus-platform/
├── backend/
│   └── employee-service/
├── database/
├── docker/
├── docs/
│   ├── architecture/
│   ├── decisions/
│   ├── sprint-notes/
│   ├── engineering-journal.md
│   └── project-context.md
├── frontend/
├── scripts/
├── .gitignore
├── LICENSE
└── README.md
```

### Empty directories and Git

Git does not track empty directories.

`.gitkeep` was used for initially empty directories when we wanted them represented in Git.

Later, it was decided that `docs/architecture/` and `docs/decisions/` do not need artificial placeholder files merely to make them appear. They can receive real documentation/ADRs when appropriate.

---

# 6. Git Repository Setup — Completed

The repository was initialized locally:

```powershell
cd F:\Workspace\nexus-platform
git init
```

Remote was added:

```powershell
git remote add origin https://github.com/SreeAnilBabu/nexus-platform.git
```

Verified with:

```powershell
git remote -v
```

Initial files:

- `.gitignore`
- `LICENSE`
- `README.md`

were committed and pushed to `main`.

The initial commit was:

```text
Initial project Structure
```

The local branch was renamed from `master` to `main`:

```powershell
git branch -M main
```

Then:

```powershell
git push -u origin main
```

---

# 7. Git Branch Strategy

Branches established:

```text
main
develop
```

Meaning:

- `main` = stable/mainline branch
- `develop` = integration/development branch
- `feature/*` = short-lived feature/documentation branches

Example:

```text
feature/sprint-0-documentation
```

was used during Sprint 0.

### Preferred workflow

```text
develop
   ↓
create feature branch
   ↓
make changes
   ↓
git status
   ↓
git add
   ↓
git commit
   ↓
git push feature branch
   ↓
checkout develop
   ↓
git pull
   ↓
git merge feature branch
   ↓
git push develop
   ↓
delete feature branch when complete
```

GitHub pull requests are optional for this personal project; command-line merging is valid and has been practiced.

### Slash in branch names

A branch name such as:

```text
feature/sprint-0-documentation
```

is a single Git branch name.

Quotes are normally unnecessary:

```powershell
git checkout feature/sprint-0-documentation
```

is valid.

---

# 8. Sprint 0 — Completed

Sprint 0 was the project foundation.

Completed:

- Workspace created
- Root project structure created
- README created
- LICENSE created
- `.gitignore` created
- Git repository initialized
- GitHub repository created
- Remote configured
- `main` branch created/pushed
- `develop` branch created/pushed
- Feature branch workflow practiced
- `.gitkeep` concept understood
- Engineering journal created
- Sprint notes created
- Sprint 0 documentation merged into `develop`

Created:

```text
docs/engineering-journal.md
docs/sprint-notes/sprint-0.md
```

### Important Git incident that was resolved

The engineering journal was initially forgotten.

The user:

1. Checked out `develop`
2. Pulled
3. Checked out `feature/sprint-0-documentation`
4. Added `docs/engineering-journal.md`
5. Committed:
   `docs: add Sprint 0 engineering journal`
6. Pushed feature branch
7. Checked out `develop`
8. Pulled
9. Merged feature branch
10. Pushed `develop`

This worked correctly.

The feature branch was later deleted locally and remotely.

### Current Sprint 0 state

Sprint 0 is complete.

---

# 9. Git Commands Practiced and Understood

Important commands:

```powershell
git status
git status --short
git branch
git checkout develop
git pull
git checkout -b feature/<name>
git add .
git commit -m "message"
git push -u origin feature/<name>
git merge feature/<name>
git push
git branch -d feature/<name>
git push origin --delete feature/<name>
git remote -v
```

### Important habit

Before committing:

```powershell
git status
```

After staging:

```powershell
git status
git diff --cached --stat
```

Always verify what will be committed.

Do not blindly commit generated build output or IDE files.

---

# 10. Spring Boot Project Generation — Completed

A Spring Boot project was generated and downloaded as a ZIP.

It was extracted under:

```text
F:\Workspace\nexus-platform\backend\employee-service
```

It was then opened/imported successfully in IntelliJ IDEA.

The user initially asked whether the ZIP should be placed under the existing workspace. Decision:

```text
nexus-platform/backend/employee-service
```

is the correct location.

### Import methods discussed

Both are valid:

- Right-click/open the extracted folder as a project
- IntelliJ → Open → select the project folder

The project imported successfully.

---

# 11. Current Spring Boot Project

Current backend:

```text
backend/employee-service/
```

Important generated files/folders:

```text
.idea/
.mvn/
src/
  main/
  test/
HELP.md
mvnw
mvnw.cmd
pom.xml
```

The user has inspected these and understands their broad purpose.

---

# 12. `.idea`

`.idea` contains IntelliJ IDEA project/IDE configuration.

It is **not a Spring Boot framework directory**.

Whether to commit `.idea` depends on the repository `.gitignore` policy. Do not blindly add IDE-specific files without checking the staging output.

---

# 13. `.mvn`

Contains Maven Wrapper configuration.

Important file:

```text
.mvn/maven-wrapper.properties
```

This works with:

```text
mvnw
mvnw.cmd
```

to make Maven execution more reproducible across developer environments.

---

# 14. Maven Wrapper — Correct Understanding

The user initially thought:

> `mvnw` makes the project compatible with Windows/Mac/etc.

Correction:

The Maven Wrapper is **not simply a Windows compatibility mechanism**.

It allows a project to invoke a defined Maven version/distribution without requiring developers to manually install that exact version.

Typical Windows command:

```powershell
.\mvnw.cmd
```

Unix-like command:

```bash
./mvnw
```

This improves build reproducibility.

---

# 15. `src/main/java`

Contains production Java source code.

This is where the actual application Java code lives.

---

# 16. `src/test/java`

Contains test source code.

These tests are used during testing and are not production application code.

---

# 17. `target/`

Generated Maven build output.

It may contain:

```text
target/
├── classes/
├── test-classes/
├── generated-sources/
└── employee-service-0.0.1-SNAPSHOT.jar
```

depending on build phases.

`target/` is generated and should normally be ignored by Git.

It can be recreated by Maven.

Important correction:

Deleting `target/` is not dangerous to the source project. Maven recreates it when required.

---

# 18. Maven Fundamentals Learned

## JDK

JDK provides the tools required to develop Java applications.

Important tool:

```text
javac
```

---

## JVM

JVM provides the runtime environment for Java bytecode.

Simplified flow:

```text
.java
   ↓
javac
   ↓
.class bytecode
   ↓
JVM
   ↓
execution on the current platform
```

Important correction:

Do not say "JVM only understands machine code."

The JVM works with Java bytecode. JVM implementations ultimately execute/interpret/JIT-compile that bytecode into native machine instructions.

---

## javac

`javac` primarily compiles Java source code:

```text
.java → .class
```

It does not provide Maven's dependency management/build lifecycle/package management.

---

# 19. Maven vs javac

Why Maven is needed even though `javac` exists:

`javac` handles Java compilation.

Maven handles the broader project build process, including:

- Dependency management
- Compilation
- Test lifecycle
- Packaging
- Plugins
- Build lifecycle
- Artifact installation
- Project metadata
- Reproducible project configuration

Conceptual flow:

```text
Java source
   ↓
javac
   ↓
.class
   ↓
Maven lifecycle
   ↓
test/package/install
   ↓
JAR
```

---

# 20. `.class` vs `.jar`

`.class`:

- Compiled Java bytecode for a class
- Usually represents one Java class

`.jar`:

- Java archive
- Can contain many `.class` files
- Can contain resources/configuration/metadata
- Packages an application/library for distribution

Example:

```text
employee-service-0.0.1-SNAPSHOT.jar
```

---

# 21. Maven Local Repository

Default:

```text
C:\Users\<username>\.m2\repository
```

For this environment:

```text
C:\Users\Anilbabu\.m2\repository
```

If Maven needs a dependency that is not available locally, it can download it from a configured remote repository and cache/store it locally.

---

# 22. `pom.xml`

POM = Project Object Model.

It is the main Maven project configuration/blueprint.

It describes things such as:

- Project identity
- Parent
- Properties
- Dependencies
- Build plugins
- Metadata
- SCM configuration
- Other Maven configuration

---

# 23. POM `modelVersion`

Current:

```xml
<modelVersion>4.0.0</modelVersion>
```

This identifies the POM model format/schema used by Maven.

It is not the Spring Boot version.

---

# 24. Maven Coordinates

Current:

```text
groupId    = com.nexuslabs
artifactId = employee-service
version    = 0.0.1-SNAPSHOT
```

## groupId

Represents the organization/namespace/project ownership.

A group can own multiple artifacts.

Example concept:

```text
com.nexuslabs:employee-service
com.nexuslabs:auth-service
com.nexuslabs:notification-service
```

---

## artifactId

Identifies a particular artifact/project/service within the group.

---

## version

Identifies the version of the artifact.

---

# 25. SNAPSHOT

Current:

```text
0.0.1-SNAPSHOT
```

`SNAPSHOT` indicates an in-development/non-final version.

A release version might be:

```text
0.0.1
```

Important: SNAPSHOT is not merely "the version is changing"; Maven has specific snapshot semantics around retrieving updated development artifacts.

---

# 26. JAR Naming

Given:

```text
artifactId = employee-service
version = 0.0.1-SNAPSHOT
```

expected packaged artifact:

```text
employee-service-0.0.1-SNAPSHOT.jar
```

General pattern:

```text
artifactId-version.jar
```

---

# 27. `target` vs `.m2`

Important distinction:

After:

```powershell
mvn clean install
```

the JAR is produced under:

```text
target/
```

and `install` additionally installs the artifact into the local Maven repository:

```text
C:\Users\Anilbabu\.m2\repository
```

So:

```text
target/
    = project build output

.m2/repository/
    = local Maven artifact/dependency repository
```

---

# 28. Maven Lifecycle Understanding

Basic lifecycle learned:

```text
clean
```

removes previous build output.

```text
compile
```

compiles production source.

```text
test
```

runs tests.

```text
package
```

packages the application, e.g. JAR.

```text
install
```

installs the packaged artifact into the local Maven repository.

Therefore:

```text
mvn clean install
```

roughly means:

```text
remove previous build output
        ↓
compile
        ↓
test
        ↓
package
        ↓
install artifact into local .m2
```

Do not oversimplify this as "javac + jar"; Maven has a complete lifecycle and plugins execute the phases.

---

# 29. Maven Dependencies

Dependencies are declared in:

```xml
<dependencies>
    ...
</dependencies>
```

Maven uses dependency declarations and dependency management to determine what artifacts/versions are needed.

Example:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

---

# 30. Maven Dependency Sections

Basic POM sections studied:

```xml
<dependencies>
```

= project libraries required by the application.

```xml
<plugins>
```

= Maven build plugins that extend/control build behavior.

```xml
<properties>
```

= reusable project configuration values.

---

# 31. Parent POM

Current:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
</parent>
```

The Spring Boot parent provides inherited Maven configuration and dependency/version management.

Important correction:

The parent does **not** simply mean "it knows PostgreSQL."

It provides managed versions and Maven configuration for a broad set of Spring Boot dependencies.

---

# 32. Why the Parent Matters

If the Spring Boot parent/dependency management is removed, the project may need explicit dependency/plugin versions and other configuration that was previously inherited.

This is why the generated POM can omit versions for many Spring Boot-managed dependencies.

Example:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

does not currently specify a version because dependency management supplies the appropriate version.

---

# 33. Maven Dependency Management

Important conceptual rule:

The dependency declaration says what the project needs.

Dependency management can determine the version used for managed dependencies.

The project does not necessarily need to hard-code every dependency version.

---

# 34. Transitive Dependencies

This is an important next topic.

A dependency may itself depend on other libraries.

Example concept:

```text
spring-boot-starter-data-jpa
        ↓
Spring Data JPA
        ↓
Hibernate
        ↓
Jakarta Persistence / other libraries
```

Maven resolves the dependency graph and downloads required transitive dependencies.

The user's understanding here is correct:

> A starter can have internal/transitive dependencies, so Maven downloads those required dependencies too.

---

# 35. Dependency Scopes

Basic understanding:

## `test`

```xml
<scope>test</scope>
```

Used for test-related dependencies.

## `runtime`

```xml
<scope>runtime</scope>
```

Required when the application runs but generally not required to compile the application's source code directly.

The PostgreSQL driver is a common example.

Important correction:

`runtime` should **not** be described as "required during build time for building the JAR." Its main meaning is runtime availability/classpath behavior.

---

# 36. Current `pom.xml`

Current generated POM is approximately:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.nexuslabs</groupId>
    <artifactId>employee-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name/>
    <description/>
    <url/>

    <licenses>
        <license/>
    </licenses>

    <developers>
        <developer/>
    </developers>

    <scm>
        <connection/>
        <developerConnection/>
        <tag/>
        <url/>
    </scm>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

# 37. Current Spring Boot Dependencies — What We Know

## `spring-boot-starter-webmvc`

Used for Spring MVC/web functionality and HTTP APIs.

It will eventually be used to build REST endpoints.

## `spring-boot-starter-data-jpa`

Provides Spring Data JPA and brings the persistence stack, including Hibernate and related dependencies through the dependency graph.

Conceptual flow:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Spring Data JPA
    ↓
JPA
    ↓
Hibernate
    ↓
JDBC
    ↓
PostgreSQL Driver
    ↓
PostgreSQL
```

## `spring-boot-starter-validation`

Provides Bean Validation support for validating incoming data.

## `postgresql`

Provides the PostgreSQL JDBC driver.

## `spring-boot-devtools`

Development-time tooling.

Current configuration:

```xml
<scope>runtime</scope>
<optional>true</optional>
```

## `*-test`

The generated test starters are scoped to:

```text
test
```

and are intended for test support.

---

# 38. Package Naming Issue

The generated README reported:

> The original package name `com.nexuslabs.employee-service` is invalid and this project uses `com.nexuslabs.employee_service` instead.

Reason:

Java package identifiers cannot contain `-`.

Invalid:

```java
package com.nexuslabs.employee-service;
```

Valid syntactically:

```java
package com.nexuslabs.employee_service;
```

However, conventional Java package naming usually favors lowercase names without underscores.

Potential future package:

```text
com.nexuslabs.employeeservice
```

has **not** yet been adopted.

Do not silently change it; discuss the package naming decision first.

---

# 39. Spring Boot Version Correction

The original plan said:

> Generate a Spring Boot 3 project.

However, the actual generated project currently has:

```text
Spring Boot 4.1.0
```

This is the actual project state.

Do not tell the user the project is Spring Boot 3.

Before changing versions, assess compatibility and whether the learning/project goals require it.

---

# 40. What Has NOT Yet Been Fully Studied

These are future modules:

- Spring Boot startup internals
- `@SpringBootApplication`
- Component scanning
- Auto-configuration
- IoC
- Dependency Injection
- Beans
- Configuration
- Spring MVC internals
- Controllers
- REST API implementation
- HTTP request lifecycle
- DTOs
- Validation implementation
- Exception handling
- Service layer
- Repository layer
- JPA implementation
- Hibernate mappings
- Entities
- Relationships
- Transactions
- PostgreSQL integration
- Spring Security
- Authentication
- Authorization
- JWT
- Kafka
- Event-driven architecture
- Microservices
- Dockerizing services
- Docker Compose
- Testing in depth
- Observability/logging
- Production configuration
- CI/CD

---

# 41. Learning Checkpoints Already Used

The teaching style includes short conceptual questions.

Examples already answered/corrected:

### JVM/JDK/javac

User answer:

> JDK provides development tools; JVM provides runtime; javac compiles `.java` to `.class`.

This is broadly correct.

Correction:

JVM executes bytecode; it is not accurate to say JVM only accepts machine code.

---

### Maven

User answer:

> javac only compiles source; Maven handles dependencies and other build activities.

Correct.

---

### POM

User answer:

> pom.xml is the blueprint of the Maven project and tells about project identity, dependencies, etc.

Correct.

---

### `.m2`

User correctly identified:

```text
C:\Users\<username>\.m2\repository
```

as the Maven local repository.

---

### Missing dependency

User correctly understood:

If a required dependency is not in `.m2`, Maven can retrieve it from the configured remote repository and store it locally.

---

### Maven Wrapper

User initially had no clear answer.

Correct understanding now:

The Maven Wrapper provides a project-controlled/reproducible Maven execution mechanism and is not merely for Windows compatibility.

---

### Maven coordinates

User understands:

```text
groupId
artifactId
version
```

and that the same group can contain multiple artifacts.

---

### SNAPSHOT

User understands that `SNAPSHOT` indicates an in-development version rather than a normal release version.

---

### Dependency management

User understands:

> Maven uses dependency declarations and dependency management/POM information to determine required versions.

---

### Transitive dependencies

User's latest understanding:

> A dependency such as `data-jpa` can bring additional dependencies, and Maven downloads those transitive dependencies.

This is correct.

---

# 42. Current Learning Position

The project is currently at the transition from Maven fundamentals into Spring Boot dependency/project understanding.

### Completed

- Environment setup
- Workspace setup
- Git/GitHub setup
- Branch strategy
- Sprint 0
- Spring Boot project generation/import
- Maven fundamentals
- Maven coordinates
- POM structure basics
- Dependency basics
- Dependency scopes basics
- Transitive dependency concept

### Immediate next module

Continue with:

# Module 1.5 — Maven/Spring Boot Dependency Deep Dive

Recommended order:

1. Spring Boot starters
2. Transitive dependencies
3. `mvn dependency:tree`
4. Read the actual dependency graph of `employee-service`
5. Why starters exist
6. Dependency management
7. Parent POM
8. Plugin management
9. Spring Boot Maven plugin
10. Build the project with Maven
11. Inspect `target/`
12. Run the generated application
13. Understand startup output
14. Make the first meaningful backend code change
15. Commit the project

Do not jump into controllers yet until the dependency/build/startup foundation is clear enough.

---

# 43. Important Project Sequence

The preferred learning sequence is:

```text
Java fundamentals
    ↓
Java 8 concepts
    ↓
Modern Java 17/21
    ↓
Maven
    ↓
Spring Boot project structure
    ↓
Spring Boot startup
    ↓
IoC / DI / Beans
    ↓
Spring MVC
    ↓
REST APIs
    ↓
DTO / Validation / Exceptions
    ↓
Service Layer
    ↓
JPA
    ↓
Hibernate
    ↓
PostgreSQL
    ↓
Transactions
    ↓
Authentication / Authorization
    ↓
Kafka
    ↓
Event-driven architecture
    ↓
Microservices
    ↓
Docker
    ↓
Testing / Production practices
```

---

# 44. Architecture Direction

Do NOT start with many microservices.

Start with a strong backend service/module:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
JPA/Hibernate
  ↓
PostgreSQL
```

Then introduce Kafka and microservices when the user understands the above stack.

Later architecture can evolve toward:

```text
                    API Gateway
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
        Employee      Auth        Other
        Service       Service     Services
             |           |
             +-----+-----+
                   |
                 Kafka
                   |
             Event Consumers
```

The exact service boundaries should be decided from actual business requirements, not invented prematurely.

---

# 45. Git Status / Current Uncommitted Work

At the time this context was requested, the user reported:

```text
PS F:\Workspace\nexus-platform> git status

On branch develop
Your branch is up to date with 'origin/develop'.

Untracked files:
    backend/employee-service/
    docs/project-context.md

nothing added to commit but untracked files present
```

Interpretation:

- `backend/employee-service/` exists locally but is not yet committed.
- `docs/project-context.md` is newly created and not yet committed.
- This is expected at this checkpoint.

### Recommended safe next action

Do not commit directly to `develop`.

Use:

```powershell
git pull
git checkout -b feature/sprint-1-project-foundation
git status
git add .
git status
git diff --cached --stat
```

Then inspect the staged files before committing.

Potential commit:

```powershell
git commit -m "feat: add employee service foundation"
```

Then:

```powershell
git push -u origin feature/sprint-1-project-foundation
```

Merge into `develop` only after verifying the staged/committed content.

---

# 46. Files That Should Normally NOT Be Committed

Check `.gitignore` before committing.

Generated build output should normally be ignored:

```text
target/
```

IDE-specific/generated files should be handled according to the repository's `.gitignore`.

Do not commit:

- `target/`
- local Maven build output
- machine-specific secrets
- local credentials
- unnecessary IDE metadata
- generated temporary files

Do commit:

- source code
- `pom.xml`
- Maven Wrapper files required by the project
- project documentation
- configuration templates
- tests
- GitHub/project documentation

---

# 47. Database Direction

PostgreSQL `nexus` already exists.

Do not immediately create a large schema manually.

Database design should be introduced when the JPA/Hibernate module begins.

Eventually learn:

- tables
- primary keys
- foreign keys
- indexes
- constraints
- migrations
- entity mapping
- relationships
- transactions
- query optimization

Database migrations should eventually be considered (e.g. Flyway/Liquibase) as part of production-oriented development.

---

# 48. Docker Direction

Docker Desktop is already installed and working.

Do not containerize everything immediately.

Introduce Docker after the local application/database flow is understood.

Eventually use Docker Compose for infrastructure such as:

- PostgreSQL
- Kafka
- supporting services

and later containerize the application services.

---

# 49. Startup Project Context

There was a separate startup/project idea discussed previously.

Important:

The startup project has **not been cancelled**.

The user explicitly said they currently cannot focus on it because of other priorities and needs time.

This Nexus Platform project is a separate current upskilling effort.

Do not mix the startup project into Nexus Platform unless the user explicitly asks.

---

# 50. Long-Term Skill Showcase Goal

The user wants the final project to demonstrate practical skills in:

- Spring Boot
- Hibernate
- Spring MVC
- Kafka
- Authentication
- Authorization
- Microservices
- RESTful APIs

The goal is to be able to discuss these technologies credibly in interviews by explaining actual implementation decisions and trade-offs, not simply listing them on a resume.

---

# 51. Project Documentation Strategy

The repository should gradually contain:

```text
docs/
├── architecture/
├── decisions/
├── sprint-notes/
├── engineering-journal.md
└── project-context.md
```

Use:

- `engineering-journal.md` for learning/engineering progress.
- `sprint-notes/` for sprint-specific objectives and results.
- `architecture/` for system architecture documentation.
- `decisions/` for Architecture Decision Records (ADRs).
- `project-context.md` for persistent cross-chat project state.

### Critical rule

`project-context.md` should be updated after major sessions rather than waiting until the end.

---

# 52. How a New ChatGPT Session Should Continue

When opening a new chat, the user can say:

> I am continuing the Nexus Platform backend project. Please read `docs/project-context.md` from the repository/file I provide. Treat it as the authoritative project and learning state. Do not restart from the beginning. Continue from the exact current learning position and preserve the teaching approach.

If the new ChatGPT session cannot directly access the GitHub repository/file, the user should upload or paste the current `project-context.md`.

The new session must:

1. Read the context.
2. Identify completed work.
3. Identify current Git/project state.
4. Identify current learning module.
5. Avoid reteaching completed modules unless the user requests a refresher.
6. Continue from the documented next topic.

---

# 53. Context Maintenance Rule

Whenever a major session ends, update this file with:

- What was learned
- What was implemented
- What was corrected
- What files changed
- Git branch
- Commit/merge state
- Current project structure
- New decisions
- New dependencies
- Current module
- Next exercise
- Any user corrections about their knowledge/preferences

The chat response alone is NOT considered the persistent source of truth.

---

# 54. Current Checkpoint — 2026-08-16

## Environment

COMPLETE

- Java 21
- Maven 3.9.11
- Git 2.37.0
- IntelliJ IDEA
- Docker Desktop 29.6.2
- Docker Compose 5.3.1
- PostgreSQL 17.10
- PostgreSQL database `nexus`

## Git foundation

COMPLETE

- GitHub repository
- main
- develop
- feature workflow
- Sprint 0
- engineering journal
- sprint notes

## Spring Boot

COMPLETE

- Project generated
- Project extracted
- Project placed under `backend/employee-service`
- Imported into IntelliJ
- `pom.xml` inspected
- Generated structure inspected

## Maven

COMPLETE/UNDERSTOOD AT BASIC LEVEL

- JDK/JVM/javac
- `.class`
- `.jar`
- Maven purpose
- POM
- `.m2`
- Maven Wrapper
- lifecycle basics
- `clean`
- `package`
- `install`
- `clean install`
- coordinates
- SNAPSHOT
- dependencies
- dependency scopes
- transitive dependency concept

## Current learning module

**Maven/Spring Boot dependency deep dive**

## Immediate next lesson

**Spring Boot starters → dependency tree → transitive dependencies → dependency management → build/run the application**

## Current Git state

Before the context document is committed:

```text
develop
├── untracked: backend/employee-service/
└── untracked: docs/project-context.md
```

These should be handled using a feature branch and verified staging before commit.

---

# END OF PERSISTENT PROJECT CONTEXT
