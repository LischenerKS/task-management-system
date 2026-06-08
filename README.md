# Task Management System

REST API для управления задачами. Java 21 + Spring Boot 4 + PostgreSQL.

## Стек

- **Java 21**, Spring Boot 4.0.6
- **Spring Data JPA** + Hibernate, PostgreSQL
- **Jakarta Validation**, SLF4J
- Сборка: Gradle 9.5.1 (Kotlin DSL)

## Запуск

### Переменные окружения

```
DB_URL=jdbc:postgresql://localhost:5432/task_management
DB_USER=postgres
DB_PASSWORD=secret
```

### Локально

```bash
# Создать БД
psql -U postgres -c "CREATE DATABASE task_management;"

# Запустить
./gradlew bootRun
```

Схема создаётся автоматически при первом запуске (`ddl-auto=update`).

```bash
# Собрать JAR
./gradlew build
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

Сервер: `http://localhost:8080`

## Жизненный цикл задачи

```
CREATED ──[start]──► IN_PROGRESS ──[complete]──► DONE
                          ▲                        │
                          └────────────────────────┘
                              (только через PUT)
```

Бизнес-правила:

- При создании `id` и `status` не передаются — выставляются автоматически
- `start`: `assignedUserId` обязателен; у пользователя не может быть больше 5 задач `IN_PROGRESS`
- `complete`: `assignedUserId` и `deadlineDate` обязательны
- Задачу со статусом `DONE` через `PUT` можно перевести только обратно в `IN_PROGRESS`
- `deadlineDate` должен быть в будущем; `creatorId` и `priority` — обязательны

## API

### GET /tasks

Список задач с фильтрацией и пагинацией.

Query-параметры (все опциональны):

| Параметр | Тип | По умолчанию |
|---|---|---|
| `creatorId` | Long | — |
| `assignedUserId` | Long | — |
| `status` | CREATED / IN_PROGRESS / DONE | — |
| `priority` | LOW / MEDIUM / HIGH | — |
| `pageSize` | Integer | 10 |
| `pageNumber` | Integer | 0 |

```http
GET /tasks?status=IN_PROGRESS&priority=HIGH&pageSize=5
```

### GET /tasks/{id}

### POST /tasks

```json
{
  "creatorId": 42,
  "assignedUserId": 7,
  "deadlineDate": "2026-12-31T23:59:59",
  "priority": "MEDIUM"
}
```

Возвращает `201 Created`.

### PUT /tasks/{id}

Тело — как у POST, без `id`. Возвращает `200 OK`.

### DELETE /tasks/{id}

Возвращает `204 No Content`.

### POST /tasks/{id}/start

Переводит задачу в `IN_PROGRESS`. Возвращает `200 OK`.

### POST /tasks/{id}/complete

Переводит задачу в `DONE`, проставляет `doneDateTime`. Возвращает `200 OK`.

## Структура проекта

```
src/main/java/io/github/lischenerks/taskmanagement/
├── TaskManagementSystemApplication.java
├── Task.java                     # DTO (record)
├── TaskStatus.java               # CREATED | IN_PROGRESS | DONE
├── TaskPriority.java             # LOW | MEDIUM | HIGH
├── TaskMapper.java               # Task <-> TaskEntity
├── controller/
│   └── TaskController.java
├── service/
│   ├── TaskService.java
│   └── TaskSearchFilter.java     # record для параметров фильтра
├── repository/
│   ├── TaskEntity.java
│   └── TaskRepository.java       # JPA + JPQL-запрос с фильтрами
└── exceptions/
    ├── GlobalExceptionHandler.java
    └── ErrorResponseDto.java
```

## Ошибки

Все ошибки возвращаются в одном формате:

```json
{
    "message": "Bad request",
    "detailedMessage": "can not complete task without filled field assignedUserId",
    "errorTime": "2026-06-08T22:11:42.580948592"
}
```

| Код | Когда |
|---|---|
| 400 | Нарушение бизнес-правил или невалидные данные |
| 404 | Задача не найдена |
| 500 | Непредвиденная ошибка |
