package uz.itpu.teamwork.project.meal.product.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.itpu.teamwork.project.meal.product.dto.request.IngredientRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.IngredientResponse;
import uz.itpu.teamwork.project.meal.product.model.Ingredient;
import uz.itpu.teamwork.project.meal.product.repository.IngredientRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    @Test
    void create_WhenDuplicateName_ShouldThrow() {
        IngredientRequest request = buildRequest();
        when(ingredientRepository.existsByName(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> ingredientService.createIngredient(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void create_WhenValid_ShouldReturnResponse() {
        IngredientRequest request = buildRequest();
        Ingredient saved = Ingredient.builder()
                .id(4L)
                .name(request.getName())
                .isAllergen(request.getIsAllergen())
                .createdAt(LocalDateTime.now())
                .build();

        when(ingredientRepository.existsByName(request.getName())).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(saved);

        IngredientResponse response = ingredientService.createIngredient(request);

        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getName()).isEqualTo(request.getName());
    }

    @Test
    void update_WhenIngredientMissing_ShouldThrow() {
        IngredientRequest request = buildRequest();
        when(ingredientRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.updateIngredient(55L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ingredient not found");
    }

    @Test
    void delete_WhenMissing_ShouldThrow() {
        when(ingredientRepository.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> ingredientService.deleteIngredient(77L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ingredient not found");
    }

    @Test
    void getAll_ShouldMapEntitiesToResponses() {
        Ingredient milk = Ingredient.builder().id(1L).name("Milk").isAllergen(false).build();
        Ingredient nuts = Ingredient.builder().id(2L).name("Almond").isAllergen(true).build();
        when(ingredientRepository.findAll()).thenReturn(List.of(milk, nuts));

        List<IngredientResponse> responses = ingredientService.getAllIngredients();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(IngredientResponse::getName).containsExactly("Milk", "Almond");
    }

    private IngredientRequest buildRequest() {
        IngredientRequest request = new IngredientRequest();
        request.setName("Whole Milk");
        request.setIsAllergen(false);
        return request;
    }
}
