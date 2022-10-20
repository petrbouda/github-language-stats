package pbouda.github.lang.statistics;

import org.junit.jupiter.api.Test;
import pbouda.github.lang.github.LanguageBytes;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageRatioAlgorithmTest {

    private static final LanguageRatioAlgorithm ALGORITHM = new LanguageRatioAlgorithm();

    @Test
    public void empty() {
        List<LanguageRatio> list = ALGORITHM.apply(List.of());
        assertEquals(List.of(), list);
    }

    @Test
    public void apply() {
        List<LanguageBytes> languageBytes = List.of(
                new LanguageBytes("Java", 3),
                new LanguageBytes("Ruby", 3),
                new LanguageBytes("Java", 1),
                new LanguageBytes("Kotlin", 4));

        List<LanguageRatio> actual = ALGORITHM.apply(languageBytes);

        //  Java = 4 / 11 = 0,3636
        //  Ruby = 3 / 11 = 0,272727273
        //  Kotlin = 4 / 11 = 0,3636
        List<LanguageRatio> expected = List.of(
                new LanguageRatio("Java", new BigDecimal("0.36")),
                new LanguageRatio("Ruby", new BigDecimal("0.27")),
                new LanguageRatio("Kotlin", new BigDecimal("0.36")));

        assertEquals(expected, actual);
    }
}