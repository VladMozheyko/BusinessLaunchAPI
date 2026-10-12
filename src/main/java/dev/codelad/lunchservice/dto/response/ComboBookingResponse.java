package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.ComboAddon;
import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.entity.ComboBookingItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Ответ с информацией о бронировании комбо.
 */
@Setter
@Getter
public class ComboBookingResponse {

    private UUID id;
    private Long venueId;
    private LocalTime time;
    private String status;
    private String cancelCode;
    private String phoneNumber;
    private String customerName;
    private String comment;
    private Integer totalGuests;
    private BigDecimal totalPrice;
    private List<ComboItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static ComboBookingResponse from(ComboBooking booking) {
        ComboBookingResponse response = new ComboBookingResponse();
        response.id = booking.getId();
        response.venueId = booking.getVenueId();
        response.time = booking.getTime();
        response.status = booking.getStatus().name();
        response.cancelCode = booking.getCancelCode();
        response.phoneNumber = booking.getPhoneNumber();
        response.customerName = booking.getCustomerName();
        response.comment = booking.getComment();
        response.totalGuests = booking.getTotalGuests();
        response.totalPrice = calculateTotalPrice(booking);
        response.items = booking.getItems().stream()
                .map(ComboItemResponse::from)
                .toList();
        response.createdAt = booking.getCreatedAt();
        response.updatedAt = booking.getUpdatedAt();
        return response;
    }

    private static BigDecimal calculateTotalPrice(ComboBooking booking) {
        if (booking.getItems() == null || booking.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return booking.getItems().stream()
                .map(item -> {
                    BigDecimal comboTotal = BigDecimal.valueOf(item.getPriceAtOrder())
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal addonsTotal = item.getAddons().stream()
                            .map(addon -> BigDecimal.valueOf(addon.getPriceAtOrder())
                                    .multiply(BigDecimal.valueOf(addon.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return comboTotal.add(addonsTotal);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Ответ с позицией комбо.
     */
    @Getter
    @Setter
    public static class ComboItemResponse {
        private Long comboId;
        private String comboName;
        private Integer quantity;
        private Integer price;
        private BigDecimal totalPrice;
        private List<ComboAddonResponse> addons;

        public static ComboItemResponse from(ComboBookingItem item) {
            ComboItemResponse response = new ComboItemResponse();
            response.comboId = item.getComboId();
            response.comboName = item.getComboName();
            response.quantity = item.getQuantity();
            response.price = item.getPriceAtOrder();
            response.totalPrice = BigDecimal.valueOf(item.getPriceAtOrder())
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            response.addons = item.getAddons().stream()
                    .map(ComboAddonResponse::from)
                    .toList();
            return response;
        }
    }

    /**
     * Ответ с дополнением.
     */
    @Getter
    @Setter
    public static class ComboAddonResponse {
        private UUID dishId;
        private String dishName;
        private Integer quantity;
        private Integer priceAtOrder;
        private BigDecimal totalPrice;

        public static ComboAddonResponse from(ComboAddon addon) {
            ComboAddonResponse response = new ComboAddonResponse();
            response.dishId = addon.getDishId();
            response.dishName = addon.getDishName();
            response.quantity = addon.getQuantity();
            response.priceAtOrder = addon.getPriceAtOrder();
            response.totalPrice = BigDecimal.valueOf(addon.getPriceAtOrder())
                    .multiply(BigDecimal.valueOf(addon.getQuantity()));
            return response;
        }
    }
}
