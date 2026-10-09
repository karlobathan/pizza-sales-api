package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.OrderRequest;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping(ApiResources.ORDERS_PATH)
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orders placed at the pizza place, with the pizzas on each")
public class OrderController {

    static final String DEFAULT_PAGE_SIZE = "20";
    static final int    MAX_PAGE_SIZE     = 100;

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Search orders",
            description = "Returns one page of orders, optionally limited to an inclusive date range, "
                    + "sorted by order date, then time, then id."
    )
    @ApiResponse(responseCode = "200", description = "A page of orders")
    @ApiResponse(responseCode = "400", ref = ApiDocs.ORDER_SEARCH_BAD_REQUEST)
    public PageResponse<OrderSummaryResponse> findAll(
            // examples use days above 12 so the year-month-day order can't be misread
            @Parameter(description = "First order date to include (yyyy-MM-dd)", example = "2015-01-13")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last order date to include (yyyy-MM-dd)", example = "2015-01-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Orders per page, at most " + MAX_PAGE_SIZE)
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return orderService.findAll(from, to, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id", description = "Returns the order with its items and totals.")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", ref = ApiDocs.ORDER_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.ORDER_BAD_REQUEST)
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an order",
            description = "Creates an order with its items. Nothing is saved if any item is invalid. "
                    + "The Location header points to the new order."
    )
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", ref = ApiDocs.ORDER_WRITE_BAD_REQUEST)
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = orderService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an order",
            description = "Replaces the order's date, time and all of its items; the new items get new ids. "
                    + "Nothing changes if any item is invalid. A deleted order can't be replaced (404)."
    )
    @ApiResponse(responseCode = "200", description = "Order replaced")
    @ApiResponse(responseCode = "404", ref = ApiDocs.ORDER_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.ORDER_WRITE_BAD_REQUEST)
    public OrderResponse replace(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.replace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an order",
            description = "Deletes the order: from then on it no longer appears in any response, and getting or "
                    + "deleting it again returns 404. It is kept in the database (soft delete), so a re-import "
                    + "doesn't recreate it."
    )
    @ApiResponse(responseCode = "204", description = "Order deleted")
    @ApiResponse(responseCode = "404", ref = ApiDocs.ORDER_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.ORDER_BAD_REQUEST)
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
