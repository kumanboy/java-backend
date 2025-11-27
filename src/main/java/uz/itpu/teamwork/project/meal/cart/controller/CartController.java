package uz.itpu.teamwork.project.meal.cart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.itpu.teamwork.project.auth.security.UserPrincipal;
import uz.itpu.teamwork.project.meal.cart.dto.request.AddToCartRequest;
import uz.itpu.teamwork.project.meal.cart.dto.request.UpdateCartItemRequest;
import uz.itpu.teamwork.project.meal.cart.dto.response.CartResponse;
import uz.itpu.teamwork.project.meal.cart.service.CartService;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.auth.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpSession session) {

        User user = getUserFromPrincipal(userPrincipal);
        String sessionId = getOrCreateSessionId(session, user);

        log.debug("Getting cart for user: {}, session: {}",
                user != null ? user.getEmail() : "guest", sessionId);

        CartResponse cart = cartService.getCart(user, sessionId);
        return ResponseEntity.ok(success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addToCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AddToCartRequest request,
            HttpSession session) {

        User user = getUserFromPrincipal(userPrincipal);
        String sessionId = getOrCreateSessionId(session, user);

        log.debug("Adding to cart for user: {}, session: {}, productId: {}",
                user != null ? user.getEmail() : "guest", sessionId, request.getProductId());

        CartResponse cart = cartService.addToCart(user, sessionId, request);
        return ResponseEntity.ok(success(cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpSession session) {

        User user = getUserFromPrincipal(userPrincipal);
        String sessionId = getOrCreateSessionId(session, user);

        log.debug("Updating cart item {} for user: {}, session: {}",
                itemId, user != null ? user.getEmail() : "guest", sessionId);

        CartResponse cart = cartService.updateCartItem(user, sessionId, itemId, request);
        return ResponseEntity.ok(success(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long itemId,
            HttpSession session) {

        User user = getUserFromPrincipal(userPrincipal);
        String sessionId = getOrCreateSessionId(session, user);

        log.debug("Removing cart item {} for user: {}, session: {}",
                itemId, user != null ? user.getEmail() : "guest", sessionId);

        CartResponse cart = cartService.removeCartItem(user, sessionId, itemId);
        return ResponseEntity.ok(success(cart));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpSession session) {

        User user = getUserFromPrincipal(userPrincipal);
        String sessionId = getOrCreateSessionId(session, user);

        log.debug("Clearing cart for user: {}, session: {}",
                user != null ? user.getEmail() : "guest", sessionId);

        cartService.clearCart(user, sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cart cleared");
        return ResponseEntity.ok(response);
    }

    private User getUserFromPrincipal(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return null;
        }

        return userRepository.findById(userPrincipal.getId()).orElse(null);
    }

    private String getOrCreateSessionId(HttpSession session, User user) {
        if (user != null) {
            log.debug("User {} is authenticated, session ID not needed", user.getEmail());
            return null;
        }

        String sessionId = (String) session.getAttribute("cart_session");

        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute("cart_session", sessionId);
            log.debug("Created new guest cart session: {}", sessionId);
        } else {
            log.debug("Using existing guest cart session: {}", sessionId);
        }

        return sessionId;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
}