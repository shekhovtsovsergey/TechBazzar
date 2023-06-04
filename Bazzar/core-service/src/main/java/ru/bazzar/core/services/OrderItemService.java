package ru.bazzar.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bazzar.core.entities.OrderItem;
import ru.bazzar.core.repositories.OrderItemRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;


    OrderItem save(OrderItem entity) {
        return orderItemRepository.save(entity);
    }
}
