package employees;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(EmployeesProperties.class)
public class ClientConfig {
    @Bean
    public EmployeesClient employeesClient(RestClient.Builder builder, EmployeesProperties employeesProperties) {
        var webClient = builder
                .baseUrl(employeesProperties.getBackendUrl())
                .build();
        var factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(webClient)).build();
        return factory.createClient(EmployeesClient.class);
    }

}
