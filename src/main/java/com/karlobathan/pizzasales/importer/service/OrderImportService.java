package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.Order;
import com.karlobathan.pizzasales.api.repository.OrderRepository;
import com.karlobathan.pizzasales.importer.config.ImportProperties;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.OrderCsvRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service responsible for importing orders from a CSV resource.
 * Orders are processed in chunks of {@code app.import.chunk-size}: for each chunk it asks the repository
 * which source order ids already exist, then persists the rest in their own transaction.
 * Memory stays bounded by the chunk size however many orders the table already holds.
 * Each chunk is committed before the next lookup, so an id repeated later in the file is skipped too.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderImportService implements Consumer<String> {

    private final ImportProperties       importProperties;
    private final OrderRepository        orderRepository;
    private final CsvResourceLoader      csvResourceLoader;
    private final ChunkedEntityPersister chunkedEntityPersister;

    @Override
    public void accept(String resourcePath) {
        CsvSource<OrderCsvRow> orderSource = csvResourceLoader.loadResource(resourcePath, OrderCsvRow.class);

        int chunkSize = importProperties.chunkSize();
        AtomicInteger existingCount = new AtomicInteger();
        AtomicInteger importedCount = new AtomicInteger();

        Map<Long, OrderCsvRow> chunk = new LinkedHashMap<>();
        orderSource.forEach(row -> {
            if (chunk.putIfAbsent(row.getOrderId(), row) != null) {
                existingCount.incrementAndGet();
                log.debug("Order with ID {} repeats within the file, skipping import.", row.getOrderId());
                return;
            }

            if (chunk.size() == chunkSize) {
                importChunk(chunk, existingCount, importedCount);
            }
        });
        if (!chunk.isEmpty()) {
            importChunk(chunk, existingCount, importedCount);
        }

        log.info("Imported {} new orders ({} already existed)", importedCount.get(), existingCount.get());
    }

    private void importChunk(Map<Long, OrderCsvRow> chunk, AtomicInteger existingCount, AtomicInteger importedCount) {
        Set<Long> existingSourceIds = orderRepository.findSourceOrderIdIn(chunk.keySet());
        existingCount.addAndGet(existingSourceIds.size());
        log.debug("{} of {} orders in chunk already exist, skipping them.", existingSourceIds.size(), chunk.size());

        List<Order> orders = chunk.values()
                .stream()
                .filter(row -> !existingSourceIds.contains(row.getOrderId()))
                .map(this::getOrder)
                .toList();
        if (!orders.isEmpty()) {
            chunkedEntityPersister.persistChunk(orders);
            importedCount.addAndGet(orders.size());
            log.debug("Persisted chunk of {} orders ({} so far)", orders.size(), importedCount.get());
        }

        chunk.clear();
    }

    private Order getOrder(OrderCsvRow row) {
        return Order.builder()
                .sourceOrderId(row.getOrderId())
                .orderDate(row.getDate())
                .orderTime(row.getTime())
                .build();
    }
}
