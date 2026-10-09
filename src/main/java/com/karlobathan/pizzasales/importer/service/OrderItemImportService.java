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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for importing order items from the order details CSV resource.
 * Order items are processed in chunks of {@code app.import.chunk-size}: for each chunk it asks the repository
 * which source order details ids already exist, loads the orders the rest belong to, then persists them
 * in their own transaction. Pizzas are a small reference table, so they are loaded once up front.
 * Fails the import if a row references a pizza or order that does not exist; chunks persisted before the
 * failure stay committed and are skipped on the next run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderItemImportService implements Consumer<String> {

    private final ImportProperties       importProperties;
    private final OrderItemRepository    orderItemRepository;
    private final OrderRepository        orderRepository;
    private final PizzaRepository        pizzaRepository;
    private final CsvResourceLoader      csvResourceLoader;
    private final ChunkedEntityPersister chunkedEntityPersister;

    @Override
    public void accept(String resourcePath) {
        CsvSource<OrderDetailsCsvRow> orderDetailsSource = csvResourceLoader.loadResource(resourcePath,
                OrderDetailsCsvRow.class
        );

        Map<String, Pizza> existingPizzas = getExistingPizzas();

        int chunkSize = importProperties.chunkSize();
        AtomicInteger existingCount = new AtomicInteger();
        AtomicInteger importedCount = new AtomicInteger();

        Map<Long, OrderDetailsCsvRow> chunk = new LinkedHashMap<>();
        orderDetailsSource.forEach(row -> {
            if (chunk.putIfAbsent(row.getOrderDetailsId(), row) != null) {
                existingCount.incrementAndGet();
                log.debug("Order details with ID {} repeats within the file, skipping import.", row.getOrderDetailsId());
                return;
            }

            if (chunk.size() == chunkSize) {
                importChunk(chunk, existingPizzas, existingCount, importedCount);
            }
        });
        if (!chunk.isEmpty()) {
            importChunk(chunk, existingPizzas, existingCount, importedCount);
        }

        log.info("Imported {} new order items ({} already existed)", importedCount.get(), existingCount.get());
    }

    private void importChunk(
            Map<Long, OrderDetailsCsvRow> chunk,
            Map<String, Pizza> existingPizzas,
            AtomicInteger existingCount,
            AtomicInteger importedCount) {
        Set<Long> existingSourceIds = orderItemRepository.findSourceOrderDetailsIdIn(chunk.keySet());
        existingCount.addAndGet(existingSourceIds.size());
        log.debug("{} of {} order items in chunk already exist, skipping them.", existingSourceIds.size(), chunk.size());

        List<OrderDetailsCsvRow> rowsToAdd = chunk.values()
                .stream()
                .filter(row -> !existingSourceIds.contains(row.getOrderDetailsId()))
                .toList();
        chunk.clear();
        if (rowsToAdd.isEmpty()) {
            return;
        }

        Map<Long, Order> orders = getOrders(rowsToAdd);
        List<OrderItem> orderItems = rowsToAdd.stream().map(row -> getOrderItem(row, orders, existingPizzas)).toList();
        chunkedEntityPersister.persistChunk(orderItems);
        importedCount.addAndGet(orderItems.size());
        log.debug("Persisted chunk of {} order items ({} so far)", orderItems.size(), importedCount.get());
    }

    private OrderItem getOrderItem(OrderDetailsCsvRow row, Map<Long, Order> orders, Map<String, Pizza> existingPizzas) {
        Order order = orders.get(row.getOrderId());
        if (order == null) {
            throw new IllegalStateException("Order details '%s' references unknown order '%s'".formatted(row.getOrderDetailsId(),
                    row.getOrderId()
            ));
        }

        Pizza pizza = existingPizzas.get(row.getPizzaId());
        if (pizza == null) {
            throw new IllegalStateException("Order details '%s' references unknown pizza '%s'".formatted(row.getOrderDetailsId(),
                    row.getPizzaId()
            ));
        }

        return OrderItem.builder()
                .sourceOrderDetailsId(row.getOrderDetailsId())
                .order(order)
                .pizza(pizza)
                .quantity(row.getQuantity())
                .build();
    }

    private Map<Long, Order> getOrders(List<OrderDetailsCsvRow> rows) {
        Set<Long> sourceOrderIds = rows.stream().map(OrderDetailsCsvRow::getOrderId).collect(Collectors.toSet());
        return orderRepository.findBySourceOrderIdIn(sourceOrderIds)
                .stream()
                .collect(Collectors.toMap(Order::getSourceOrderId, Function.identity()));
    }

    private Map<String, Pizza> getExistingPizzas() {
        return pizzaRepository.findAll().stream().collect(Collectors.toMap(Pizza::getCode, Function.identity()));
    }
}
