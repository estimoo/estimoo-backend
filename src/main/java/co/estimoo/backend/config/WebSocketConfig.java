package co.estimoo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // mesaj yayını yapılacak alan
        config.setApplicationDestinationPrefixes("/app"); // client → server mesaj prefix
        config.setUserDestinationPrefix("/user"); // bireysel cevaplar
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new SessionHandshakeInterceptor())
                .withSockJS();
    }
}
