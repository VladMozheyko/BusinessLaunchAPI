package dev.codelad.lunchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Информация о ролях и правах доступа в системе LunchService.
 * <p>
 * Содержит полную сводную таблицу всех эндпоинтов с правами доступ�� по ролям.
 * <p>
 * <b>Роли:</b>
 * <ul>
 *   <li>GL — GLOBAL_ADMIN (Глобальный администратор)</li>
 *   <li>FO — FRANCHISE_OWNER (Владелец франшизы)</li>
 *   <li>LA — LOCATION_ADMIN (Локальный администратор)</li>
 *   <li>U — USER (Пользователь)</li>
 * </ul>
 */
@Tag(name = "📊 Информация о ролях", description = "Полная сводная таблица прав доступа по ролям")
@RestController
@RequestMapping("/api/v1/lunch")
public class RoleInfoController {

    @GetMapping("/roles")
    @Operation(
            summary = "Информация о ролях и правах доступа",
            description = """
                    Возвращает полную информацию о ролях в системе LunchService и правах доступа к API-эндпоинтам.
                    
                    <h3>Роли:</h3>
                    <ul>
                      <li>GL — GLOBAL_ADMIN (Глобальный администратор)</li>
                      <li>FO — FRANCHISE_OWNER (Владелец франшизы)</li>
                      <li>LA — LOCATION_ADMIN (Локальный администратор)</li>
                      <li>U — USER (Пользователь)</li>
                    </ul>
                    
                    <h3>Полная сводная таблица:</h3>
                    <table border="1" cellpadding="5" cellspacing="0">
                      <tr>
                        <th>Endpoint</th>
                        <th>Действие</th>
                        <th>GL</th>
                        <th>FO</th>
                        <th>LA</th>
                        <th>U</th>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/category</td>
                        <td>Создать категорию</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PATCH /api/v1/lunch/category/{id}/addon</td>
                        <td>Переключить в «Дополнительно»</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>DELETE /api/v1/lunch/category/{id}</td>
                        <td>Удалить категорию (и блюда)</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/dish</td>
                        <td>Создать блюдо</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PUT /api/v1/lunch/dish/{id}</td>
                        <td>Обновить блюдо</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PATCH /api/v1/lunch/dish/{id}/availability</td>
                        <td>Изменение isAvailable</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PATCH /api/v1/lunch/dish/{id}/free-addon</td>
                        <td>Изменение isFreeAddon</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>DELETE /api/v1/lunch/dish/{id}</td>
                        <td>Удаление блюда</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo</td>
                        <td>Создать комбо</td>
                        <td>✅</td><td>✅</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PATCH /api/v1/lunch/combo/{id}/status</td>
                        <td>Изменение isActive</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/venue</td>
                        <td>Создать/обновить карточку</td>
                        <td>❌</td><td>✅</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/venue/my</td>
                        <td>Получить свою карточку</td>
                        <td>❌</td><td>✅</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/venue/{id}</td>
                        <td>Получить карточку по ID</td>
                        <td>✅</td><td>❌</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>DELETE /api/v1/lunch/venue/{id}</td>
                        <td>Удалить карточку</td>
                        <td>✅</td><td>✅</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking</td>
                        <td>Создать бронирование</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/booking/my</td>
                        <td>Мои бронирования</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/booking/{id}</td>
                        <td>Просмотр бронирования</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking/cancel-by-code</td>
                        <td>Отмена по коду</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking/cancel-by-code-and-phone</td>
                        <td>Отмена по коду и телефону</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking/{id}/approve</td>
                        <td>Подтверждение бронирования</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking/{id}/complete</td>
                        <td>Выполнение бронирования</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/booking/{id}/cancel</td>
                        <td>Отмена бронирования админом</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking</td>
                        <td>Создать бронирование комбо</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking/cancel-by-code</td>
                        <td>Отмена комбо по коду</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking/cancel-by-code-and-phone</td>
                        <td>Отмена комбо по коду и телефону</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/combo-booking/by-phone</td>
                        <td>Бронирования по телефону</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/combo-booking/my</td>
                        <td>Мои бронирования комбо</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking/{id}/approve</td>
                        <td>Подтверждение комбо</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking/{id}/complete</td>
                        <td>Выполнение комбо</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/combo-booking/{id}/cancel</td>
                        <td>Отмена комбо админом</td>
                        <td>✅</td><td>✅</td><td>✅</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/onboarding</td>
                        <td>Подать заявку</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/onboarding/my</td>
                        <td>Мои заявки</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>DELETE /api/v1/lunch/onboarding/{id}/cancel</td>
                        <td>Отменить заявку</td>
                        <td>❌</td><td>❌</td><td>❌</td><td>✅</td>
                      </tr>
                      <tr>
                        <td>GET /api/v1/lunch/onboarding/pending</td>
                        <td>Ожидающие заявки</td>
                        <td>✅</td><td>❌</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PUT /api/v1/lunch/onboarding/{id}/approve</td>
                        <td>Одобрить заявку</td>
                        <td>✅</td><td>❌</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>PUT /api/v1/lunch/onboarding/{id}/reject</td>
                        <td>Отклонить заявку</td>
                        <td>✅</td><td>❌</td><td>❌</td><td>❌</td>
                      </tr>
                      <tr>
                        <td>POST /api/v1/lunch/onboarding/assign-location-admin</td>
                        <td>Назначить LOCATION_ADMIN</td>
                        <td>✅</td><td>✅</td><td>❌</td><td>❌</td>
                      </tr>
                    </table>
                    """
    )
    @ApiResponse(responseCode = "200", description = "Информация о ролях")
    public ResponseEntity<Map<String, Object>> getRoleInfo() {
        return ResponseEntity.ok(Map.of(
                "roles", List.of(
                        Map.of("name", "GLOBAL_ADMIN", "short", "GL", "description", "Глобальный администратор — полный доступ ко всем ресурсам"),
                        Map.of("name", "FRANCHISE_OWNER", "short", "FO", "description", "Владелец франшизы — управление своими локациями"),
                        Map.of("name", "LOCATION_ADMIN", "short", "LA", "description", "Локальный администратор — управление конкретной локацией"),
                        Map.of("name", "USER", "short", "U", "description", "Пользователь — авторизованный пользователь")
                ),
                "permissions", List.of(
                        Map.of("endpoint", "POST /api/v1/lunch/category", "action", "Создать категорию", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "PATCH /api/v1/lunch/category/{id}/addon", "action", "Переключить в «Дополнительно»", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "DELETE /api/v1/lunch/category/{id}", "action", "Удалить категорию (и блюда)", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/dish", "action", "Создать блюдо", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "PUT /api/v1/lunch/dish/{id}", "action", "Обновить блюдо", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "PATCH /api/v1/lunch/dish/{id}/availability", "action", "Изменение isAvailable", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "PATCH /api/v1/lunch/dish/{id}/free-addon", "action", "Изменение isFreeAddon", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "DELETE /api/v1/lunch/dish/{id}", "action", "Удаление блюда", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo", "action", "Создать комбо", "roles", List.of("GL", "FO")),
                        Map.of("endpoint", "PATCH /api/v1/lunch/combo/{id}/status", "action", "Изменение isActive", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/venue", "action", "Создать/обновить карточку", "roles", List.of("FO")),
                        Map.of("endpoint", "GET /api/v1/lunch/venue/my", "action", "Получить свою карточку", "roles", List.of("FO")),
                        Map.of("endpoint", "GET /api/v1/lunch/venue/{id}", "action", "Получить карточку по ID", "roles", List.of("GL")),
                        Map.of("endpoint", "DELETE /api/v1/lunch/venue/{id}", "action", "Удалить карточку", "roles", List.of("GL", "FO")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking", "action", "Создать бронирование", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/booking/my", "action", "Мои бронирования", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/booking/{id}", "action", "Просмотр бронирования", "roles", List.of("GL", "FO", "LA", "U")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking/cancel-by-code", "action", "Отмена по коду", "roles", List.of("U")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking/cancel-by-code-and-phone", "action", "Отмена по коду и телефону", "roles", List.of("U")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking/{id}/approve", "action", "Подтверждение бронирования", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking/{id}/complete", "action", "Выполнение бронирования", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/booking/{id}/cancel", "action", "Отмена бронирования админом", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking", "action", "Создать бронирование комбо", "roles", List.of("U")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking/cancel-by-code", "action", "Отмена комбо по коду", "roles", List.of("U")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking/cancel-by-code-and-phone", "action", "Отмена комбо по коду и телефону", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/combo-booking/by-phone", "action", "Бронирования по телефону", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/combo-booking/my", "action", "Мои бронирования комбо", "roles", List.of("U")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking/{id}/approve", "action", "Подтверждение комбо", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking/{id}/complete", "action", "Выполнение комбо", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/combo-booking/{id}/cancel", "action", "Отмена комбо админом", "roles", List.of("GL", "FO", "LA")),
                        Map.of("endpoint", "POST /api/v1/lunch/onboarding", "action", "Подать заявку", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/onboarding/my", "action", "Мои заявки", "roles", List.of("U")),
                        Map.of("endpoint", "DELETE /api/v1/lunch/onboarding/{id}/cancel", "action", "Отменить заявку", "roles", List.of("U")),
                        Map.of("endpoint", "GET /api/v1/lunch/onboarding/pending", "action", "Ожидающие заявки", "roles", List.of("GL")),
                        Map.of("endpoint", "PUT /api/v1/lunch/onboarding/{id}/approve", "action", "Одобрить заявку", "roles", List.of("GL")),
                        Map.of("endpoint", "PUT /api/v1/lunch/onboarding/{id}/reject", "action", "Отклонить заявку", "roles", List.of("GL")),
                        Map.of("endpoint", "POST /api/v1/lunch/onboarding/assign-location-admin", "action", "Назначить LOCATION_ADMIN", "roles", List.of("GL", "FO"))
                )
        ));
    }
}
