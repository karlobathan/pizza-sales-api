package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.domain.OrderItem;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.OrderMapper;
import com.karlobathan.pizzasales.api.repository.OrderItemRepository;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private DefaultOrderService service;

    @Test
    @DisplayName("findAll requests the page sorted by date, time and id and maps it to a page response")
    void findAll_requestsSortedPageAndMapsIt() {
        Order order = Order.builder().id(1L).build();
        OrderSummaryResponse summary = new OrderSummaryResponse(1L, LocalDate.of(2015, 1, 1), LocalTime.NOON);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(order), invocation.getArgument(1), 41));
        when(orderMapper.toSummary(order)).thenReturn(summary);

        PageResponse<OrderSummaryResponse> result = service.findAll(null, null, 2, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(2, 20, Sort.by("orderDate", "orderTime", "id")));
        assertThat(result).isEqualTo(new PageResponse<>(List.of(summary), 2, 20, 41, 3));
    }

    @Test
    @DisplayName("findAll accepts a range whose from and to are the same day")
    void findAll_acceptsSingleDayRange() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(), invocation.getArgument(1), 0));
        LocalDate day = LocalDate.of(2015, 1, 1);

        assertThat(service.findAll(day, day, 0, 20).content()).isEmpty();
    }

    @Test
    @DisplayName("findAll rejects a range whose from is after to without querying")
    void findAll_rejectsFromAfterTo() {
        assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> service.findAll(LocalDate.of(2015, 12, 31),
                LocalDate.of(2015, 1, 1),
                0,
                20
        )).withMessage("'from' (2015-12-31) must not be after 'to' (2015-01-01)");
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("findById maps the order with its items")
    void findById_mapsOrderWithItems() {
        Order order = Order.builder().id(1L).build();
        List<OrderItem> items = List.of(OrderItem.builder().id(10L).build());
        OrderResponse response = new OrderResponse(1L, LocalDate.of(2015, 1, 1), LocalTime.NOON, List.of(), 0, BigDecimal.ZERO);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(items);
        when(orderMapper.toResponse(order, items)).thenReturn(response);

        assertThat(service.findById(1L)).isSameAs(response);
    }

    @Test
    @DisplayName("findById throws resource not found naming the order and id without loading items")
    void findById_throwsResourceNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.findById(99L))
                .withMessage("Order with id 99 not found");
        verifyNoInteractions(orderItemRepository, orderMapper);
    }
}
