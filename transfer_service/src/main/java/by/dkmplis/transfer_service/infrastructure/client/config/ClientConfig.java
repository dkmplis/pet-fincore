package by.dkmplis.transfer_service.infrastructure.client.config;

import by.dkmplis.transfer_service.infrastructure.client.ledger.LedgerClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LedgerClientProperties.class)
@RequiredArgsConstructor
public class ClientConfig {
    
    @Bean
    public RestClient ledgerRestClient(
            RestClient.Builder builder,
            LedgerClientProperties properties
    ) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = 
                new JdkClientHttpRequestFactory(httpClient);
        
        requestFactory.setReadTimeout(
                properties.readTimeout()
        );
        
        return builder
                .clone()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
