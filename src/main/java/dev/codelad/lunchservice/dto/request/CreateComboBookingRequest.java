package dev.codelad.lunchservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/**
 * Запрос на создание бронирования комбо-набора.
 */
@Setter
@Getter
public class CreateComboBookingRequest {

    // Getters and Setters
    @NotNull(message = "ID заведения не может быть пустым")
    private Long venueId;

    @NotNull(message = "Время не может быть пустым")
    private LocalTime time;

    @NotEmpty(message = "Должна быть хотя бы одна позиция комбо")
    private List<ComboItemRequest> items;

    private String phoneNumber;
    private String customerName;
    private String comment;

    /**
     * Запрос на позицию комбо.
     */
    @Setter
    @Getter
    public static class ComboItemRequest {
        @NotNull(message = "ID комбо не может быть пустым")
        private Long comboId;

        @NotNull(message = "Количество не может быть пустым")
        private Integer quantity;

        private List<ComboAddonRequest> addons;

    }

    /**
     * Запрос на дополнение к позиции комбо.
     */
    @Setter
    @Getter
    public static class ComboAddonRequest {
        @NotNull(message = "ID блюда не может быть пустым")
        private java.util.UUID dishId;

        @NotNull(message = "Количество не может быть пустым")
        private Integer quantity;

    }
}
