package ma.ac.emi.studentmanagement.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnleashConfig {

    @Value("${unleash.api-url}")
    private String apiUrl;

    @Value("${unleash.api-token}")
    private String apiToken;

    @Value("${unleash.app-name}")
    private String appName;

    @Value("${unleash.environment}")
    private String environment;

    @Bean
    public Unleash unleash() {
        io.getunleash.util.UnleashConfig config = io.getunleash.util.UnleashConfig.builder()
                .appName(appName)
                .instanceId(appName + "-" + System.getenv().getOrDefault("APP_VERSION", "v1"))
                .unleashAPI(apiUrl)
                .apiKey(apiToken)
                .environment(environment)
                .synchronousFetchOnInitialisation(true)
                .build();

        return new DefaultUnleash(config);
    }
}
