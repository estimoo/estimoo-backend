package co.estimoo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
            "https://api.estimoo.co",
            "http://api.estimoo.co", 
            "https://estimoo.co",
            "http://estimoo.co",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080"
    };

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // mesaj yayını yapılacak alan
        config.setApplicationDestinationPrefixes("/app"); // client → server mesaj prefix
        config.setUserDestinationPrefix("/user"); // bireysel cevaplar
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .addInterceptors(new SessionHandshakeInterceptor())
                .withSockJS();
    }
}
