package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Запрос на создание/обновление карточки заведения.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание/обновление карточки заведения")
public class VenueProfileRequest {

    @NotNull(message = "venueId обязателен")
    @Schema(description = "ID заведения (уникальный идентификатор в системе)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long venueId;

    @NotBlank(message = "name обязателен")
    @Size(max = 100, message = "name должен содержать не более 100 символов")
    @Schema(description = "Название бизнес-ланча", example = "Ланчи в офисе", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "description должен содержать не более 500 символов")
    @Schema(description = "Дополнительное описание", example = "Вкусные бизнес-ланчи каждый день", maxLength = 500)
    private String description;

    @Schema(description = "URL логотипа", example = "https://example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Расписание работы по дням недели (Пн-Вс)")
    private List<ScheduleItem> schedules;

    /**
     * Элемент расписания на один день.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Расписание на один день")
    public static class ScheduleItem {

        @NotBlank(message = "dayOfWeek обязателен")
        @Schema(description = "День недели", example = "MONDAY", requiredMode = Schema.RequiredMode.REQUIRED)
        private String dayOfWeek;

        @Schema(description = "Работает ли день", example = "true")
        private Boolean working;

        @Schema(description = "Время начала", example = "12:00:00")
        private LocalTime startTime;

        @Schema(description = "Время окончания", example = "17:00:00")
        private LocalTime endTime;
    }
}
