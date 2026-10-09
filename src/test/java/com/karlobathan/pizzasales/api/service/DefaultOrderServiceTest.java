package com.karlobathan.pizzasales.api.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private PizzaRepository pizzaRepository;

    @Mock
    private OrderMapper orderMapper;

    @Captor
    private ArgumentCaptor<List<OrderItem>> itemsCaptor;

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

    @Test
    @DisplayName("create saves the order and its items with the referenced pizzas and maps them")
    void create_savesOrderAndItems() {
        LocalDate date = LocalDate.of(2015, 3, 1);
        LocalTime time = LocalTime.of(18, 30);
        Pizza pizza = Pizza.builder().id(10L).price(new BigDecimal("12.50")).build();
        Order saved = Order.builder().id(7L).orderDate(date).orderTime(time).build();
        OrderResponse response = new OrderResponse(7L, date, time, List.of(), 0, BigDecimal.ZERO);
        when(pizzaRepository.findWithPizzaTypeByIdIn(Set.of(10L))).thenReturn(List.of(pizza));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class), anyList())).thenReturn(response);

        OrderResponse result = service.create(new OrderRequest(date, time, List.of(new OrderItemRequest(10L, 2),
                new OrderItemRequest(10L, 1)
        )));

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getId()).isNull();
        assertThat(order.getValue().getSourceOrderId()).isNull();
        assertThat(order.getValue().getOrderDate()).isEqualTo(date);
        assertThat(order.getValue().getOrderTime()).isEqualTo(time);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).allSatisfy(item -> {
            assertThat(item.getOrder()).isSameAs(saved);
            assertThat(item.getPizza()).isSameAs(pizza);
            assertThat(item.getSourceOrderDetailsId()).isNull();
        });
        assertThat(itemsCaptor.getValue()).extracting(OrderItem::getQuantity).containsExactly(2, 1);
        verify(orderMapper).toResponse(saved, itemsCaptor.getValue());
    }

    @Test
    @DisplayName("create rejects unknown pizzas listing their ids without saving anything")
    void create_rejectsUnknownPizzasWithoutSaving() {
        Pizza pizza = Pizza.builder().id(10L).build();
        when(pizzaRepository.findWithPizzaTypeByIdIn(Set.of(10L, 98L, 99L))).thenReturn(List.of(pizza));

        assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> service.create(new OrderRequest(LocalDate.of(2015, 3, 1),
                LocalTime.of(18, 30),
                List.of(new OrderItemRequest(99L, 1), new OrderItemRequest(10L, 1), new OrderItemRequest(98L, 1))
        ))).withMessage("Unknown pizza id(s): 98, 99");
        verifyNoInteractions(orderRepository, orderItemRepository, orderMapper);
    }
}
