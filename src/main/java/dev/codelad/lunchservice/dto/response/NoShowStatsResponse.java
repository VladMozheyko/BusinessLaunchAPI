package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.LunchBooking;
import lombok.Data;

import java.util.List;

/**
 * Ответ со статистикой неяв пользователя.
 */
@Data
public class NoShowStatsResponse {

    private String userId;
    private String phoneNumber;
    private Integer noShowCount;
    private List<LunchBooking> bookingHistory;

    public static NoShowStatsResponse from(LunchBooking booking) {
        NoShowStatsResponse response = new NoShowStatsResponse();
        response.userId = booking.getUserId();
        response.phoneNumber = booking.getPhoneNumber();
        response.noShowCount = booking.getNoShowCount();
        response.bookingHistory = List.of(booking);
        return response;
    }
}
