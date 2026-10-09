package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.OrderMapper;
import com.karlobathan.pizzasales.api.repository.OrderItemRepository;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link OrderService} backed by the database. Searching runs one query for the page and one to count; reading
 * an order runs one query for the order and one for its items with their pizzas and pizza types.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultOrderService implements OrderService {

    // id last so orders placed at the same moment still come back in a stable order across pages
    static final Sort ORDER_SORT = Sort.by("orderDate", "orderTime", "id");

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper         orderMapper;

    @Override
    public PageResponse<OrderSummaryResponse> findAll(LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw InvalidRequestException.fromAfterTo(from, to);
        }

        return PageResponse.of(orderRepository.findAll(orderDateBetween(from, to), PageRequest.of(page, size, ORDER_SORT))
                .map(orderMapper::toSummary));
    }

    @Override
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiResources.ORDER, id));
        return orderMapper.toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(id));
    }

    // inclusive on both ends; a null bound is left out, so no bounds matches every order
    private static Specification<Order> orderDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
