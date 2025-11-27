package uz.itpu.teamwork.project.meal.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.meal.cart.dto.request.AddToCartRequest;
import uz.itpu.teamwork.project.meal.cart.dto.request.UpdateCartItemRequest;
import uz.itpu.teamwork.project.meal.cart.dto.response.CartItemResponse;
import uz.itpu.teamwork.project.meal.cart.dto.response.CartResponse;
import uz.itpu.teamwork.project.meal.cart.model.Cart;
import uz.itpu.teamwork.project.meal.cart.model.CartItem;
import uz.itpu.teamwork.project.meal.cart.repository.CartItemRepository;
import uz.itpu.teamwork.project.meal.cart.repository.CartRepository;
import uz.itpu.teamwork.project.meal.product.model.Product;
import uz.itpu.teamwork.project.meal.product.repository.ProductRepository;
import uz.itpu.teamwork.project.exception.BadRequestException;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(User user, String sessionId) {
        Cart cart = findOrCreateCart(user, sessionId);
        return buildCartResponse(cart);
    }

    public CartResponse addToCart(User user, String sessionId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }

        Cart cart = findOrCreateCart(user, sessionId);

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceSnapshot(product.getBasePrice())
                    .build();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    public CartResponse updateCartItem(User user, String sessionId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = findCart(user, sessionId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to this cart");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    public CartResponse removeCartItem(User user, String sessionId, Long itemId) {
        Cart cart = findCart(user, sessionId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to this cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return buildCartResponse(cart);
    }

    public void clearCart(User user, String sessionId) {
        Cart cart = findCart(user, sessionId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.clearItems();
    }

    private Cart findOrCreateCart(User user, String sessionId) {
        if (user != null) {
            log.debug("=== FINDING CART FOR AUTHENTICATED USER: {} (ID: {}) ===",
                    user.getEmail(), user.getId());

            Cart userCart = cartRepository.findByUserId(user.getId()).orElse(null);

            if (userCart != null) {
                log.debug("Found existing user cart: ID={}, items={}",
                        userCart.getId(), userCart.getItems().size());
                return userCart;
            }

            log.debug("No cart found for user, creating new one");
            Cart newCart = Cart.builder()
                    .user(user)
                    .sessionId(null)
                    .build();

            Cart savedCart = cartRepository.save(newCart);
            log.debug("Created new user cart: ID={}", savedCart.getId());
            return savedCart;
        }

        log.debug("=== FINDING CART FOR GUEST: session={} ===", sessionId);

        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            log.debug("Generated new session ID: {}", sessionId);
        }

        Cart guestCart = cartRepository.findBySessionId(sessionId).orElse(null);

        if (guestCart != null) {
            log.debug("Found existing guest cart: ID={}, items={}",
                    guestCart.getId(), guestCart.getItems().size());
            return guestCart;
        }

        log.debug("No cart found for guest, creating new one");
        Cart newCart = Cart.builder()
                .user(null)
                .sessionId(sessionId)
                .build();

        Cart savedCart = cartRepository.save(newCart);
        log.debug("Created new guest cart: ID={}, session={}", savedCart.getId(), sessionId);
        return savedCart;
    }

    private Cart findCart(User user, String sessionId) {
        if (user != null) {
            log.debug("Finding cart for user: {}", user.getEmail());
            return cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> {
                        log.error("Cart not found for user: {}", user.getEmail());
                        return new ResourceNotFoundException("Cart not found");
                    });
        }

        log.debug("Finding cart for guest session: {}", sessionId);
        return cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for session: {}", sessionId);
                    return new ResourceNotFoundException("Cart not found");
                });
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal totalPrice = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    private CartItemResponse buildCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getPriceSnapshot())
                .subtotal(item.getSubtotal())
                .build();
    }
}