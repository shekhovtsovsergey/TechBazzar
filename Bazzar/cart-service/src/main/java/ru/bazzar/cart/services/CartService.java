package ru.bazzar.cart.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bazzar.cart.api.ProductDto;
import ru.bazzar.cart.integrations.ProductServiceIntegration;
import ru.bazzar.cart.model.Cart;

import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class CartService {
    private final ProductServiceIntegration productServiceIntegration;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cart-service.cart-prefix}")
    private String cartPrefix;

    public Cart getCurrentCart(String username) {
        String uuid = cartPrefix + username;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(uuid))) {
            redisTemplate.opsForValue().set(uuid, new Cart());
        }
        return (Cart)redisTemplate.opsForValue().get(uuid);
    }

    public void add(String username, Long productId) {
        execute(username, cart -> cart.add(productServiceIntegration.getProductById(productId)));
    }

    public void remove(String username, Long productId) {
        execute(username, cart -> cart.remove(productId));
    }

    public void clear(String username) {
        execute(username, Cart::clear);
    }

    public void changeQuantity(String username, Long productId, Integer delta) {
        ProductDto product = productServiceIntegration.getProductById(productId);
        execute(username, cart -> cart.changeQuantity(product,delta));
    }
    private void execute(String username, Consumer<Cart> operation) {
        Cart cart = getCurrentCart(username);
        operation.accept(cart);
        redisTemplate.opsForValue().set(cartPrefix + username, cart);
    }

    public Integer getCartSize(String username) {
        Cart cart = getCurrentCart(username);
        return cart.getItems().size();
    }

    public void updateCart(String uuid, Cart cart) {
        redisTemplate.opsForValue().set(cartPrefix + uuid, cart);
    }
}
