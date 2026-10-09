package com.karlobathan.pizzasales.importer.loader;

import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class FileCsvResourceLoaderTest {

    private CsvResourceLoader loader;

    @BeforeEach
    void setUp() {
        loader = new FileCsvResourceLoader(new DefaultResourceLoader());
    }

    @Test
    @DisplayName("loadCsvFile reads all rows into beans")
    void loadCsvFile_readsAllRowsIntoBeans() {
        List<SampleRow> rows = new ArrayList<>();

        CsvSource<SampleRow> sampleRowCsvSource = loader.loadResource("classpath:csv/sample.csv", SampleRow.class);
        sampleRowCsvSource.forEach(rows::add);

        assertThat(rows).hasSize(10);
        assertThat(rows.getFirst()).extracting(SampleRow::getId, SampleRow::getName, SampleRow::getPrice)
                .containsExactly(1, "Margherita", 9.50);
        assertThat(rows.getLast()).extracting(SampleRow::getId, SampleRow::getName, SampleRow::getPrice)
                .containsExactly(10, "Buffalo", 12.00);
        assertThat(rows).extracting(SampleRow::getId).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("loadCsvFile reads all rows into beans with row containing multiple values")
    void loadCsvFile_readsAllRowsIntoBeansWithMultipleValues() {
        List<SampleRowWithList> rows = new ArrayList<>();

        CsvSource<SampleRowWithList> sampleRowCsvSource = loader.loadResource("classpath:csv/sample-with-list.csv",
                SampleRowWithList.class
        );
        sampleRowCsvSource.forEach(rows::add);

        assertThat(rows).hasSize(5);
        assertThat(rows.getFirst()).extracting(SampleRowWithList::getId,
                SampleRowWithList::getName,
                SampleRowWithList::getPrice
        ).containsExactly(1, "Margherita", 9.50);
        assertThat(rows.getFirst().getIngredients()).containsExactly("Tomato Sauce", "Mozzarella", "Basil");
        assertThat(rows.getLast()).extracting(SampleRowWithList::getId,
                SampleRowWithList::getName,
                SampleRowWithList::getPrice
        ).containsExactly(5, "Veggie", 10.00);
        assertThat(rows.getLast().getIngredients()).containsExactly("Tomato Sauce",
                "Mozzarella",
                "Bell Peppers",
                "Onions",
                "Mushrooms",
                "Olives"
        );
        assertThat(rows).extracting(SampleRowWithList::getId).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("constructor rejects null resourceLoader")
    void constructor_rejectsNullResourceLoader() {
        assertThatNullPointerException().isThrownBy(() -> new FileCsvResourceLoader(null))
                .withMessage("resourceLoader must not be null");
    }

    @Getter
    public static class SampleRow {

        @CsvBindByName(column = "id", required = true)
        private Integer id;

        @CsvBindByName(column = "name", required = true)
        private String name;

        @CsvBindByName(column = "price", required = true)
        private Double price;

    }

    @Getter
    public static class SampleRowWithList extends SampleRow {

        @CsvBindAndSplitByName(column = "ingredients", required = true, elementType = String.class, splitOn = BinderConfig.SPLIT_ON_COMMA_DELIMITER)
        private List<String> ingredients;

    }
}
