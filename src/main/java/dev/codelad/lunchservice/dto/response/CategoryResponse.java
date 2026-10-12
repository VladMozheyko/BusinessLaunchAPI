package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Category;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class CategoryResponse {

    private Long id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        response.createdAt = category.getCreatedAt();
        response.updatedAt = category.getUpdatedAt();
        return response;
    }

}
