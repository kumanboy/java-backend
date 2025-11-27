package uz.itpu.teamwork.project.meal.product.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uz.itpu.teamwork.project.meal.category.model.Category;
import uz.itpu.teamwork.project.meal.category.repository.CategoryRepository;
import uz.itpu.teamwork.project.meal.country.model.Country;
import uz.itpu.teamwork.project.meal.country.repository.CountryRepository;
import uz.itpu.teamwork.project.meal.product.dto.request.ProductIngredientRequest;
import uz.itpu.teamwork.project.meal.product.dto.request.ProductRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ProductResponse;
import uz.itpu.teamwork.project.meal.product.model.Ingredient;
import uz.itpu.teamwork.project.meal.product.model.Modifier;
import uz.itpu.teamwork.project.meal.product.model.Product;
import uz.itpu.teamwork.project.meal.product.model.ProductAvailability;
import uz.itpu.teamwork.project.meal.product.model.ProductIngredient;
import uz.itpu.teamwork.project.meal.product.repository.IngredientRepository;
import uz.itpu.teamwork.project.meal.product.repository.ModifierRepository;
import uz.itpu.teamwork.project.meal.product.repository.ProductRepository;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private ModifierRepository modifierRepository;
    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_ShouldPopulateIngredientsAvailabilityAndModifiers() {
        ProductRequest request = new ProductRequest();
        request.setName("Deluxe Burger");
        request.setDescription("Stacked burger");
        request.setCategoryId(2L);
        request.setBasePrice(new BigDecimal("12.50"));
        request.setIsActive(true);

        ProductIngredientRequest ingredientRequest = new ProductIngredientRequest();
        ingredientRequest.setIngredientId(3L);
        ingredientRequest.setQuantity("2 slices");
        request.setIngredients(List.of(ingredientRequest));
        request.setAvailability(Map.of("US", true));
        request.setModifierIds(List.of(7L));

        Category category = Category.builder().id(2L).name("Burgers").build();
        Ingredient ingredient = Ingredient.builder().id(3L).name("Cheese").isAllergen(false).build();
        Country country = Country.builder().id(1L).code("US").name("United States").build();
        Modifier modifier = Modifier.builder().id(7L).name("Extra Cheese").priceAdjustment(BigDecimal.ONE).build();

        when(categoryRepository.findById(request.getCategoryId())).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getId() == null) {
                product.setId(15L);
            }
            return product;
        });
        when(ingredientRepository.findById(ingredientRequest.getIngredientId())).thenReturn(Optional.of(ingredient));
        when(countryRepository.findByCode("US")).thenReturn(Optional.of(country));
        when(modifierRepository.findAllById(request.getModifierIds())).thenReturn(List.of(modifier));

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getName()).isEqualTo("Deluxe Burger");
        assertThat(response.getCategoryId()).isEqualTo(category.getId());
        assertThat(response.getCategoryName()).isEqualTo(category.getName());
        assertThat(response.getIngredients()).hasSize(1);
        assertThat(response.getIngredients().get(0).getId()).isEqualTo(ingredient.getId());
        assertThat(response.getAvailability()).containsKey("US");
        assertThat(response.getAvailability().get("US").getIsAvailable()).isTrue();
        assertThat(response.getModifiers()).hasSize(1);
        assertThat(response.getModifiers().get(0).getId()).isEqualTo(modifier.getId());

        verify(modifierRepository).findAllById(request.getModifierIds());
    }

    @Test
    void filterProducts_ShouldApplySpecifications_AndCountryFilter() {
        Product availableInUsa = buildProduct(1L, "US", true);
        Product notAvailable = buildProduct(2L, "US", false);

        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(availableInUsa, notAvailable));

        List<ProductResponse> responses = productService.filterProducts(
                5L,
                new BigDecimal("5.00"),
                new BigDecimal("25.00"),
                "US",
                true
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(availableInUsa.getId());
    }

    @Test
    void deleteProduct_WhenProductMissing_ShouldThrowException() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: '99'");

        verify(productRepository, never()).deleteById(anyLong());
    }

    private Product buildProduct(Long id, String countryCode, boolean available) {
        Category category = Category.builder().id(3L).name("Category").build();
        Product product = Product.builder()
                .id(id)
                .name("Product " + id)
                .description("Description")
                .category(category)
                .basePrice(new BigDecimal("10.00"))
                .imageUrl("image.png")
                .isActive(true)
                .build();

        Ingredient ingredient = Ingredient.builder().id(id).name("Ingredient " + id).isAllergen(false).build();
        product.getProductIngredients().add(
                ProductIngredient.builder()
                        .product(product)
                        .ingredient(ingredient)
                        .quantity("1 unit")
                        .build()
        );

        Country country = Country.builder().code(countryCode).name("Country").build();
        product.getProductAvailabilities().add(
                ProductAvailability.builder()
                        .product(product)
                        .country(country)
                        .isAvailable(available)
                        .stockQuantity(5)
                        .build()
        );

        product.getModifiers().add(
                Modifier.builder()
                        .id(id)
                        .name("Modifier")
                        .priceAdjustment(BigDecimal.ONE)
                        .isActive(true)
                        .build()
        );

        return product;
    }
}
