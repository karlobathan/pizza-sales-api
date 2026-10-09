package com.karlobathan.pizzasales.api.mapper;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.domain.OrderItem;
import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.OrderItemResponse;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapperImpl(new PizzaMapperImpl());

    private final PizzaType pepperoniType = PizzaType.builder().id(1L).code("pepperoni").name("The Pepperoni Pizza").build();
    private final Pizza     pepperoniM    = pizza(10L, "pepperoni_m", PizzaSize.M, "12.50");
    private final Pizza     pepperoniL    = pizza(11L, "pepperoni_l", PizzaSize.L, "15.25");
    private final Order     order         = Order.builder()
            .id(5L)
            .sourceOrderId(42L)
            .orderDate(LocalDate.of(2015, 1, 1))
            .orderTime(LocalTime.of(11, 38, 36))
            .build();

    @Test
    @DisplayName("toSummary maps order id, date and time")
    void toSummary_mapsOrderIdDateAndTime() {
        assertThat(mapper.toSummary(order)).isEqualTo(new OrderSummaryResponse(5L,
                LocalDate.of(2015, 1, 1),
                LocalTime.of(11, 38, 36)
        ));
    }

    @Test
    @DisplayName("toResponse maps an order item with its pizza and a line total of price times quantity")
    void toResponse_mapsOrderItemWithLineTotal() {
        OrderItemResponse response = mapper.toResponse(item(100L, pepperoniM, 3));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.lineTotal()).isEqualByComparingTo("37.50");
        assertThat(response.pizza().code()).isEqualTo("pepperoni_m");
        assertThat(response.pizza().pizzaType().code()).isEqualTo("pepperoni");
    }

    @Test
    @DisplayName("toResponse maps an order with its items in the given order and sums quantity and price")
    void toResponse_mapsOrderWithItemsAndTotals() {
        OrderResponse response = mapper.toResponse(order, List.of(item(100L, pepperoniM, 2), item(101L, pepperoniL, 1)));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.orderDate()).isEqualTo(LocalDate.of(2015, 1, 1));
        assertThat(response.orderTime()).isEqualTo(LocalTime.of(11, 38, 36));
        assertThat(response.items()).extracting(OrderItemResponse::id).containsExactly(100L, 101L);
        assertThat(response.totalQuantity()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualByComparingTo("40.25");
    }

    @Test
    @DisplayName("toResponse maps an order without items to zero totals")
    void toResponse_mapsOrderWithoutItemsToZeroTotals() {
        OrderResponse response = mapper.toResponse(order, List.of());

        assertThat(response.items()).isEmpty();
        assertThat(response.totalQuantity()).isZero();
        assertThat(response.totalPrice()).isEqualByComparingTo("0");
    }

    private Pizza pizza(Long id, String code, PizzaSize size, String price) {
        return Pizza.builder().id(id).code(code).size(size).price(new BigDecimal(price)).pizzaType(pepperoniType).build();
    }

    private OrderItem item(Long id, Pizza pizza, int quantity) {
        return OrderItem.builder().id(id).order(order).pizza(pizza).quantity(quantity).build();
    }
}
