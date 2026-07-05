# Task Management System

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

REST API для управления задачами (task tracker): создание, назначение исполнителей, контроль дедлайнов и приоритетов, переходы по жизненному циклу задачи.

## Содержание

- [Возможности](#возможности)
- [Технологический стек](#технологический-стек)
- [Быстрый старт](#быстрый-старт)
    - [Через Docker Compose](#через-docker-compose)
    - [Локальный запуск](#локальный-запуск)
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
- Автогенерируемая OpenAPI-документация (springdoc) и Swagger UI
- Docker-образ и `docker-compose.yml` для запуска в один шаг

## Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 4.0.6 (Web, Data JPA, Validation) |
| База данных | PostgreSQL 16 |
| ORM | Hibernate / Spring Data JPA |
| Документация API | springdoc-openapi 3.0.3 |
| Сборка | Gradle 9.5.1 (Kotlin DSL) |
| Тесты | JUnit 5, Mockito |
| Логирование | SLF4J |
| Контейнеризация | Docker, Docker Compose |

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

**Требования:** JDK 21, PostgreSQL, Gradle не нужен — используется wrapper (`gradlew`).

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

Схема базы данных создаётся автоматически при первом запуске (`spring.jpa.hibernate.ddl-auto=update`).

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

- При создании задачи поля `id` и `status` не передаются — выставляются автоматически (`status = CREATED`)
- `POST /tasks/{id}/start`: обязателен `assignedUserId`; у одного исполнителя не может быть больше 5 задач в статусе `IN_PROGRESS` одновременно; повторный `start` уже стартовавшей задачи запрещён
- `POST /tasks/{id}/complete`: обязательны `assignedUserId` и `deadlineDate`; при завершении автоматически проставляется `doneDateTime`
- Задачу в статусе `DONE` через `PUT` можно перевести только обратно в `IN_PROGRESS` — прямое редактирование других полей заблокировано
- `deadlineDate` обязателен и должен быть указан в будущем (`@Future`)
- `creatorId` и `priority` — обязательные поля при создании и обновлении

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

```http
GET /tasks?status=IN_PROGRESS&priority=HIGH&pageSize=5
```

### `GET /tasks/{id}`

Получить задачу по идентификатору. `404`, если не найдена.

### `POST /tasks`

Создать новую задачу.

```json
{
  "creatorId": 42,
  "assignedUserId": 7,
  "deadlineDate": "2026-12-31T23:59:59",
  "priority": "MEDIUM"
}
```

Возвращает `201 Created` с созданной задачей.

### `PUT /tasks/{id}`

Обновить задачу. Тело запроса — как у `POST`, без `id`. Возвращает `200 OK`.

### `DELETE /tasks/{id}`

Удалить задачу. Возвращает `204 No Content`.

### `POST /tasks/{id}/start`

Перевести задачу в статус `IN_PROGRESS`. Возвращает `200 OK`.

### `POST /tasks/{id}/complete`

Завершить задачу: перевести в статус `DONE`, проставить `doneDateTime`. Возвращает `200 OK`.

## Формат ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "type": "Entity not found",
  "message": "Not found task with id = 99",
  "timestamp": "2026-06-08T14:30:00"
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
│   ├── TaskEntity.java
│   └── TaskRepository.java                # JPA + JPQL-запрос с фильтрами
└── exceptions/
    ├── GlobalExceptionHandler.java
    └── ErrorResponseDto.java
```

## Тестирование

Юнит-тесты написаны на JUnit 5 с использованием Mockito. Запуск:

```bash
./gradlew test
```
