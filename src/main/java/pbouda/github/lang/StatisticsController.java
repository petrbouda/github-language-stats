package pbouda.github.lang;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pbouda.github.lang.statistics.LanguageRatio;
import pbouda.github.lang.storage.Storage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {

    private final Storage<LanguageRatio> storage;

    public StatisticsController(Storage<LanguageRatio> storage) {
        this.storage = storage;
    }

    @GetMapping("languages")
    public Map<String, BigDecimal> get() {
        return storage.get().stream()
                .collect(Collectors.toMap(LanguageRatio::name, LanguageRatio::ratio));
    }
}
