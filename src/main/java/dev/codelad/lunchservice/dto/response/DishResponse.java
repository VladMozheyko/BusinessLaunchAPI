package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Dish;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class DishResponse {

    private UUID id;
    private Long categoryId;
    private String name;
    private Integer weight;
    private String weightUnit;
    private Integer price;
    private boolean isAvailable;
    private Integer quantity;
    private boolean isFreeAddon;
    private Instant createdAt;
    private Instant updatedAt;

    public static DishResponse from(Dish dish) {
        DishResponse response = new DishResponse();
        response.id = dish.getId();
        response.categoryId = dish.getCategoryId();
        response.name = dish.getName();
        response.weight = dish.getWeight();
        response.weightUnit = dish.getWeightUnit();
        response.price = dish.getPrice();
        response.isAvailable = dish.isAvailable();
        response.quantity = dish.getQuantity();
        response.isFreeAddon = dish.isFreeAddon();
        response.createdAt = dish.getCreatedAt();
        response.updatedAt = dish.getUpdatedAt();
        return response;
    }
}
