# Task Management System

[Русская версия](README.md)

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Coverage](.github/badges/jacoco.svg)

A REST API for a task tracker: creating tasks, assigning them to users, tracking deadlines and priorities, and moving tasks through their lifecycle.

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Quick start](#quick-start)
- [API docs (Swagger)](#api-docs-swagger)
- [Task lifecycle](#task-lifecycle)
- [Business rules](#business-rules)
- [API endpoints](#api-endpoints)
- [Error format](#error-format)
- [Project structure](#project-structure)
- [Testing](#testing)
- [License](#license)

## Features

- CRUD operations on tasks
- Filtering by creator, assignee, status and priority, with pagination and sorting
- Dedicated `start` and `complete` endpoints for moving a task through its lifecycle
- Domain model and DTOs kept separate, with explicit mapping between them
- Input validation and a single, consistent error response format
- Pessimistic locking on the assignee's rows to prevent race conditions when starting tasks
- Optimistic locking at the entity level, returns `409` on a version conflict
- Versioned database schema via Flyway
- Auto-generated OpenAPI documentation and Swagger UI
- Docker image and `docker-compose.yml` for a one-command startup
- Integration tests against a real PostgreSQL instance via Testcontainers
- Jacoco coverage reports

## Tech stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 (Web, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 16 |
| ORM / migrations | Hibernate / Spring Data JPA, Flyway |
| API documentation | springdoc-openapi 3.0.3 (Swagger UI) |
| Build | Gradle 9.5.1 (Kotlin DSL) |
| Testing | JUnit 5, Mockito, Testcontainers, Jacoco |
| Code formatting | Spotless (Eclipse formatter) |
| CI | GitHub Actions |
| Containerization | Docker (multi-stage build), Docker Compose |

## Quick start

### With Docker Compose

```bash
git clone https://github.com/LischenerKS/task-management-system.git
cd task-management-system
docker compose up --build
```

The app is available at `http://localhost:8080`, PostgreSQL listens on port `5432`.

### Running locally

**Requirements:** JDK 21, PostgreSQL 16. The Gradle wrapper (`gradlew`) is included, no separate install needed.

1. Create the database:

```bash
psql -U postgres -c "CREATE DATABASE task_management;"
```

2. Set environment variables:

```
DB_URL=jdbc:postgresql://localhost:5432/task_management
DB_USER=postgres
DB_PASSWORD=secret
```

3. Run the application:

```bash
./gradlew bootRun
```

The schema is created and versioned automatically on startup via Flyway migrations (`src/main/resources/db/migration`).

Or build and run the JAR directly:

```bash
./gradlew build
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

The server starts on `http://localhost:8080`.

## API docs (Swagger)

```
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI JSON spec is available at `http://localhost:8080/v3/api-docs`.

## Task lifecycle

```
CREATED ──[start]──► IN_PROGRESS ──[complete]──► DONE
```

Status changes only through the `start` and `complete` endpoints. `PUT /tasks/{id}` does not affect status.

## Business rules

- Status is always set by the server: `CREATED` on creation, `IN_PROGRESS`/`DONE` via `start`/`complete`. It can't be set directly through create or update
- Creating a task (`POST /tasks`) requires `creatorId`, `assignedUserId`, `deadlineDate` (must be in the future) and `priority`
- Updating a task (`PUT /tasks/{id}`) requires `assignedUserId`, `deadlineDate` (also future-only) and `priority`. The creator can't be changed after creation
- Starting a task (`POST /tasks/{id}/start`):
  - requires `assignedUserId` to be set
  - fails if the task is already `IN_PROGRESS`
  - a single assignee can't have more than **5** tasks `IN_PROGRESS` at once; this is enforced under a pessimistic lock on the assignee's rows to prevent a race on concurrent `start` calls
- Completing a task (`POST /tasks/{id}/complete`) requires `assignedUserId` and `deadlineDate` to be set; `doneDateTime` is set to the current time automatically
- Concurrent updates to the same task are protected by optimistic locking (`version` field); a version conflict returns `409 Conflict`

## API endpoints

### `GET /tasks`

List tasks with filtering, pagination and sorting.

Query parameters (all optional):

| Parameter | Type | Default |
|---|---|---|
| `creatorId` | Long | not set |
| `assignedUserId` | Long | not set |
| `status` | `CREATED` / `IN_PROGRESS` / `DONE` | not set |
| `priority` | `LOW` / `MEDIUM` / `HIGH` | not set |
| `page` | Integer | 0 |
| `size` | Integer | 10 |
| `sort` | string | `id` |

```bash
curl "http://localhost:8080/tasks?status=IN_PROGRESS&priority=HIGH&size=5"
```

The response is wrapped in the standard Spring Data `Page` object: `content`, `totalElements`, `totalPages`, `number`, `size` and other metadata fields.

### `GET /tasks/{id}`

Get a task by id. Returns `404` if not found.

```bash
curl http://localhost:8080/tasks/1
```

### `POST /tasks`

Create a new task.

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "creatorId": 42,
    "assignedUserId": 7,
    "deadlineDate": "2026-12-31T23:59:59",
    "priority": "MEDIUM"
  }'
```

Returns `201 Created` with the created task in `CREATED` status.

### `PUT /tasks/{id}`

Update the assignee, deadline and priority of a task. Status and creator are not affected.

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "assignedUserId": 7,
    "deadlineDate": "2026-12-31T23:59:59",
    "priority": "HIGH"
  }'
```

Returns `200 OK`, or `409 Conflict` on a version conflict.

### `DELETE /tasks/{id}`

Delete a task. Returns `204 No Content`.

```bash
curl -X DELETE http://localhost:8080/tasks/1
```

### `POST /tasks/{id}/start`

Move a task to `IN_PROGRESS`. Returns `200 OK`.

```bash
curl -X POST http://localhost:8080/tasks/1/start
```

### `POST /tasks/{id}/complete`

Complete a task: move it to `DONE` and set `doneDateTime`. Returns `200 OK`.

```bash
curl -X POST http://localhost:8080/tasks/1/complete
```

## Error format

All errors are returned in a single format:

```json
{
  "message": "Entity not found",
  "detailedMessage": "Not found task with id = 99",
  "errorTime": "2026-06-08T14:30:00"
}
```

| Code | When |
|---|---|
| `400` | Business rule violation or invalid input |
| `404` | Task with the given `id` was not found |
| `409` | Version conflict on a concurrent task update (optimistic locking) |
| `500` | Unexpected server-side error |

## Project structure

```
src/main/java/io/github/lischenerks/taskmanagement/
├── TaskManagementSystemApplication.java   # entry point
├── domain/
│   ├── Task.java                          # domain model (record)
│   ├── TaskStatus.java                    # CREATED | IN_PROGRESS | DONE
│   └── TaskPriority.java                  # LOW | MEDIUM | HIGH
├── dto/
│   ├── CreateTaskDto.java                 # POST /tasks request body
│   ├── UpdateTaskDto.java                 # PUT /tasks/{id} request body
│   ├── TaskResponseDto.java               # response body
│   └── ErrorResponseDto.java              # single error format
├── mapper/
│   └── TaskMapper.java                    # Task <-> TaskEntity <-> DTO
├── controller/
│   └── TaskController.java
├── service/
│   ├── TaskService.java
│   └── TaskSearchFilter.java              # record for filter parameters
├── repository/
│   ├── TaskEntity.java                    # @Version, optimistic locking
│   └── TaskRepository.java                # JPQL filters, pessimistic lock
└── exceptions/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.properties
└── db/migration/
    └── V1__init_task_table.sql            # Flyway
```

## Testing

```bash
./gradlew test
```

- `TaskServiceTest`: service layer unit tests (Mockito)
- `TaskServiceConcurrencyTest`: parallel `startTask` calls for the same assignee check the 5-active-tasks limit, a parallel `updateTask` on the same task checks optimistic locking
- `TaskRepositoryTest`: `@DataJpaTest`, repository JPQL filters
- `TaskControllerTest`: controller tests (MockMvc), including the `409` response on a version conflict
- `TaskManagementSystemApplicationTests`: application context loaded against a real PostgreSQL instance via Testcontainers (Docker must be available locally/in CI)

Coverage reports are generated by Jacoco (`build/reports/jacoco/test/html`) and the README badge is updated automatically on push to `master`/`dev`.

## License

Distributed under the [Apache License 2.0](LICENSE).