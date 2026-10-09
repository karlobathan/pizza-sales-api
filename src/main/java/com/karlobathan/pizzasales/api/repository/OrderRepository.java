package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o.sourceOrderId from Order o where o.sourceOrderId in :sourceOrderIds")
    Set<Long> findSourceOrderIdIn(Collection<Long> sourceOrderIds);

    List<Order> findBySourceOrderIdIn(Collection<Long> sourceOrderIds);
}
