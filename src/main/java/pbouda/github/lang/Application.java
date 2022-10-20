package pbouda.github.lang;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import pbouda.github.lang.github.GithubClient;
import pbouda.github.lang.github.HttpGithubClient;
import pbouda.github.lang.scheduler.JdkBasedScheduler;
import pbouda.github.lang.scheduler.Scheduler;
import pbouda.github.lang.statistics.LanguageRatio;
import pbouda.github.lang.statistics.StatisticsUpdateTask;
import pbouda.github.lang.storage.AtomicStorage;
import pbouda.github.lang.storage.Storage;

import java.net.URI;
import java.util.concurrent.Executors;

@SpringBootApplication
public class Application implements ApplicationListener<ApplicationStartedEvent> {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        ConfigurableEnvironment env = context.getEnvironment();

        GithubClient githubClient = new HttpGithubClient(
                URI.create(env.getRequiredProperty("github.baseUri")),
                env.getRequiredProperty("github.organization"),
                env.getRequiredProperty("github.token"));

        @SuppressWarnings("unchecked")
        Storage<LanguageRatio> storage = (Storage<LanguageRatio>) context.getBean(Storage.class);

        Runnable task = new StatisticsUpdateTask(githubClient, storage);
        Scheduler scheduler = new JdkBasedScheduler(task);
        scheduler.start();
    }

    @Bean
    public Storage<LanguageRatio> languageRatioStorage() {
        return new AtomicStorage<>();
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
        return handler -> handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
