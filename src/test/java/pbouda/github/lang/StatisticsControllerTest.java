package pbouda.github.lang;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pbouda.github.lang.statistics.LanguageRatio;
import pbouda.github.lang.storage.Storage;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticsController.class)
@ContextConfiguration(classes = {StatisticsController.class, Application.class})
class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private Storage<LanguageRatio> storage;

    @Test
    public void empty() throws Exception {
        when(storage.get())
                .thenReturn(List.of());

        ResultActions result = mvc.perform(get("/statistics/languages"))
                .andExpect(status().isOk());

        String actual = result.andReturn().getResponse().getContentAsString();
        assertEquals("{}", actual, true);
    }

    @Test
    public void languages() throws Exception {
        List<LanguageRatio> ratios = List.of(
                new LanguageRatio("Java", new BigDecimal("0.40")),
                new LanguageRatio("Ruby", new BigDecimal("0.20")),
                new LanguageRatio("Kotlin", new BigDecimal("0.40")));

        when(storage.get())
                .thenReturn(ratios);

        String expected = """
                {
                    "Java": 0.40,
                    "Ruby": 0.20,
                    "Kotlin": 0.40
                }""";

        ResultActions result = mvc.perform(get("/statistics/languages"))
                .andExpect(status().isOk());

        String actual = result.andReturn().getResponse().getContentAsString();
        assertEquals(expected, actual, true);
    }
}