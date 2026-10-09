package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.domain.OrderItem;
import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.repository.OrderItemRepository;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.importer.config.ImportProperties;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.OrderDetailsCsvRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemImportServiceTest {

    private static final String RESOURCE_PATH = "classpath:data/order_details.csv";
    private static final int    CHUNK_SIZE    = 2;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private CsvResourceLoader csvResourceLoader;

    @Mock
    private ChunkedEntityPersister chunkedEntityPersister;

    @Captor
    private ArgumentCaptor<Iterable<OrderItem>> orderItemsCaptor;

    private OrderItemImportService service;

    private final Pizza pepperoniS = Pizza.builder().id(1L).code("pepperoni_s").build();
    private final Pizza hawaiianM  = Pizza.builder().id(2L).code("hawaiian_m").build();

    @BeforeEach
    void setUp() {
        ImportProperties importProperties = new ImportProperties(null, null, null, RESOURCE_PATH, CHUNK_SIZE);
        service = new OrderItemImportService(importProperties,
                orderItemRepository,
                orderRepository,
                pizzaRepository,
                csvResourceLoader,
                chunkedEntityPersister
        );
        lenient().when(pizzaRepository.findAll()).thenReturn(List.of(pepperoniS, hawaiianM));
        // every order id asked for exists, unless a test overrides it
        lenient().when(orderRepository.findBySourceOrderIdIn(anyCollection()))
                .thenAnswer(invocation -> invocation.<Collection<Long>>getArgument(0).stream().map(this::order).toList());
    }

    @Test
    @DisplayName("accept builds order items linked to their order and pizza with quantity")
    void accept_buildsOrderItemsLinkedToOrderAndPizza() {
        givenRows(row(1L, 10L, "pepperoni_s", 2));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(orderItemsCaptor.capture());
        assertThat(orderItemsCaptor.getValue()).singleElement().satisfies(orderItem -> {
            assertThat(orderItem.getId()).isNull();
            assertThat(orderItem.getSourceOrderDetailsId()).isEqualTo(1L);
            assertThat(orderItem.getOrder().getSourceOrderId()).isEqualTo(10L);
            assertThat(orderItem.getPizza()).isSameAs(pepperoniS);
            assertThat(orderItem.getQuantity()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("accept links items of the same order to the same order")
    void accept_linksItemsOfSameOrderToSameOrder() {
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 10L, "hawaiian_m", 1));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(orderItemsCaptor.capture());
        List<OrderItem> orderItems = new ArrayList<>();
        orderItemsCaptor.getValue().forEach(orderItems::add);
        assertThat(orderItems).hasSize(2);
        assertThat(orderItems.get(1).getOrder()).isSameAs(orderItems.get(0).getOrder());
    }

    @Test
    @DisplayName("accept looks up orders once per chunk with only the order ids of new rows")
    void accept_looksUpOrdersOncePerChunkForNewRowsOnly() {
        when(orderItemRepository.findSourceOrderDetailsIdIn(anyCollection())).thenReturn(Set.of(1L));
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 20L, "hawaiian_m", 1));

        service.accept(RESOURCE_PATH);

        verify(orderRepository, times(1)).findBySourceOrderIdIn(Set.of(20L));
    }

    @Test
    @DisplayName("accept skips rows whose source id already exists")
    void accept_skipsRowsWhoseSourceIdAlreadyExists() {
        when(orderItemRepository.findSourceOrderDetailsIdIn(anyCollection())).thenReturn(Set.of(1L));
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 10L, "hawaiian_m", 1));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(orderItemsCaptor.capture());
        assertThat(orderItemsCaptor.getValue()).extracting(OrderItem::getSourceOrderDetailsId).containsExactly(2L);
    }

    @Test
    @DisplayName("accept skips a source id repeated in a later chunk once the earlier chunk is persisted")
    void accept_skipsSourceIdRepeatedInLaterChunk() {
        Set<Long> persistedIds = new HashSet<>();
        doAnswer(invocation -> {
            invocation.<Iterable<OrderItem>>getArgument(0)
                    .forEach(orderItem -> persistedIds.add(orderItem.getSourceOrderDetailsId()));
            return null;
        }).when(chunkedEntityPersister).persistChunk(any());
        when(orderItemRepository.findSourceOrderDetailsIdIn(anyCollection())).thenAnswer(invocation -> {
            Set<Long> existing = new HashSet<>(invocation.<Collection<Long>>getArgument(0));
            existing.retainAll(persistedIds);
            return existing;
        });
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 10L, "hawaiian_m", 1), row(1L, 10L, "pepperoni_s", 5));

        service.accept(RESOURCE_PATH);

        assertThat(persistedIds).containsExactlyInAnyOrder(1L, 2L);
        verify(chunkedEntityPersister, times(1)).persistChunk(any());
    }

    @Test
    @DisplayName("accept persists order items in chunks of the configured chunk size plus a final partial chunk")
    void accept_persistsOrderItemsInChunks() {
        int rowCount = CHUNK_SIZE * 2 + 1;
        givenRows(LongStream.rangeClosed(1, rowCount)
                .mapToObj(id -> row(id, 10L, "pepperoni_s", 1))
                .toArray(OrderDetailsCsvRow[]::new));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister, times(3)).persistChunk(orderItemsCaptor.capture());
        List<Iterable<OrderItem>> chunks = orderItemsCaptor.getAllValues();
        assertThat(chunks.get(0)).hasSize(CHUNK_SIZE);
        assertThat(chunks.get(1)).hasSize(CHUNK_SIZE);
        assertThat(chunks.get(2)).extracting(OrderItem::getSourceOrderDetailsId).containsExactly((long) rowCount);
    }

    @Test
    @DisplayName("accept persists nothing and skips the order lookup when every source id already exists")
    void accept_persistsNothingWhenEverySourceIdAlreadyExists() {
        when(orderItemRepository.findSourceOrderDetailsIdIn(anyCollection())).thenReturn(Set.of(1L));
        givenRows(row(1L, 10L, "pepperoni_s", 1));

        service.accept(RESOURCE_PATH);

        verify(orderRepository, never()).findBySourceOrderIdIn(anyCollection());
        verify(chunkedEntityPersister, never()).persistChunk(any());
    }

    @Test
    @DisplayName("accept fails without persisting the chunk when a row references an unknown order")
    void accept_failsWhenOrderIsUnknown() {
        when(orderRepository.findBySourceOrderIdIn(anyCollection())).thenReturn(List.of(order(10L)));
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 99L, "pepperoni_s", 1));

        assertThatIllegalStateException().isThrownBy(() -> service.accept(RESOURCE_PATH))
                .withMessage("Order details '2' references unknown order '99'");
        verify(chunkedEntityPersister, never()).persistChunk(any());
    }

    @Test
    @DisplayName("accept fails without persisting the chunk when a row references an unknown pizza")
    void accept_failsWhenPizzaIsUnknown() {
        givenRows(row(1L, 10L, "pepperoni_s", 1), row(2L, 10L, "ghost_m", 1));

        assertThatIllegalStateException().isThrownBy(() -> service.accept(RESOURCE_PATH))
                .withMessage("Order details '2' references unknown pizza 'ghost_m'");
        verify(chunkedEntityPersister, never()).persistChunk(any());
    }

    private Order order(Long sourceOrderId) {
        return Order.builder().id(sourceOrderId * 100).sourceOrderId(sourceOrderId).build();
    }

    private void givenRows(OrderDetailsCsvRow... rows) {
        CsvSource<OrderDetailsCsvRow> source = action -> List.of(rows).forEach(action);
        when(csvResourceLoader.loadResource(RESOURCE_PATH, OrderDetailsCsvRow.class)).thenReturn(source);
    }

    private static OrderDetailsCsvRow row(long orderDetailsId, long orderId, String pizzaId, int quantity) {
        OrderDetailsCsvRow row = new OrderDetailsCsvRow();
        row.setOrderDetailsId(orderDetailsId);
        row.setOrderId(orderId);
        row.setPizzaId(pizzaId);
        row.setQuantity(quantity);
        return row;
    }
}
