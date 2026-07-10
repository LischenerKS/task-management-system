# Task Management System

[English version](README.en.md)

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![Coverage](.github/badges/jacoco.svg)

REST API для управления задачами (task tracker): создание, назначение
исполнителей, контроль дедлайнов и приоритетов, переходы по жизненному
циклу задачи. Проект написан как demo/pet-проект с акцентом на
production-практики: миграции БД, конкурентный доступ, тесты на реальной
СУБД, CI.

## Содержание

- [Возможности](#возможности)
- [Технологический стек](#технологический-стек)
- [Быстрый старт](#быстрый-старт)
- [Документация API (Swagger)](#документация-api-swagger)
- [Жизненный цикл задачи](#жизненный-цикл-задачи)
- [Бизнес-правила](#бизнес-правила)
- [Эндпоинты API](#эндпоинты-api)
- [Формат ошибок](#формат-ошибок)
- [Структура проекта](#структура-проекта)
- [Тестирование](#тестирование)

## Возможности

- CRUD-операции над задачами
- Фильтрация списка задач по автору, исполнителю, статусу и приоритету + пагинация
- Управление жизненным циклом задачи через отдельные эндпоинты `start` / `complete`
- Валидация входных данных (Jakarta Validation) и единый формат ответа об ошибке
- Защита от гонок при параллельном старте задач одним исполнителем
  (пессимистичная блокировка) и optimistic locking на уровне сущности (`@Version`)
- Версионируемая схема БД (Flyway)
- Автогенерируемая OpenAPI-документация (springdoc) и Swagger UI
- Docker-образ и `docker-compose.yml` для запуска в один шаг
- Интеграционные тесты на реальном PostgreSQL через Testcontainers, отчёт покрытия Jacoco

## Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 4.0.6 (Web, Data JPA, Validation, Actuator) |
| База данных | PostgreSQL 16 |
| ORM / миграции | Hibernate / Spring Data JPA, Flyway |
| Документация API | springdoc-openapi 3.0.3 (Swagger UI) |
| Сборка | Gradle 9.5.1 (Kotlin DSL) |
| Тесты | JUnit 5, Mockito, Testcontainers, Jacoco |
| Форматирование кода | Spotless (Eclipse formatter) |
| CI | GitHub Actions |
| Контейнеризация | Docker (multi-stage), Docker Compose |

## Быстрый старт

### Через Docker Compose

Самый простой способ поднять приложение вместе с базой данных:

```bash
git clone https://github.com/LischenerKS/task-management-system.git
cd task-management-system
docker compose up --build
```

Приложение будет доступно на `http://localhost:8080`, PostgreSQL — на порту `5432`.

### Локальный запуск

**Требования:** JDK 21, PostgreSQL 16. Gradle отдельно ставить не нужно — используется wrapper (`gradlew`).

1. Создать базу данных:

```bash
psql -U postgres -c "CREATE DATABASE task_management;"
```

2. Задать переменные окружения:

```
DB_URL=jdbc:postgresql://localhost:5432/task_management
DB_USER=postgres
DB_PASSWORD=secret
```

3. Запустить приложение:

```bash
./gradlew bootRun
```

Схема базы данных создаётся и версионируется автоматически при старте
через Flyway-миграции (`src/main/resources/db/migration`).

Либо собрать и запустить JAR-файл:

```bash
./gradlew build
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

Сервер стартует на `http://localhost:8080`.

## Документация API (Swagger)

После запуска приложения интерактивная документация доступна по адресу:

```
http://localhost:8080/swagger-ui/index.html
```

Спецификация OpenAPI в формате JSON — по адресу `http://localhost:8080/v3/api-docs`.

## Жизненный цикл задачи

```
CREATED ──[start]──► IN_PROGRESS ──[complete]──► DONE
                          ▲                        │
                          └────────────────────────┘
                              (только через PUT)
```

## Бизнес-правила

Правила ниже взяты непосредственно из `TaskService` и валидации DTO `Task`:

- При создании задачи поля `id` и `status` передавать нельзя — сервер сам
  выставляет `status = CREATED` (запрос с ненулевым `id`/`status` отклоняется `400`)
- `deadlineDate` обязателен и должен быть в будущем (`@Future`); `creatorId`
  и `priority` обязательны при создании и обновлении
- `POST /tasks/{id}/start`:
  - у задачи должен быть заполнен `assignedUserId`;
  - повторный `start` уже стартовавшей задачи (`status = IN_PROGRESS`) запрещён;
  - у одного исполнителя (`assignedUserId`) не может быть больше **5** задач
    одновременно в статусе `IN_PROGRESS` — лимит проверяется под
    пессимистичной блокировкой строк исполнителя, что исключает гонку при
    параллельных запросах на `start`
- `POST /tasks/{id}/complete`:
  - обязательны `assignedUserId` и `deadlineDate`;
  - при завершении сервер автоматически проставляет `doneDateTime = now()`
- `PUT /tasks/{id}`: задачу в статусе `DONE` можно перевести обратно только
  в `IN_PROGRESS` — любое другое изменение статуса `DONE`-задачи отклоняется
- Параллельное изменение одной и той же задачи защищено optimistic locking
  (поле `version` в `TaskEntity`)

## Эндпоинты API

### `GET /tasks`

Список задач с фильтрацией и пагинацией.

Query-параметры (все опциональны):

| Параметр | Тип | По умолчанию |
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

Получить задачу по идентификатору. `404`, если не найдена.

```bash
curl http://localhost:8080/tasks/1
```

### `POST /tasks`

Создать новую задачу.

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

Возвращает `201 Created` с созданной задачей.

### `PUT /tasks/{id}`

Обновить задачу. Тело запроса — как у `POST`, без `id`. Возвращает `200 OK`.

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

Удалить задачу. Возвращает `204 No Content`.

```bash
curl -X DELETE http://localhost:8080/tasks/1
```

### `POST /tasks/{id}/start`

Перевести задачу в статус `IN_PROGRESS`. Возвращает `200 OK`.

```bash
curl -X POST http://localhost:8080/tasks/1/start
```

### `POST /tasks/{id}/complete`

Завершить задачу: перевести в статус `DONE`, проставить `doneDateTime`. Возвращает `200 OK`.

```bash
curl -X POST http://localhost:8080/tasks/1/complete
```

## Формат ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "message": "Entity not found",
  "detailedMessage": "Not found task with id = 99",
  "errorTime": "2026-06-08T14:30:00"
}
```

| Код | Когда возникает |
|---|---|
| `400` | Нарушение бизнес-правил или невалидные входные данные |
| `404` | Задача с указанным `id` не найдена |
| `500` | Непредвиденная ошибка на стороне сервера |

## Структура проекта

```
src/main/java/io/github/lischenerks/taskmanagement/
├── TaskManagementSystemApplication.java   # точка входа
├── Task.java                              # DTO (record)
├── TaskStatus.java                        # CREATED | IN_PROGRESS | DONE
├── TaskPriority.java                      # LOW | MEDIUM | HIGH
├── TaskMapper.java                        # Task <-> TaskEntity
├── controller/
│   └── TaskController.java
├── service/
│   ├── TaskService.java
│   └── TaskSearchFilter.java              # record для параметров фильтра
├── repository/
│   ├── TaskEntity.java                    # @Version — optimistic locking
│   └── TaskRepository.java                # JPQL-фильтры + pessimistic lock
└── exceptions/
    ├── GlobalExceptionHandler.java
    └── ErrorResponseDto.java

src/main/resources/
├── application.properties
└── db/migration/
    └── V1__init_task_table.sql            # Flyway
```

## Тестирование

```bash
./gradlew test
```

- `TaskServiceTest` — unit-тесты сервисного слоя (Mockito)
- `TaskServiceConcurrencyTest` — конкурентный тест: N параллельных
  `startTask` для одного исполнителя, проверка, что лимит в 5 активных
  задач не нарушается
- `TaskRepositoryTest` — `@DataJpaTest`, JPQL-фильтры репозитория
- `TaskControllerTest` — тесты контроллера (MockMvc)
- `TaskManagementSystemApplicationTests` — контекст поднимается на
  реальном PostgreSQL через Testcontainers (Docker должен быть доступен
  локально/в CI)

Отчёт покрытия генерируется Jacoco (`build/reports/jacoco/test/html`) и
автоматически обновляет бейдж в README при пуше в `master`/`dev`.
