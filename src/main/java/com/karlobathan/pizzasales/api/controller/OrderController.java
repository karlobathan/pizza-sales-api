package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @Parameter(description = "First order date to include (yyyy-MM-dd)", example = "2015-01-01")
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
}
