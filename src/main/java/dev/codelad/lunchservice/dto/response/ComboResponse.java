package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.Combo;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ComboResponse {

    private Long id;
    private Long venueId;
    private String name;
    private Integer price;
    private boolean isActive;
    private Integer dailyLimit;
    private List<Long> categoryIds;
    private List<DishResponse> freeAddons;
    private List<DishResponse> addons;
    private Instant createdAt;
    private Instant updatedAt;

    public static ComboResponse from(Combo combo) {
        ComboResponse response = new ComboResponse();
        response.id = combo.getId();
        response.venueId = combo.getVenueId();
        response.name = combo.getName();
        response.price = combo.getPrice();
        response.isActive = combo.isActive();
        response.dailyLimit = combo.getDailyLimit();
        response.categoryIds = combo.getCategories().stream()
                .map(Category::getId)
                .toList();
        response.freeAddons = combo.getFreeAddons().stream()
                .map(DishResponse::from)
                .toList();
        response.addons = combo.getAddons().stream()
                .map(DishResponse::from)
                .toList();
        response.createdAt = combo.getCreatedAt();
        response.updatedAt = combo.getUpdatedAt();
        return response;
    }
}
