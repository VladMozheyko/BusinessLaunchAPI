package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Запрос на создание бронирования бизнес-ланча.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание бронирования бизнес-ланча")
public class CreateLunchBookingRequest {

    @NotNull(message = "venueId обязателен")
    @Schema(description = "ID заведения", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long venueId;

    @NotNull(message = "time обязателен")
    @Schema(description = "Время получения заказа", example = "13:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime time;

    @NotEmpty(message = "Список позиций не может быть пустым")
    @Valid
    @Schema(description = "Список выбранных блюд с количеством", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemDto> items;

    /**
     * Позиция заказа (одно блюдо из категории).
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Позиция заказа: категория, блюдо и количество")
    public static class OrderItemDto {

        @NotNull(message = "categoryId обязателен")
        @Schema(description = "ID категории блюда", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long categoryId;

        @NotNull(message = "dishId обязателен")
        @Schema(description = "ID блюда", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
        private UUID dishId;

        @NotNull(message = "quantity обязателен")
        @Schema(description = "Количество порций", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;
    }
}
