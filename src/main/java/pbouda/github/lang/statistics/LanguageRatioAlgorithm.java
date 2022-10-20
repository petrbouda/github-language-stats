package pbouda.github.lang.statistics;

import pbouda.github.lang.github.LanguageBytes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class LanguageRatioAlgorithm implements Function<List<LanguageBytes>, List<LanguageRatio>> {

    @Override
    public List<LanguageRatio> apply(List<LanguageBytes> languageBytes) {
        return languageBytes.stream()
                .collect(Collectors.teeing(totalBytes(), totalBytesPerLanguage(), ratioPerLanguage()));
    }

    private static BiFunction<Long, Map<String, Long>, List<LanguageRatio>> ratioPerLanguage() {
        return (total, perLanguage) ->
                perLanguage.entrySet().stream()
                        .map(entry -> new LanguageRatio(entry.getKey(), div(entry.getValue(), total)))
                        .toList();
    }

    private static Collector<LanguageBytes, ?, Map<String, Long>> totalBytesPerLanguage() {
        return Collectors.groupingBy(LanguageBytes::name, Collectors.summingLong(LanguageBytes::bytes));
    }

    private static Collector<LanguageBytes, ?, Long> totalBytes() {
        return Collectors.summingLong(LanguageBytes::bytes);
    }

    private static BigDecimal div(long dividend, long divisor) {
        return new BigDecimal((double) dividend / divisor)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
