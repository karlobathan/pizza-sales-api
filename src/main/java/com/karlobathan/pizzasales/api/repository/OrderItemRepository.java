package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi.sourceOrderDetailsId from OrderItem oi where oi.sourceOrderDetailsId in :sourceOrderDetailsIds")
    Set<Long> findSourceOrderDetailsIdIn(Collection<Long> sourceOrderDetailsIds);
}
