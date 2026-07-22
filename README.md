# Task Management System

[English version](README.en.md)

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Coverage](.github/badges/jacoco.svg)

REST API для трекера задач: создание задач, назначение исполнителей, контроль дедлайнов и приоритетов, переходы по жизненному циклу задачи.

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
- [Лицензия](#лицензия)

## Возможности

- CRUD-операции над задачами
- Фильтрация списка задач по автору, исполнителю, статусу и приоритету, пагинация и сортировка
- Отдельные эндпоинты `start` и `complete` для перевода задачи по жизненному циклу
- Доменная модель и слой DTO разделены, маппинг между ними явный
- Валидация входных данных и единый формат ответа об ошибке
- Пессимистичная блокировка строк исполнителя защищает от гонок при параллельном старте задач
- Optimistic locking на уровне сущности, конфликт версий возвращает `409`
- Версионируемая схема БД через Flyway
- Автогенерируемая OpenAPI-документация и Swagger UI
- Docker-образ и `docker-compose.yml` для запуска в один шаг
- Интеграционные тесты на реальном PostgreSQL через Testcontainers
- Отчёт покрытия Jacoco

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

```bash
git clone https://github.com/LischenerKS/task-management-system.git
cd task-management-system
docker compose up --build
```

Приложение доступно на `http://localhost:8080`, PostgreSQL слушает порт `5432`.

### Локальный запуск

**Требования:** JDK 21, PostgreSQL 16. Gradle отдельно ставить не нужно, используется wrapper (`gradlew`).

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

Схема базы данных создаётся и версионируется автоматически при старте через Flyway-миграции (`src/main/resources/db/migration`).

Либо собрать и запустить JAR-файл:

```bash
./gradlew build
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

Сервер стартует на `http://localhost:8080`.

## Документация API (Swagger)

```
http://localhost:8080/swagger-ui/index.html
```

Спецификация OpenAPI в формате JSON находится по адресу `http://localhost:8080/v3/api-docs`.

## Жизненный цикл задачи

```
CREATED ──[start]──► IN_PROGRESS ──[complete]──► DONE
```

Статус задачи меняется только через эндпоинты `start` и `complete`. `PUT /tasks/{id}` статус не затрагивает.

## Бизнес-правила

- Статус всегда выставляет сервер: `CREATED` при создании, `IN_PROGRESS`/`DONE` через `start`/`complete`. Напрямую через create/update его установить нельзя
- При создании (`POST /tasks`) обязательны `creatorId`, `assignedUserId`, `deadlineDate` (должен быть в будущем) и `priority`
- При обновлении (`PUT /tasks/{id}`) обязательны `assignedUserId`, `deadlineDate` (тоже только в будущем) и `priority`. Изменить `creatorId` после создания задачи нельзя
- Старт задачи (`POST /tasks/{id}/start`):
  - требует заполненного `assignedUserId`
  - запрещён, если задача уже в статусе `IN_PROGRESS`
  - у одного исполнителя не может быть больше **5** задач одновременно в статусе `IN_PROGRESS`; лимит проверяется под пессимистичной блокировкой строк исполнителя, что исключает гонку при параллельных запросах на `start`
- Завершение задачи (`POST /tasks/{id}/complete`) требует заполненных `assignedUserId` и `deadlineDate`; `doneDateTime` проставляется автоматически текущим временем
- Параллельное изменение одной и той же задачи защищено optimistic locking (поле `version`); конфликт версий возвращает `409 Conflict`

## Эндпоинты API

### `GET /tasks`

Список задач с фильтрацией, пагинацией и сортировкой.

Query-параметры (все опциональны):

| Параметр | Тип | По умолчанию |
|---|---|---|
| `creatorId` | Long | не задан |
| `assignedUserId` | Long | не задан |
| `status` | `CREATED` / `IN_PROGRESS` / `DONE` | не задан |
| `priority` | `LOW` / `MEDIUM` / `HIGH` | не задан |
| `page` | Integer | 0 |
| `size` | Integer | 10 |
| `sort` | строка | `id` |

```bash
curl "http://localhost:8080/tasks?status=IN_PROGRESS&priority=HIGH&size=5"
```

Ответ оборачивается в стандартный объект Spring Data `Page`: `content`, `totalElements`, `totalPages`, `number`, `size` и другие служебные поля.

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

Возвращает `201 Created` с созданной задачей в статусе `CREATED`.

### `PUT /tasks/{id}`

Обновить исполнителя, дедлайн и приоритет задачи. Статус и создатель задачи не меняются.

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "assignedUserId": 7,
    "deadlineDate": "2026-12-31T23:59:59",
    "priority": "HIGH"
  }'
```

Возвращает `200 OK`, при конфликте версий `409 Conflict`.

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
| `409` | Конфликт версий при параллельном обновлении задачи (optimistic locking) |
| `500` | Непредвиденная ошибка на стороне сервера |

## Структура проекта

```
src/main/java/io/github/lischenerks/taskmanagement/
├── TaskManagementSystemApplication.java   # точка входа
├── domain/
│   ├── Task.java                          # доменная модель (record)
│   ├── TaskStatus.java                    # CREATED | IN_PROGRESS | DONE
│   └── TaskPriority.java                  # LOW | MEDIUM | HIGH
├── dto/
│   ├── CreateTaskDto.java                 # тело POST /tasks
│   ├── UpdateTaskDto.java                 # тело PUT /tasks/{id}
│   ├── TaskResponseDto.java               # тело ответа
│   └── ErrorResponseDto.java              # единый формат ошибки
├── mapper/
│   └── TaskMapper.java                    # Task <-> TaskEntity <-> DTO
├── controller/
│   └── TaskController.java
├── service/
│   ├── TaskService.java
│   └── TaskSearchFilter.java              # record для параметров фильтра
├── repository/
│   ├── TaskEntity.java                    # @Version, optimistic locking
│   └── TaskRepository.java                # JPQL-фильтры, пессимистичная блокировка
└── exceptions/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.properties
└── db/migration/
    └── V1__init_task_table.sql            # Flyway
```

## Тестирование

```bash
./gradlew test
```

- `TaskServiceTest`: unit-тесты сервисного слоя (Mockito)
- `TaskServiceConcurrencyTest`: параллельные `startTask` для одного исполнителя проверяют лимит в 5 активных задач, параллельный `updateTask` над одной задачей проверяет optimistic locking
- `TaskRepositoryTest`: `@DataJpaTest`, JPQL-фильтры репозитория
- `TaskControllerTest`: тесты контроллера (MockMvc), включая ответ `409` при конфликте версий
- `TaskManagementSystemApplicationTests`: контекст поднимается на реальном PostgreSQL через Testcontainers (Docker должен быть доступен локально/в CI)

Отчёт покрытия генерируется Jacoco (`build/reports/jacoco/test/html`) и автоматически обновляет бейдж в README при пуше в `master`/`dev`.

## Лицензия

Проект распространяется под лицензией [Apache License 2.0](LICENSE).