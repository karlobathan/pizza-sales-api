package com.karlobathan.pizzasales.api.mapper;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.domain.OrderItem;
import com.karlobathan.pizzasales.api.dto.OrderItemResponse;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps order entities to API responses. Callers must have fetched each item's pizza and its pizza type, as they
 * are lazy. Prices come from the pizza's current price; the dataset has no price history.
 */
@Mapper(componentModel = "spring", uses = PizzaMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface OrderMapper {

    OrderSummaryResponse toSummary(Order order);

    @Mapping(target = "lineTotal", expression = "java(lineTotal(orderItem))")
    OrderItemResponse toResponse(OrderItem orderItem);

    default OrderResponse toResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> items = orderItems.stream().map(this::toResponse).toList();
        int totalQuantity = items.stream().mapToInt(OrderItemResponse::quantity).sum();
        BigDecimal totalPrice = items.stream().map(OrderItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getOrderDate(), order.getOrderTime(), items, totalQuantity, totalPrice);
    }

    default BigDecimal lineTotal(OrderItem orderItem) {
        return orderItem.getPizza().getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }
}
