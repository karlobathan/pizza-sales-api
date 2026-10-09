package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import com.karlobathan.pizzasales.importer.config.ImportProperties;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.OrderCsvRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderImportServiceTest {

    private static final String RESOURCE_PATH = "classpath:data/orders.csv";
    private static final int    CHUNK_SIZE    = 2;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CsvResourceLoader csvResourceLoader;

    @Mock
    private ChunkedEntityPersister chunkedEntityPersister;

    @Captor
    private ArgumentCaptor<Iterable<Order>> ordersCaptor;

    private OrderImportService service;

    @BeforeEach
    void setUp() {
        ImportProperties importProperties = new ImportProperties(null, null, RESOURCE_PATH, CHUNK_SIZE);
        service = new OrderImportService(importProperties, orderRepository, csvResourceLoader, chunkedEntityPersister);
    }

    @Test
    @DisplayName("accept builds orders from rows with source id, date and time")
    void accept_buildsOrdersFromRows() {
        givenRows(row(1L, "2015-01-01", "11:38:36"));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).singleElement().satisfies(order -> {
            assertThat(order.getId()).isNull();
            assertThat(order.getSourceOrderId()).isEqualTo(1L);
            assertThat(order.getOrderDate()).isEqualTo(LocalDate.of(2015, 1, 1));
            assertThat(order.getOrderTime()).isEqualTo(LocalTime.of(11, 38, 36));
        });
    }

    @Test
    @DisplayName("accept skips rows whose source id already exists")
    void accept_skipsRowsWhoseSourceIdAlreadyExists() {
        when(orderRepository.findSourceOrderIdIn(anyCollection())).thenReturn(Set.of(1L));
        givenRows(row(1L, "2015-01-01", "11:38:36"), row(2L, "2015-01-01", "11:57:40"));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).extracting(Order::getSourceOrderId).containsExactly(2L);
    }

    @Test
    @DisplayName("accept keeps the first row when a source id repeats within the file")
    void accept_keepsFirstRowWhenSourceIdRepeatsWithinFile() {
        givenRows(row(1L, "2015-01-01", "11:38:36"), row(1L, "2015-02-02", "09:00:00"));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister).persistChunk(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).singleElement()
                .extracting(Order::getOrderDate)
                .isEqualTo(LocalDate.of(2015, 1, 1));
    }

    @Test
    @DisplayName("accept persists orders in chunks of the configured chunk size plus a final partial chunk")
    void accept_persistsOrdersInChunks() {
        int rowCount = CHUNK_SIZE * 2 + 1;
        givenRows(LongStream.rangeClosed(1, rowCount)
                .mapToObj(id -> row(id, "2015-01-01", "11:38:36"))
                .toArray(OrderCsvRow[]::new));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister, times(3)).persistChunk(ordersCaptor.capture());
        List<Iterable<Order>> chunks = ordersCaptor.getAllValues();
        assertThat(chunks.get(0)).hasSize(CHUNK_SIZE);
        assertThat(chunks.get(1)).hasSize(CHUNK_SIZE);
        assertThat(chunks.get(2)).extracting(Order::getSourceOrderId).containsExactly((long) rowCount);
    }

    @Test
    @DisplayName("accept looks up existing source ids once per chunk with only that chunk's ids")
    void accept_looksUpExistingSourceIdsOncePerChunk() {
        List<List<Long>> lookups = new ArrayList<>();
        when(orderRepository.findSourceOrderIdIn(anyCollection())).thenAnswer(invocation -> {
            lookups.add(List.copyOf(invocation.<Collection<Long>>getArgument(0)));
            return Set.of();
        });
        givenRows(LongStream.rangeClosed(1, 5)
                .mapToObj(id -> row(id, "2015-01-01", "11:38:36"))
                .toArray(OrderCsvRow[]::new));

        service.accept(RESOURCE_PATH);

        assertThat(lookups).containsExactly(List.of(1L, 2L), List.of(3L, 4L), List.of(5L));
    }

    @Test
    @DisplayName("accept skips a source id repeated in a later chunk once the earlier chunk is persisted")
    void accept_skipsSourceIdRepeatedInLaterChunk() {
        Set<Long> persistedIds = new HashSet<>();
        doAnswer(invocation -> {
            invocation.<Iterable<Order>>getArgument(0).forEach(order -> persistedIds.add(order.getSourceOrderId()));
            return null;
        }).when(chunkedEntityPersister).persistChunk(any());
        when(orderRepository.findSourceOrderIdIn(anyCollection())).thenAnswer(invocation -> {
            Set<Long> existing = new HashSet<>(invocation.<Collection<Long>>getArgument(0));
            existing.retainAll(persistedIds);
            return existing;
        });
        givenRows(row(1L, "2015-01-01", "11:38:36"), row(2L, "2015-01-01", "11:57:40"), row(1L, "2015-02-02", "09:00:00"));

        service.accept(RESOURCE_PATH);

        assertThat(persistedIds).containsExactlyInAnyOrder(1L, 2L);
        verify(chunkedEntityPersister, times(1)).persistChunk(any());
    }

    @Test
    @DisplayName("accept persists nothing when every source id already exists")
    void accept_persistsNothingWhenEverySourceIdAlreadyExists() {
        when(orderRepository.findSourceOrderIdIn(anyCollection())).thenReturn(Set.of(1L));
        givenRows(row(1L, "2015-01-01", "11:38:36"));

        service.accept(RESOURCE_PATH);

        verify(chunkedEntityPersister, never()).persistChunk(any());
    }

    private void givenRows(OrderCsvRow... rows) {
        CsvSource<OrderCsvRow> source = action -> List.of(rows).forEach(action);
        when(csvResourceLoader.loadResource(RESOURCE_PATH, OrderCsvRow.class)).thenReturn(source);
    }

    private static OrderCsvRow row(long orderId, String date, String time) {
        OrderCsvRow row = new OrderCsvRow();
        row.setOrderId(orderId);
        row.setDate(LocalDate.parse(date));
        row.setTime(LocalTime.parse(time));
        return row;
    }
}
