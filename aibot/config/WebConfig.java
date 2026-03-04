package com.aibot.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        try {
            SslContext sslContext = SslContextBuilder.forClient().build();

            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext))
                    .responseTimeout(Duration.ofSeconds(30))
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                    );

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .filter(logRequest())
                    .filter(logResponse());

        } catch (SSLException e) {
            throw new RuntimeException("Ошибка создания SSLContext", e);
        }
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    // ✅ Используем slf4j logger вместо System.out.println
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("🌐 Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> {
                        if (!name.equalsIgnoreCase("Authorization")) {
                            log.debug("  {}: {}", name, value);
                        }
                    })
            );
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("📥 Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}