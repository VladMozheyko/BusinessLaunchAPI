# LunchService

![LunchService Logo](src/main/resources/images/lunch_service.jpg)

## Назначение

Распределённый микросервис для бронирования бизнес-ланчей в заведениях. Предоставляет полный цикл управления: от создания меню (категории, блюда, комбо) и карточек заведений до бронирования ланчей пользователями.

### Ключевые возможности

- **Управление меню** — CRUD категорий, блюд, комбо-наборов; снимок данных при заказе; блокировка редактирования при активных бронированиях БЛ
- **Бронирование** — создание броней с выбором блюд; отмена по уникальному коду `XXX-XXX-XXX`; подтверждение/завершение администратором
- **Комбо-бронирование** — аналогично обычным броням, но с комбо-наборами и дополнительными позициями
- **Карточки заведений** — создание профилей с расписанием работы по дням недели
- **Синхронизация меню** — импорт категорий и блюд из FoodService (Feign)
- **Уведомления** — проксирование в NotificationService (FCM)

## Стек технологий

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 4.1.1
- **ORM:** Spring Data JPA + Hibernate
- **База данных:** MySQL (production), H2 (test)
- **Межсервисное взаимодействие:** Spring Cloud OpenFeign (FoodService 8086, NotificationService 8082, VenueService 8081)
- **Аутентификация:** JWT (stateless), два ключа — Auth Service + Bar Auth
- **Авторизация:** `@PreAuthorize`, роль-based доступ (GL, BAR/BREW OWNER, LM, U)
- **Rate Limiting:** In-memory, 5 запросов/мин на IP для cancel-эндпоинтов
- **Документация:** SpringDoc OpenAPI 3.0.2 (`/swagger-ui.html`)
- **Тестирование:** Spring Boot Test, MockMvc, H2

## Архитектура и роль

LunchService — доменно-ориентированный микросервис в рамках multi-repo архитектуры. Не является API-шлюзом или шлюзом авторизации: работает со своей базой данных (`lunch` schema) и общается с другими сервисами через Feign-клиенты.

### Доменные области

| Область | Контроллер | Сущности |
|---------|-----------|----------|
| Меню | `CategoryController`, `DishController`, `ComboController` | `Category`, `Dish`, `Combo`, `ComboAddon` |
| Заведения | `VenueProfileController` | `VenueProfile`, `VenueSchedule` |
| Бронирование | `LunchBookingController`, `ComboBookingController` | `LunchBooking`, `LunchOrderItem`, `ComboBooking`, `ComboBookingItem` |
| Синхронизация | `FoodSyncController`, `FoodSyncService` | `FoodSyncStatus` |
| Уведомления | `DeviceProxyController`, `PreferencesProxyController` | — (проксирование) |

### Ключевые сущности

#### `Category` (categories)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `Long` | Auto-generated PK |
| `venueProfile` | `VenueProfile` | FK → venue_profiles (ManyToOne) |
| `name` | `String` | Название категории |
| `hidden` | `Boolean` | Soft-delete флаг |
| `syncedFromFood` | `Boolean` | Флаг источника |
| `dishes` | `List<Dish>` | OneToMany, cascade ALL |

#### `Dish` (dishes)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `UUID` | UUID PK |
| `category` | `Category` | FK → categories (ManyToOne) |
| `name` | `String` | Название блюда |
| `weight`, `weightUnit` | `Integer`, `String` | Вес и единица измерения |
| `price` | `Integer` | Цена в копейках |
| `isAvailable` | `boolean` | Видимость в меню |
| `quantity` | `Integer` | Лимит количества |
| `isFreeAddon` | `boolean` | Бесплатное дополнение |
| `active`, `hidden` | `Boolean` | Флаги активности |
| `dishType` | `DishType` | MAIN / ADDON |

#### `Combo` (combos)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `Long` | Auto-generated PK |
| `venueProfile` | `VenueProfile` | FK → venue_profiles |
| `name`, `price` | `String`, `Integer` | Название и цена в копейках |
| `isActive` | `boolean` | Активность в меню |
| `dailyLimit` | `Integer` | Лимит в день (0 = безлимит) |
| `categories` | `List<Category>` | ManyToMany (combo_categories) |
| `addons`, `freeAddons` | `List<Dish>` | ManyToMany |

#### `LunchBooking` (lunch_bookings)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `UUID` | UUID PK |
| `userId` | `String` | externalId из Auth Service |
| `venue` | `VenueProfile` | FK → venue_profiles |
| `time` | `LocalTime` | Время бронирования |
| `status` | `BookingStatus` | PENDING / APPROVED / COMPLETED / CANCELLED / NO_SHOW |
| `cancelCode` | `String` | Уникальный код отмены (XXX-XXX-XXX) |
| `phoneNumber` | `String` | Телефон клиента |
| `totalGuests` | `Integer` | Количество гостей |
| `expiredAt` | `Instant` | Срок действия |
| `items` | `List<LunchOrderItem>` | OneToMany, cascade ALL |

#### `LunchOrderItem` (lunch_order_items)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `UUID` | UUID PK |
| `booking` | `LunchBooking` | FK → lunch_bookings |
| `dish` | `Dish` | FK → dishes |
| `quantity` | `Integer` | Количество |
| `dishNameAtOrder`, `dishWeightAtOrder` | `String`, `Integer` | Снимок данных при заказе |
| `priceAtOrder` | `BigDecimal` | Цена в момент заказа |

#### `VenueProfile` (venue_profiles)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `Long` | Auto-generated PK |
| `venueId` | `Long` | Уникальный ID из Bar Auth |
| `name`, `description`, `logoUrl` | `String` | Название, описание, логотип |
| `schedules` | `List<VenueSchedule>` | OneToMany |
| `bookings` | `List<LunchBooking>` | OneToMany, cascade ALL |

### Транзакции

| Метод | Тип транзакции | Обоснование |
|-------|----------------|-------------|
| `CategoryService.create()` | полная (`@Transactional`) | INSERT + каскад |
| `CategoryService.findAllByVenue()` | `@Transactional(readOnly = true)` | только чтение |
| `DishService.create()` | полная | INSERT |
| `DishService.update()` | полная | UPDATE + проверка lock |
| `DishService.findByCategory()` | `@Transactional(readOnly = true)` | только чтение |
| `ComboService.create()` | полная | INSERT + ManyToMany связи |
| `ComboService.findByVenue()` | `@Transactional(readOnly = true)` | только чтение |
| `LunchBookingService.create()` | полная | INSERT + каскад items + snapshot |
| `LunchBookingService.findByUser()` | `@Transactional(readOnly = true)` | только чтение |
| `LunchBookingService.cancelByCode()` | полная | UPDATE status + каскад |
| `ComboBookingService.create()` | полная | INSERT + лимит-валидация |
| `ComboBookingService.findByPhone()` | `@Transactional(readOnly = true)` | только чтение |
| `VenueProfileService.createOrUpdate()` | полная | INSERT/UPDATE + schedule cascade |
| `FoodSyncService.sync()` | полная | UPSERT categories + dishes |

### Оптимистическая блокировка

`@Version` **не применяется** на уровне сущностей. Целостность данных обеспечивается:
- **Soft-delete** (`hidden` / `active` флаги) вместо каскадного удаления
- **Блокировка редактирования блюд** — `DishService` проверяет `existsByDishIdAndActiveOrders()` перед UPDATE цены/веса/названия
- **Каскадные операции** — `cascade = ALL` на OneToMany связях (Category→Dish, Booking→OrderItem)

### Authorization

| Роль | Полное название | Права |
|------|----------------|-------|
| `GLOBAL_ADMIN` (GL) | Глобальный администратор | Полный доступ ко всем ресурсам, рассмотрение заявок онбординга |
| `BAR/BREW OWNER` (FO) | Владеле�� франшизы | Управление своими категориями/блюдами/комбо, назначение LA |
| `LOCATION_MANAGER` (LA) | Локальный администратор | Управление категориями/блюдами/комбо своей локации |
| `USER` (U) | Пользователь | Бронирование, онбординг |

## API Endpoints

Базовый путь: `/api/v1/lunch`

### Меню

| Метод | Путь | Описание | Авторизация | Пример запроса (JSON) | Пример ответа (JSON) |
|-------|------|----------|-------------|----------------------|---------------------|
| `POST` | `/category` | Создать категорию | GL, BAR/BREW OWNER, LM | `{"name": "Салаты"}` | `{"id": 1, "name": "Салаты", "hidden": false, "syncedFromFood": false, "createdAt": "2025-10-26T10:00:00Z"}` |
| `GET` | `/categories` | Все категории с блюдами | GL, BAR/BREW OWNER, LM | — | `[{"id": 1, "name": "Салаты", "dishes": [...]}]` |
| `GET` | `/category/my` | Категории по venueId | GL, BAR/BREW OWNER, LM | — | `[{"id": 1, "name": "Салаты", "venueId": 42}]` |
| `PATCH` | `/category/{id}/toggle` | Переключить hidden | GL, BAR/BREW OWNER, LM | — | `{"id": 1, "hidden": true}` |
| `DELETE` | `/category/{id}/hard` | Жёсткое удаление | GL | — | `204 No Content` |
| `POST` | `/dish` | Создать блюдо | GL, BAR/BREW OWNER, LM | `{"categoryId": 1, "name": "Цезарь", "weight": 250, "price": 45000, "quantity": 10}` | `{"id": "550e8400-e29b-41d4-a716-446655440000", "name": "Цезарь", "price": 45000}` |
| `PUT` | `/dish/{id}` | Обновить блюдо | GL, BAR/BREW OWNER, LM | `{"weight": 300, "price": 50000}` | `{"id": "...", "weight": 300, "price": 50000}` |
| `PATCH` | `/dish/{id}/availability` | Вкл/выкл блюдо | GL, BAR/BREW OWNER, LM | `{"isAvailable": false}` | `{"id": "...", "isAvailable": false}` |
| `PATCH` | `/dish/{id}/free-addon` | Флаг бесплатного дополнения | GL, BAR/BREW OWNER, LM | `{"isFreeAddon": true}` | `{"id": "...", "isFreeAddon": true}` |
| `DELETE` | `/dish/{id}` | Скрыть блюдо | GL, BAR/BREW OWNER, LM | — | `204 No Content` |
| `DELETE` | `/dish/{id}/hard` | Жёсткое удаление | GL | — | `204 No Content` |
| `GET` | `/combo` | Все комбо | GL, BAR/BREW OWNER, LM | — | `[{"id": 1, "name": "Комбо 1", "price": 75000, "isActive": true}]` |
| `POST` | `/combo` | Создать комбо | GL, BAR/BREW OWNER | `{"name": "Комбо бизнес", "price": 75000, "dailyLimit": 20}` | `{"id": 1, "name": "Комбо бизнес", "price": 75000}` |
| `PATCH` | `/combo/{id}/status` | Вкл/выкл комбо | GL, BAR/BREW OWNER, LM | `{"isActive": false}` | `{"id": 1, "isActive": false}` |

### Заведения

| Метод | Путь | Описание | Авторизация | Пример запроса (JSON) | Пример ответа (JSON) |
|-------|------|----------|-------------|----------------------|---------------------|
| `POST` | `/venue` | Создать/обновить карточку | FO | `{"venueId": 42, "name": "Кафе №1", "schedules": [...]}` | `{"id": 1, "venueId": 42, "name": "Кафе №1"}` |
| `GET` | `/venue/my` | Получить свою карточку | FO | — | `{"id": 1, "venueId": 42, "name": "Кафе №1"}` |
| `GET` | `/venue/{id}` | Получить карточку по ID | GL, BAR/BREW OWNER | — | `{"id": 1, "venueId": 42, "name": "Кафе №1"}` |

### Бронирование

| Метод | Путь | Описание | Авторизация | Пример запроса (JSON) | Пример ответа (JSON) |
|-------|------|----------|-------------|----------------------|---------------------|
| `POST` | `/booking` | Создать бронь | U | `{"venueId": 42, "time": "13:00", "totalGuests": 2, "items": [{"dishId": "...", "quantity": 1}]}` | `{"id": "550e8400-...", "cancelCode": "A1B-2C3-D4E", "status": "PENDING"}` |
| `GET` | `/booking/my` | Мои брони | U | — | `[{"id": "...", "venueId": 42, "time": "13:00", "status": "PENDING"}]` |
| `POST` | `/booking/cancel-by-code` | Отмена по коду | U | `{"cancelCode": "A1B-2C3-D4E"}` | `{"id": "...", "status": "CANCELLED"}` |
| `POST` | `/booking/{id}/approve` | Подтвердить бронь | GL, BAR/BREW OWNER, LM | — | `{"id": "...", "status": "APPROVED"}` |
| `POST` | `/booking/{id}/complete` | Завершить бронь | GL, BAR/BREW OWNER, LM | — | `{"id": "...", "status": "COMPLETED"}` |
| `POST` | `/booking/{id}/cancel` | Отменить бронь админом | GL, BAR/BREW OWNER, LM | — | `{"id": "...", "status": "CANCELLED"}` |

### Комбо-бронирование

| Метод | Путь | Описание | Авторизация | Пример запроса (JSON) | Пример ответа (JSON) |
|-------|------|----------|-------------|----------------------|---------------------|
| `POST` | `/combo-booking` | Создать комбо-бронь | U | `{"venueId": 42, "time": "13:00", "totalGuests": 2, "items": [{"comboId": 1, "quantity": 1}]}` | `{"id": "...", "cancelCode": "X9Y-8Z7-W6V", "status": "PENDING"}` |
| `GET` | `/combo-booking/my` | Мои комбо-брони | U | — | `[{"id": "...", "status": "PENDING"}]` |
| `POST` | `/combo-booking/cancel-by-code` | Отмена по коду | U | `{"cancelCode": "X9Y-8Z7-W6V"}` | `{"id": "...", "status": "CANCELLED"}` |
| `GET` | `/combo-booking/by-phone` | Брони по телефону | U | — | `[{"id": "...", "phoneNumber": "+79001234567"}]` |

### Синхронизация

| Метод | Путь | Описание | Авторизация | Пример запроса (JSON) | Пример ответа (JSON) |
|-------|------|----------|-------------|----------------------|---------------------|
| `POST` | `/food-sync/sync/{venueId}` | Синхронизировать меню | GL, BAR/BREW OWNER, LM | — | `{"venueId": 42, "syncedCategoriesCount": 5, "syncedDishesCount": 23}` |
| `GET` | `/food-sync/status/{venueId}` | Статус синхронизации | GL, BAR/BREW OWNER, LM | — | `{"venueId": 42, "lastSyncTime": "2025-10-26T10:00:00Z", "syncType": "MANUAL"}` |

## Обработка ошибок

| Код ошибки | HTTP Status | Сообщение | Причина | Решение |
|------------|-------------|-----------|---------|---------|
| `OPERATION_SUCCESS` | 200/201 | Операция выполнена | Успешное создание/обновление/удаление | — |
| `VALIDATION_FAILED` | 400 | Поле «{name}» обязательно | `@Valid` не прошёл валидацию | Проверить формат данных |
| `DISH_EDIT_BLOCKED` | 409 | Блюдо заблокировано: есть активные бронирования | Попытка UPDATE цены/веса при статусе NEW/CONFIRMED | Дождаться завершения броней |
| `RATE_LIMIT_EXCEEDED` | 429 | Превышен лимит запросов | >5 попыток отмены по коду с одного IP за 1 мин | Подождать сброса счётчика |
| `FEIGN_TIMEOUT` | 504 | Food Service не отвечает | Feign-клиент не получил ответ за timeout | Проверить Food Service (8086) |
| `NOTIFICATION_SERVICE_UNAVAILABLE` | 502 | Notification Service недоступен | Feign-клиент вернул 5xx | Проверить Notification Service (8082) |

## Развертывание

```bash
# Сборка
./mvnw clean package -DskipTests

# Docker
docker build -t lunchservice:latest .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/lunch \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET_KEY=<key> \
  -e JWT_BAR_AUTH_SECRET_KEY=<bar-key> \
  lunchservice:latest
```

### docker-compose (фрагмент)

```yaml
lunch-service:
  image: lunchservice:latest
  ports:
    - "8080:8080"
  environment:
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/lunch
    SPRING_DATASOURCE_USERNAME: root
    SPRING_DATASOURCE_PASSWORD: password
    JWT_SECRET_KEY: ${JWT_SECRET}
    JWT_BAR_AUTH_SECRET_KEY: ${BAR_AUTH_SECRET}
    FEIGN_CLIENT_URL_FOOD: http://food-service:8086
    FEIGN_CLIENT_URL_NOTIFICATION: http://notification-service:8082
  depends_on:
    - mysql
```

### Scheduled Tasks

| Task | Cron | Описание |
|------|------|----------|
| `BookingCleanupTask` | `fixedRate = 60000` | Проверка и перевод просроченных APPROVED броней в NO_SHOW (каждую минуту) |
| `FoodSyncTask` | `0 0 3 * * *` (Europe/Moscow) | Автоматическая синхронизация меню из Food Service (ежедневно в 03:00 MSK) |

## Тестирование

```bash
# Все тесты
./mvnw test

# С покрытием
./mvnw test jacoco:report

# Только контроллеры
./mvnw test -Dtest=*ControllerTest
```

### Стратегия тестирования

| Уровень | Инструмент | Что тестируется |
|---------|-----------|-----------------|
| Controller | `MockMvc` + `@SpringBootTest` | HTTP-маршруты, валидация, кодировки ответов |
| Service | `@SpringBootTest` | Бизнес-логика, транзакции, Feign-клиенты (Mock) |
| Repository | `@DataJpaTest` + H2 | SQL-запросы, JOIN, каскады |
| Security | `@WithMockCustomUser` | `@PreAuthorize`, роли, rate-limiting |

### Ключевые тестовые сценарии

- **Блокировка редактирования блюда** — `DishServiceTest.update_blocked_when_active_bookings_exist()`
- **Генерация cancel-кода** — `CancelCodeGeneratorTest.uniqueness_and_format()`
- **Rate limiting** — `InMemoryRateLimiterTest.fifth_request_allowed_sixth_rejected()`
- **Snapshot данных при заказе** — `LunchBookingServiceTest.order_items_snapshot_preserved_after_dish_update()`
- **Soft-delete каскад** — `CategoryServiceTest.delete_removes_dishes()`
