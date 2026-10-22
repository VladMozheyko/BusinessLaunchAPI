package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Category;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Setter
@Getter
public class CategoryResponse {

    private Long id;
    private String name;
    private Long venueId;
    private boolean hidden;
    private Instant createdAt;
    private Instant updatedAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        response.venueId = category.getVenueId();
        response.hidden = category.getHidden();
        response.createdAt = category.getCreatedAt();
        response.updatedAt = category.getUpdatedAt();
        return response;
    }

    public static List<CategoryResponse> from(List<Category> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
