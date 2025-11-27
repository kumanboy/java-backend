package uz.itpu.teamwork.project.meal.product.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.itpu.teamwork.project.meal.product.dto.request.ModifierRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ModifierResponse;
import uz.itpu.teamwork.project.meal.product.model.Modifier;
import uz.itpu.teamwork.project.meal.product.repository.ModifierRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModifierServiceTest {

    @Mock
    private ModifierRepository modifierRepository;

    @InjectMocks
    private ModifierService modifierService;

    @Test
    void create_ShouldPersistModifier() {
        ModifierRequest request = buildRequest();
        Modifier saved = Modifier.builder()
                .id(9L)
                .name(request.getName())
                .priceAdjustment(request.getPriceAdjustment())
                .isActive(true)
                .build();

        when(modifierRepository.save(any(Modifier.class))).thenReturn(saved);

        ModifierResponse response = modifierService.createModifier(request);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getPriceAdjustment()).isEqualTo(BigDecimal.valueOf(1.25));
    }

    @Test
    void update_WhenModifierMissing_ShouldThrow() {
        when(modifierRepository.findById(33L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifierService.updateModifier(33L, buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Modifier not found with id: 33");
    }

    @Test
    void delete_WhenMissing_ShouldThrow() {
        when(modifierRepository.existsById(44L)).thenReturn(false);

        assertThatThrownBy(() -> modifierService.deleteModifier(44L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Modifier not found with id: 44");
    }

    @Test
    void getActiveModifiers_ShouldReturnOnlyActive() {
        Modifier active = Modifier.builder().id(1L).name("Extra Shot").isActive(true).build();
        Modifier inactive = Modifier.builder().id(2L).name("Unavailable").isActive(false).build();

        when(modifierRepository.findByIsActiveTrue()).thenReturn(List.of(active));
        when(modifierRepository.findAll()).thenReturn(List.of(active, inactive));

        assertThat(modifierService.getActiveModifiers()).hasSize(1);
        assertThat(modifierService.getAllModifiers()).hasSize(2);
    }

    private ModifierRequest buildRequest() {
        ModifierRequest request = new ModifierRequest();
        request.setName("Extra Shot");
        request.setDescription("Add 30ml espresso");
        request.setPriceAdjustment(BigDecimal.valueOf(1.25));
        request.setIsActive(true);
        return request;
    }
}
