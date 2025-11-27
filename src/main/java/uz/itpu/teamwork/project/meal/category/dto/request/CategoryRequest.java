package uz.itpu.teamwork.project.meal.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean isActive = true;
}