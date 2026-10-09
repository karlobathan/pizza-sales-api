package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi.sourceOrderDetailsId from OrderItem oi where oi.sourceOrderDetailsId in :sourceOrderDetailsIds")
    Set<Long> findSourceOrderDetailsIdIn(Collection<Long> sourceOrderDetailsIds);

    // fetches each item's pizza and its pizza type in the same query, as both are lazy and the API maps them
    @EntityGraph(attributePaths = {"pizza", "pizza.pizzaType"})
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
