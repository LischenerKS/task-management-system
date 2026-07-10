# Task Management System

[Русская версия](README.md)

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![Coverage](.github/badges/jacoco.svg)

A REST API for task management (task tracker): creating tasks, assigning
them to users, tracking deadlines and priorities, and moving tasks through
their lifecycle. Built as a demo/pet project with a focus on
production-grade practices: versioned DB migrations, concurrency handling,
integration tests against a real database, CI.

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

## Features

- CRUD operations on tasks
- Filtering the task list by creator, assignee, status and priority, with pagination
- Task lifecycle management via dedicated `start` / `complete` endpoints
- Input validation (Jakarta Validation) and a single, consistent error response format
- Protection against race conditions when starting tasks concurrently for
  the same assignee (pessimistic locking) and optimistic locking at the
  entity level (`@Version`)
- Versioned database schema (Flyway)
- Auto-generated OpenAPI documentation (springdoc) and Swagger UI
- Docker image and `docker-compose.yml` for a one-command startup
- Integration tests against a real PostgreSQL instance via Testcontainers, with Jacoco coverage reports

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

The easiest way to run the application together with the database:

```bash
git clone https://github.com/LischenerKS/task-management-system.git
cd task-management-system
docker compose up --build
```

The app will be available at `http://localhost:8080`, PostgreSQL on port `5432`.

### Running locally

**Requirements:** JDK 21, PostgreSQL 16. No need to install Gradle separately — the wrapper (`gradlew`) is included.

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

The database schema is created and versioned automatically on startup via
Flyway migrations (`src/main/resources/db/migration`).

Or build and run the JAR file directly:

```bash
./gradlew build
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

The server starts on `http://localhost:8080`.

## API docs (Swagger)

Once the application is running, interactive documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI JSON specification is available at `http://localhost:8080/v3/api-docs`.

## Task lifecycle

```
CREATED ──[start]──► IN_PROGRESS ──[complete]──► DONE
                          ▲                        │
                          └────────────────────────┘
                              (only via PUT)
```

## Business rules

The rules below are taken directly from `TaskService` and the `Task` DTO validation:

- `id` and `status` must not be sent when creating a task — the server
  always sets `status = CREATED` (a request with a non-null `id`/`status` is rejected with `400`)
- `deadlineDate` is required and must be in the future (`@Future`);
  `creatorId` and `priority` are required on both create and update
- `POST /tasks/{id}/start`:
    - the task must have `assignedUserId` set;
    - starting a task that's already `IN_PROGRESS` is rejected;
    - a single assignee (`assignedUserId`) cannot have more than **5** tasks
      `IN_PROGRESS` at the same time — the limit is checked under a
      pessimistic lock on the assignee's rows, which rules out a race
      condition on concurrent `start` calls
- `POST /tasks/{id}/complete`:
    - `assignedUserId` and `deadlineDate` are required;
    - the server automatically sets `doneDateTime = now()` on completion
- `PUT /tasks/{id}`: a task in `DONE` status can only be moved back to
  `IN_PROGRESS` — any other change to a `DONE` task is rejected
- Concurrent updates to the same task are protected by optimistic locking
  (the `version` field on `TaskEntity`)

## API endpoints

### `GET /tasks`

List tasks with filtering and pagination.

Query parameters (all optional):

| Parameter | Type | Default |
|---|---|---|
| `creatorId` | Long | — |
| `assignedUserId` | Long | — |
| `status` | `CREATED` / `IN_PROGRESS` / `DONE` | — |
| `priority` | `LOW` / `MEDIUM` / `HIGH` | — |
| `pageSize` | Integer | 10 |
| `pageNumber` | Integer | 0 |

```bash
curl "http://localhost:8080/tasks?status=IN_PROGRESS&priority=HIGH&pageSize=5"
```

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

Returns `201 Created` with the created task.

### `PUT /tasks/{id}`

Update a task. Request body is the same shape as `POST`, without `id`. Returns `200 OK`.

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "creatorId": 42,
    "assignedUserId": 7,
    "status": "IN_PROGRESS",
    "deadlineDate": "2026-12-31T23:59:59",
    "priority": "HIGH"
  }'
```

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
| `500` | Unexpected server-side error |

## Project structure

```
src/main/java/io/github/lischenerks/taskmanagement/
├── TaskManagementSystemApplication.java   # entry point
├── Task.java                              # DTO (record)
├── TaskStatus.java                        # CREATED | IN_PROGRESS | DONE
├── TaskPriority.java                      # LOW | MEDIUM | HIGH
├── TaskMapper.java                        # Task <-> TaskEntity
├── controller/
│   └── TaskController.java
├── service/
│   ├── TaskService.java
│   └── TaskSearchFilter.java              # record for filter parameters
├── repository/
│   ├── TaskEntity.java                    # @Version — optimistic locking
│   └── TaskRepository.java                # JPQL filters + pessimistic lock
└── exceptions/
    ├── GlobalExceptionHandler.java
    └── ErrorResponseDto.java

src/main/resources/
├── application.properties
└── db/migration/
    └── V1__init_task_table.sql            # Flyway
```

## Testing

```bash
./gradlew test
```

- `TaskServiceTest` — service layer unit tests (Mockito)
- `TaskServiceConcurrencyTest` — concurrency test: N parallel `startTask`
  calls for the same assignee, verifying the 5-active-tasks limit holds
- `TaskRepositoryTest` — `@DataJpaTest`, repository JPQL filters
- `TaskControllerTest` — controller tests (MockMvc)
- `TaskManagementSystemApplicationTests` — the application context is
  loaded against a real PostgreSQL instance via Testcontainers (Docker
  must be available locally/in CI)

Coverage reports are generated by Jacoco
(`build/reports/jacoco/test/html`) and the README badge is updated
automatically on push to `master`/`dev`.

