package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.domain.OrderItem;
import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.dto.OrderItemRequest;
import com.karlobathan.pizzasales.api.dto.OrderRequest;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.OrderMapper;
import com.karlobathan.pizzasales.api.repository.OrderItemRepository;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link OrderService} backed by the database. Searching runs one query for the page and one to count; reading
 * an order runs one query for the order and one for its items with their pizzas and pizza types. Creating an order
 * checks every referenced pizza in one query before saving anything, so an invalid item saves nothing.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultOrderService implements OrderService {

    // id last so orders placed at the same moment still come back in a stable order across pages
    static final Sort ORDER_SORT = Sort.by("orderDate", "orderTime", "id");

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PizzaRepository     pizzaRepository;
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

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request) {
        Map<Long, Pizza> pizzas = findPizzas(request.items());
        Order order = orderRepository.save(Order.builder()
                .orderDate(request.orderDate())
                .orderTime(request.orderTime())
                .build());
        List<OrderItem> items = orderItemRepository.saveAll(request.items()
                .stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .pizza(pizzas.get(item.pizzaId()))
                        .quantity(item.quantity())
                        .build())
                .toList());
        return orderMapper.toResponse(order, items);
    }

    // every referenced pizza, with its pizza type for the response, in one query; fails before anything is saved
    private Map<Long, Pizza> findPizzas(List<OrderItemRequest> items) {
        Set<Long> pizzaIds = items.stream().map(OrderItemRequest::pizzaId).collect(Collectors.toSet());
        Map<Long, Pizza> pizzas = pizzaRepository.findWithPizzaTypeByIdIn(pizzaIds)
                .stream()
                .collect(Collectors.toMap(Pizza::getId, Function.identity()));

        Set<Long> unknownIds = new HashSet<>(pizzaIds);
        unknownIds.removeAll(pizzas.keySet());
        if (!unknownIds.isEmpty()) {
            throw InvalidRequestException.unknownPizzas(unknownIds);
        }
        return pizzas;
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
