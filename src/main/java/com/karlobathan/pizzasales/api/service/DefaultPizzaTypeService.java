package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaTypeCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaCategoryRepository;
import com.karlobathan.pizzasales.api.repository.PizzaIngredientRepository;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link PizzaTypeService} backed by the database. Category and ingredients are fetched in the same query as the
 * pizza type, so mapping them needs no further queries. Writes match categories and ingredients by name ignoring case,
 * like the CSV import, and create the ones that don't exist yet.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPizzaTypeService implements PizzaTypeService {

    private final PizzaTypeRepository       pizzaTypeRepository;
    private final PizzaRepository           pizzaRepository;
    private final PizzaCategoryRepository   pizzaCategoryRepository;
    private final PizzaIngredientRepository pizzaIngredientRepository;
    private final PizzaMapper               pizzaMapper;

    @Override
    public List<PizzaTypeResponse> findAll() {
        return pizzaMapper.toPizzaTypeResponses(pizzaTypeRepository.findAllActiveWithDetails());
    }

    @Override
    public PizzaTypeResponse findById(Long id) {
        return pizzaMapper.toResponse(getPizzaType(id));
    }

    @Override
    @Transactional
    public PizzaTypeResponse create(PizzaTypeCreateRequest request) {
        if (pizzaTypeRepository.existsByCode(request.code())) {
            throw ConflictException.codeTaken(ApiResources.PIZZA_TYPE, request.code());
        }

        PizzaType pizzaType = PizzaType.builder()
                .code(request.code())
                .name(request.name().trim())
                .pizzaCategory(findOrCreateCategory(request.category()))
                .pizzaIngredients(findOrCreateIngredients(request.ingredients()))
                .build();
        return pizzaMapper.toResponse(pizzaTypeRepository.save(pizzaType));
    }

    @Override
    @Transactional
    public PizzaTypeResponse replace(Long id, PizzaTypeUpdateRequest request) {
        PizzaType pizzaType = getPizzaType(id);
        pizzaType.setName(request.name().trim()); // written on commit
        pizzaType.setPizzaCategory(findOrCreateCategory(request.category()));
        pizzaType.getPizzaIngredients().clear();
        pizzaType.getPizzaIngredients().addAll(findOrCreateIngredients(request.ingredients()));
        return pizzaMapper.toResponse(pizzaType);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PizzaType pizzaType = getPizzaType(id);
        // its pizzas must go first, so no pizza on the menu points to a deleted pizza type
        if (pizzaRepository.existsByPizzaTypeIdAndDeletedAtIsNull(id)) {
            throw ConflictException.stillHasPizzas(id);
        }
        pizzaType.setDeletedAt(Instant.now()); // written on commit
    }

    private PizzaType getPizzaType(Long id) {
        return pizzaTypeRepository.findActiveWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiResources.PIZZA_TYPE, id));
    }

    private PizzaCategory findOrCreateCategory(String name) {
        String trimmed = name.trim();
        return pizzaCategoryRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> pizzaCategoryRepository.save(PizzaCategory.builder().name(trimmed).build()));
    }

    // one query for the existing ones; names repeated ignoring case are kept once, with their first spelling
    private Set<PizzaIngredient> findOrCreateIngredients(List<String> names) {
        Map<String, String> namesByLowerCase = new LinkedHashMap<>();
        names.stream().map(String::trim).forEach(name -> namesByLowerCase.putIfAbsent(name.toLowerCase(), name));

        Map<String, PizzaIngredient> existing = pizzaIngredientRepository.findByLowerCaseNameIn(namesByLowerCase.keySet())
                .stream()
                .collect(Collectors.toMap(ingredient -> ingredient.getName().toLowerCase(), Function.identity()));

        Set<PizzaIngredient> ingredients = new HashSet<>();
        namesByLowerCase.forEach((lowerCase, name) -> ingredients.add(existing.containsKey(lowerCase)
                ? existing.get(lowerCase)
                : pizzaIngredientRepository.save(PizzaIngredient.builder().name(name).build())));
        return ingredients;
    }
}
