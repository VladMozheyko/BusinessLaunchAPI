# LunchService — Полная документация проекта

## Содержание

- [Общие сведения](#общие-сведения)
- [Технологический стек](#технологический-стек)
- [Архитектура](#архитектура)
- [Роли и права доступа](#роли-и-права-доступа)
- [API Endpoints](#api-endpoints)
- [Бизнес-логика](#бизнес-логика)
- [Сущности и база данных](#сущности-и-база-данных)
- [Безопасность](#безопасность)
- [Конфигурация](#конфигурация)
- [CI/CD](#cicd)
- [Развёртывание](#развёртывание)
- [Разработка](#разработка)
- [Troubleshooting](#troubleshooting)

---

## Общие сведения

**LunchService** — микросервис для бронирования бизнес-ланчей в ресторанах и кафе.

### Основные возможности:
1. **Управление меню** — категории блюд, блюда (цена, вес, доступность, бесплатные дополнения)
2. **Комбо-наборы** — создание комбо-меню и управление активностью
3. **Карточки заведений** — франчайзи создают карточки с названием, описанием, логотипом и расписанием
4. **Бронирование ланчей** — пользователи бронируют место на конкретное время с выбором блюд
5. **Отмена по коду** — уникальный код `XXX-XXX-XXX` для отмены без авторизации
6. **Онбординг** — система заявок на присоединение к системе
7. **Назначение ролей** — GL и FO назначают LOCATION_ADMIN напрямую

---

## Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| Язык | Java | 21 |
| Фреймворк | Spring Boot | 4.1.1 |
| База данных | MySQL | 8.0 |
| ORM | Spring Data JPA | — |
| Аутентификация | JWT | 0.11.5 |
| API документация | SpringDoc OpenAPI | 3.0.2 |
| Загрузка .env | dotenv-java | 3.2.0 |
| HTTP-клиент | Spring Cloud OpenFeign | — |
| Сборка | Maven | 3.9 |
| Контейнеризация | Docker | — |

---

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                      LunchService                           │
│  Spring Boot 4.1.1 | Java 21 | MySQL 8.0                   │
├─────────────────────────────────────────────────────────────┤
│  Controllers (REST API)                                     │
│  ├── CategoryController                                      │
│  ├── DishController                                          │
│  ├── ComboController                                         │
│  ├── VenueProfileController                                  │
│  ├── LunchBookingController                                  │
│  ├── ComboBookingController                                  │
│  ├── OnboardingRequestController                             │
│  ├── AuthController                                          │
│  └── RoleInfoController                                      │
├─────────────────────────────────────────────────────────────┤
│  Services (Business Logic)                                  │
│  ├── CategoryService                                         │
│  ├── DishService                                             │
│  ├── ComboService                                            │
│  ├── VenueProfileService                                     │
│  ├── LunchBookingService                                     │
│  ├── ComboBookingService                                     │
│  ├── OnboardingRequestService                                │
│  └── SecurityContextHelper                                   │
├─────────────────────────────────────────────────────────────┤
│  Repositories (Data Access)                                 │
│  ├── CategoryRepository                                      │
│  ├── DishRepository                                          │
│  ├── ComboRepository                                         │
│  ├── VenueProfileRepository                                  │
│  ├── LunchBookingRepository                                  │
│  ├── ComboBookingRepository                                  │
│  ├── OnboardingRequestRepository                             │
│  └── UserRepository                                          │
├─────────────────────────────────────────────────────────────┤
│  Entities (Domain Models)                                   │
│  ├── Category, Dish, Combo, VenueProfile,                    │
│  ├── LunchBooking, ComboBooking, OnboardingRequest,          │
│  └── User, Role, AuthenticatedUser                           │
├─────────────────────────────────────────────────────────────┤
│  Config                                                     │
│  ├── SecurityConfig (JWT filter)                             │
│  ├── SecurityProperties (JWT settings)                       │
│  ├── CorsConfig                                              │
│  └── OpenApiConfig                                           │
├─────────────────────────────────────────────────────────────┤
│  Utils                                                      │
│  ├── DotenvLoader (.env parsing)                             │
│  ├── CancelCodeGenerator (code generation)                   │
│  ├── DayOfWeekConverter (enum conversion)                    │
│  └── IpAddressUtils (IP extraction)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Роли и права доступа

### Роли пользователей

| Роль | Полное название | Сокращение | Описание |
|------|-----------------|------------|----------|
| **GLOBAL_ADMIN** | Глобальный администратор | **GL** | Полный доступ ко всем ресурсам |
| **FRANCHISE_OWNER** | Владелец франшизы | **FO** | Управление своими категориями, блюдами, комбо |
| **LOCATION_ADMIN** | Локальный администратор | **LA** | Управление конкретной локацией |
| **USER** | Пользователь | **U** | Авторизованный клиент, может бронировать |
| **GUEST** | Гость | **G** | Неавторизованный посетитель (не используется) |

### Как назначаются роли

1. **Первоначальное назначение:**
   - Первый пользователь с email `admin@company.com` → **GL**
   - Все остальные → **U**
   - Роли хранятся в БД, не в JWT

2. **Присоединение (онбординг):**
   ```
   USER → заявка → GL одобряет → FRANCHISE_OWNER
   ```

3. **Назначение LOCATION_ADMIN:**
   ```
   GL / FO → назначает → LOCATION_ADMIN (напрямую)
   ```

### Сводная таблица прав

| Действие | GL | FO | LA | U |
|----------|----|----|----|---|
| Категории | ✅ | ✅ | ✅ | ❌ |
| Блюда | ✅ | ✅ | ✅ | ❌ |
| Комбо | ✅ | ✅ | ❌ | ❌ |
| Карточки заведений | ✅ | ✅ | ❌ | ❌ |
| Бронирование | ❌ | ❌ | ❌ | ✅ |
| Отмена по коду | ✅ | ✅ | ✅ | ✅ |
| Онбординг | ✅ | ✅ | ❌ | ✅ (подать) |
| Назначение LA | ✅ | ✅ | ❌ | ❌ |

---

## API Endpoints

Базовый путь: `/api/v1/lunch`

### 1. Управление категориями (`/category`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/category` | Создать категорию | GL, FO, LA |
| `PATCH` | `/category/{id}/addon` | Переключить в «Дополнительно» | GL, FO, LA |
| `DELETE` | `/category/{id}` | Удалить категорию (и блюда) | GL, FO, LA |

### 2. Управление блюдами (`/dish`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/dish` | Создать блюдо | GL, FO, LA |
| `PUT` | `/dish/{id}` | Обновить цену/вес | GL, FO, LA |
| `PATCH` | `/dish/{id}/availability` | Вкл/выкл блюдо | GL, FO, LA |
| `PATCH` | `/dish/{id}/free-addon` | Бесплатное дополнение | GL, FO, LA |
| `DELETE` | `/dish/{id}` | Удалить блюдо | GL, FO, LA |

### 3. Управление комбо (`/combo`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/combo` | Создать комбо | GL, FO |
| `PATCH` | `/combo/{id}/status` | Вкл/выкл комбо | GL, FO, LA |

### 4. Карточки заведений (`/venue`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/venue` | Создать/обновить карточку | FO |
| `GET` | `/venue/my` | Получить свою карточку | FO |
| `GET` | `/venue/{id}` | Получить карточку по ID | GL, FO |
| `PUT` | `/venue/{id}/activate` | Активировать карточку | FO |
| `PUT` | `/venue/{id}/deactivate` | Деактивировать карточку | FO |

### 5. Бронирование ланчей (`/booking`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/booking` | Создать бронирование | U |
| `POST` | `/booking/cancel-by-code` | Отменить по коду | U |
| `GET` | `/booking` | Получить свои бронирования | U |
| `GET` | `/booking/{id}` | Получить бронирование по ID | U, GL, FO, LA |

### 6. Комбо-бронирование (`/combo-booking`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/combo-booking` | Создать бронирование комбо | U |
| `POST` | `/combo-booking/cancel-by-code` | Отменить по коду | U |
| `POST` | `/combo-booking/cancel-by-phone` | Отменить по коду и телефону | U |

### 7. Онбординг (`/onboarding`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/onboarding` | Подать заявку | U |
| `GET` | `/onboarding/my` | Мои заявки | U |
| `DELETE` | `/onboarding/{id}/cancel` | Отменить заявку | U |
| `GET` | `/onboarding/pending` | Ожидающие заявки | GL |
| `PUT` | `/onboarding/{id}/approve` | Одобрить заявку | GL |
| `PUT` | `/onboarding/{id}/reject` | Отклонить заявку | GL |
| `POST` | `/onboarding/assign-location-admin` | Назначить LA | GL, FO |

### 8. Аутентификация (`/auth`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `POST` | `/auth/login` | Войти | Все |
| `POST` | `/auth/register` | Зарегистрироваться | Все |

### 9. Информация о ролях (`/roles`)

| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| `GET` | `/roles` | Список ролей и прав | Все |

---

## Бизнес-логика

### 1. Снимок данных в момент заказа

При создании бронирования данные блюда копируются в `LunchOrderItem`:
- `dishNameAtOrder` — название
- `dishWeightAtOrder` — вес
- `dishWeightUnitAtOrder` — единица измерения
- `priceAtOrder` — цена

Прошлые заказы не зависят от изменений блюда.

### 2. Запрет редактирования при активных заказах

Если у блюда есть активные бронирования (статусы `NEW`, `CONFIRMED`):
- ❌ Редактирование названия, веса, цены, количества **заблокировано**
- ✅ Разрешено только изменение `isAvailable`

После завершения всех активных заказов редактирование доступно.

### 3. Количество блюд

- `null` — не ограничено
- `0` — ни одного (недоступно)
- `>0` — ограниченное количество

### 4. Код отмены бронирования

- Формат: `XXX-XXX-XXX` (11 символов)
- Каждый блок: 1 буква (A-Z) + 2 цифры (0-9)
- Пример: `A1B-2C3-D4E`
- Rate limiting: 5 попыток/мин с одного IP

### 5. Каскадное удаление

- Удаление категории → удаляются все блюда
- Удаление бронирования → удаляются все позиции заказа

### 6. Расписание заведения

- Каждый день недели (Пн-Вс) — отдельное расписание
- Для каждого дня: `startTime`, `endTime`, `working` (работает/выходной)
- Если `working = false`, время игнорируется

---

## Сущности и база данных

### Основные сущности

| Сущность | Описание | Ключевые поля |
|----------|----------|---------------|
| `Category` | Категория блюд | `name`, `venueId`, `isAddon` |
| `Dish` | Блюдо | `name`, `price`, `weight`, `quantity`, `isAvailable`, `isFreeAddon`, `categoryId` |
| `Combo` | Комбо-набор | `name`, `price`, `dailyLimit`, `isActive`, `categories[]` |
| `VenueProfile` | Карточка заведения | `name`, `description`, `logoUrl`, `active`, `schedules[]` |
| `LunchBooking` | Бронирование ланча | `userId`, `venue`, `time`, `status`, `cancelCode`, `items[]` |
| `ComboBooking` | Бронирование комбо | `userId`, `combo`, `time`, `status`, `cancelCode`, `items[]` |
| `OnboardingRequest` | Заявка на присоединение | `userId`, `status`, `reviewedBy`, `reviewedAt` |
| `User` | Пользователь | `email`, `role`, `externalId` |
| `Role` | Роль | `authority` |

### Статусы

**BookingStatus:** `PENDING`, `APPROVED`, `COMPLETED`, `CANCELLED`

**ComboBookingStatus:** `PENDING`, `APPROVED`, `COMPLETED`, `CANCELLED`

**OnboardingRequestStatus:** `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`

---

## Безопасность

### JWT-аутентификация

- Токены выдаются Auth-сервисом
- Фильтр `JwtAuthenticationFilter` проверяет токен
- `AuthenticatedUser` содержит: `userId`, `externalId`, `email`, `role`, `authorities`

### Ролевой доступ

Каждый endpoint защищён через `@PreAuthorize`:
```java
@PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('FRANCHISE_OWNER')")
```

### Rate Limiting

Endpoint отмены по коду: 5 попыток/мин с одного IP.

### Конфигурация безопасности

| Параметр | Значение по умолчанию |
|----------|----------------------|
| `security.jwt.header` | `Authorization` |
| `security.jwt.prefix` | `Bearer` |
| `security.jwt.secret-key` | (из .env) |

---

## Конфигурация

### Файлы конфигурации

| Файл | Назначение |
|------|------------|
| `application.properties` | Общие настройки (timezone, JPA, JWT header) |
| `application-local.properties` | Локальная разработка (DDL=create) |
| `application-dev.properties` | Dev-среда (DDL=update) |
| `application-prod.properties` | Prod-среда (DDL=update, env vars) |

### .env файл

```
# Общие настройки
API_SERVER_URL=http://localhost:8080
LOGGING_LEVEL=debug
ROOT_LOGGING_LEVEL=info

# === LOCAL ===
LOCAL_DB_URL=jdbc:mysql://localhost:3306/lunch_service
LOCAL_DB_USERNAME=root
LOCAL_DB_PASSWORD=den27lad27
LOCAL_SECURITY_JWT_SECRET_KEY=local-secret-key...

# === DEV ===
DEV_DB_URL=jdbc:mysql://dev-mysql:3306/lunch_service
DEV_DB_USERNAME=lunchuser
DEV_DB_PASSWORD=

# === PROD ===
PROD_DB_URL=jdbc:mysql://prod-mysql:3306/lunch_service
PROD_DB_USERNAME=lunchuser
PROD_DB_PASSWORD=<prod-db-password>
```

### Загрузка .env

`DotenvLoader` загружает переменные при старте:
1. Считывает `SPRING_PROFILES_ACTIVE` из `.env`
2. Загружает переменные с префиксом профиля (`LOCAL_`, `DEV_`, `PROD_`)
3. Устанавливает через `System.setProperty()`

---

## CI/CD

### GitHub Actions

| Workflow | Файл | Триггер | Действие |
|----------|------|---------|----------|
| **Dev Deploy** | `ci-cd-dev.yml` | Push в `dev`/`develop` | Build → Docker → Deploy |
| **Prod Deploy** | `ci-cd-prod.yml` | Push в `main`/`master` | Build → Security Scan → Docker → Deploy |

### Теги Docker image

| Workflow | Теги |
|----------|------|
| Dev | `dev-latest` |
| Prod | `sha-{commit}`, `prod-latest`, `{tag}` (если задан) |

### Required Secrets

| Secret | Описание |
|--------|----------|
| `DEV_SERVER_HOST` | IP dev сервера |
| `DEV_SERVER_USER` | Пользователь dev |
| `DEV_SERVER_SSH_KEY` | SSH ключ dev |
| `PROD_SERVER_HOST` | IP prod сервера |
| `PROD_SERVER_USER` | Пользователь prod |
| `PROD_SERVER_SSH_KEY` | SSH ключ prod |

Подробная инструкция: `.github/CICD_SETUP.md`

---

## Развёртывание

### Локальная разработка

```bash
# 1. Настройка
cp .env.example .env  # если есть
# Отредактировать .env

# 2. Запуск
./mvnw spring-boot:run

# 3. Проверка
curl http://localhost:8080/actuator/health
```

### Docker

```bash
# 1. Сборка и запуск
docker compose up -d

# 2. Проверка
docker compose ps
docker compose logs -f

# 3. Остановка
docker compose down
```

### Production

```bash
# На сервере
cd /opt/lunch-service
docker compose --env-file .env.prod up -d --remove-orphans
```

---

## Разработка

### Структура проекта

```
src/main/java/dev/codelad/lunchservice/
├── config/          # Конфигурация (Security, CORS, OpenAPI)
├── constant/        # Константы (Messages)
├── controller/      # REST контроллеры
├── dto/             # DTO (request/response)
│   ├── request/
│   └── response/
├── entity/          # JPA сущности
├── enums/           # Enum (BookingStatus, ComboBookingStatus)
├── exception/       # Исключения
├── repository/      # Spring Data JPA репозитории
├── security/        # JWT filter, AuthenticatedUser, Role
├── service/         # Сервисы (бизнес-логика)
└── util/            # Утилиты (DotenvLoader, CancelCodeGenerator)
```

### Полезные команды

```bash
# Сборка
./mvnw clean package

# Тесты
./mvnw test

# Проверка кода
./mvnw verify

# Запуск
./mvnw spring-boot:run

# Docker
docker compose up -d
docker compose down
docker compose logs -f
```

### Код-стайл

- **Cognitive Complexity:** ≤ 15 (SonarQube)
- **UTF-8 кодировка** для всего проекта
- **Java text blocks** (`"""`) для Swagger описаний
- **java.time API** вместо java.util.Date
- **noneMatch()** вместо `!anyMatch()`

---

## Troubleshooting

### Приложение не запускается

1. Проверь `.env` файл
2. Проверь `SPRING_PROFILES_ACTIVE`
3. Проверь подключение к MySQL
4. Посмотри логи: `./mvnw spring-boot:run`

### Ошибка подключения к БД

```properties
# application-local.properties
spring.datasource.url=jdbc:mysql://localhost:3306/lunch_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### JWT ошибка

```properties
# .env
SECURITY_JWT_SECRET_KEY=<base64-encoded-64-byte-key>
```

### Healthcheck не проходит

```bash
# Посмотри логи
docker compose logs --tail 100

# Увеличь start_period
healthcheck:
  start_period: 120s
```

---

## Ссылки

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Actuator:** `http://localhost:8080/actuator/health`
- **GitHub Actions:** https://github.com/your-repo/actions
- **Docker Hub/GHCR:** ghcr.io/your-repo/lunch-service

---

## Авторы и поддержка

- **Разработка:** LunchService Team
- **Документация:** PROJECT_DESCRIPTION.md, BUSINESS_LOGIC.md, ROLES_AND_PERMISSIONS.md
- **CI/CD:** .github/CICD_SETUP.md
