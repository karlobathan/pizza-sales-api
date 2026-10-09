package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * {@link PizzaService} backed by the database. The pizza type is fetched in the same query as the pizza,
 * so mapping it needs no further queries. Writes check for conflicts before saving anything.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPizzaService implements PizzaService {

    private final PizzaRepository     pizzaRepository;
    private final PizzaTypeRepository pizzaTypeRepository;
    private final PizzaMapper         pizzaMapper;

    @Override
    public List<PizzaResponse> findAll() {
        return pizzaMapper.toPizzaResponses(pizzaRepository.findAllActiveWithPizzaType());
    }

    @Override
    public PizzaResponse findById(Long id) {
        return pizzaMapper.toResponse(getPizza(id));
    }

    @Override
    @Transactional
    public PizzaResponse create(PizzaCreateRequest request) {
        if (pizzaRepository.existsByCode(request.code())) {
            throw ConflictException.codeTaken(ApiResources.PIZZA, request.code());
        }
        PizzaType pizzaType = getPizzaType(request.pizzaTypeId());
        if (pizzaRepository.existsByPizzaTypeIdAndSizeAndDeletedAtIsNull(pizzaType.getId(), request.size())) {
            throw ConflictException.sizeTaken(pizzaType.getCode(), request.size());
        }

        Pizza pizza = Pizza.builder()
                .code(request.code())
                .pizzaType(pizzaType)
                .size(request.size())
                .price(request.price())
                .build();
        return pizzaMapper.toResponse(pizzaRepository.save(pizza));
    }

    @Override
    @Transactional
    public PizzaResponse replace(Long id, PizzaUpdateRequest request) {
        Pizza pizza = getPizza(id);
        PizzaType pizzaType = getPizzaType(request.pizzaTypeId());
        if (pizzaRepository.existsByPizzaTypeIdAndSizeAndDeletedAtIsNullAndIdNot(pizzaType.getId(), request.size(), id)) {
            throw ConflictException.sizeTaken(pizzaType.getCode(), request.size());
        }

        pizza.setPizzaType(pizzaType); // written on commit
        pizza.setSize(request.size());
        pizza.setPrice(request.price());
        return pizzaMapper.toResponse(pizza);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getPizza(id).setDeletedAt(Instant.now()); // written on commit; orders keep pointing to it
    }

    private Pizza getPizza(Long id) {
        return pizzaRepository.findActiveWithPizzaTypeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiResources.PIZZA, id));
    }

    // a pizza type that doesn't exist is a problem with the request body, not the URL, so 400 rather than 404
    private PizzaType getPizzaType(Long pizzaTypeId) {
        return pizzaTypeRepository.findActiveWithDetailsById(pizzaTypeId)
                .orElseThrow(() -> InvalidRequestException.unknownPizzaType(pizzaTypeId));
    }
}
