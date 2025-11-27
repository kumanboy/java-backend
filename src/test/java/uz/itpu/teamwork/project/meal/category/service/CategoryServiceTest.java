package uz.itpu.teamwork.project.meal.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.itpu.teamwork.project.meal.category.dto.request.CategoryRequest;
import uz.itpu.teamwork.project.meal.category.dto.response.CategoryResponse;
import uz.itpu.teamwork.project.meal.category.model.Category;
import uz.itpu.teamwork.project.meal.category.repository.CategoryRepository;
import uz.itpu.teamwork.project.exception.ResourceAlreadyExistsException;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_WhenNameExists_ShouldThrowConflict() {
        CategoryRequest request = buildRequest();
        when(categoryRepository.existsByName(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Category")
                .hasMessageContaining("name");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void create_WhenValid_ShouldPersistAndReturnResponse() {
        CategoryRequest request = buildRequest();
        Category saved = Category.builder()
                .id(11L)
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(3)
                .isActive(true)
                .build();

        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getDescription()).isEqualTo(request.getDescription());
    }

    @Test
    void update_WhenNameConflicts_ShouldThrow() {
        CategoryRequest request = buildRequest();
        Category existing = Category.builder().id(5L).name("Old").description("desc").isActive(true).build();
        Category other = Category.builder().id(7L).name(request.getName()).build();

        when(categoryRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(categoryRepository.findByName(request.getName())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> categoryService.update(existing.getId(), request))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(categoryRepository, never()).save(existing);
    }

    @Test
    void delete_WhenMissing_ShouldThrowNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CategoryRequest buildRequest() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Breakfast");
        request.setDescription("Morning menu");
        request.setDisplayOrder(3);
        request.setIsActive(true);
        return request;
    }
}
