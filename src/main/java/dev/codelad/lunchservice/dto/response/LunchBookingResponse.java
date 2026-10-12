package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.entity.LunchOrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class LunchBookingResponse {

    private UUID id;
    private Long venueId;
    private LocalTime time;
    private String status;
    private Integer totalGuests;
    private String cancelCode;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static LunchBookingResponse from(LunchBooking booking) {
        LunchBookingResponse response = new LunchBookingResponse();
        response.id = booking.getId();
        response.venueId = booking.getVenue().getId();
        response.time = booking.getTime();
        response.status = booking.getStatus().name();
        response.totalGuests = booking.getTotalGuests();
        response.cancelCode = booking.getCancelCode();
        response.totalPrice = calculateTotalPrice(booking);
        response.items = booking.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        response.createdAt = booking.getCreatedAt();
        response.updatedAt = booking.getUpdatedAt();
        return response;
    }

    /**
     * Рассчитать общую сумму по всем позициям заказа.
     */
    private static BigDecimal calculateTotalPrice(LunchBooking booking) {
        if (booking.getItems() == null || booking.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return booking.getItems().stream()
                .map(item -> item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Data
    public static class OrderItemResponse {
        private UUID id;
        private UUID dishId;
        private String dishName;
        private Integer quantity;
        private BigDecimal priceAtOrder;
        private BigDecimal totalPrice;

        public static OrderItemResponse from(LunchOrderItem item) {
            OrderItemResponse response = new OrderItemResponse();
            response.id = item.getId();
            response.dishId = item.getDish().getId();
            response.dishName = item.getDish().getName();
            response.quantity = item.getQuantity();
            response.priceAtOrder = item.getPriceAtOrder();
            response.totalPrice = item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity()));
            return response;
        }
    }
}
