package romelt_techcare.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSOCKET CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Configures STOMP over WebSocket for real-time administrator
 * notifications.
 *
 * Responsibilities:
 * - Exposes the WebSocket handshake endpoint.
 * - Enables administrator-specific user destinations.
 * - Enables the built-in simple message broker.
 * - Restricts WebSocket connections to the configured frontend.
 *
 * Endpoints:
 *
 * WebSocket handshake:
 * /api/v1/ws
 *
 * Administrator subscription:
 * /user/queue/notifications
 *
 * Application destination prefix:
 * /app
 *
 * Important:
 * PostgreSQL remains the source of truth for notifications.
 * WebSocket messages only provide immediate portal updates.
 * ================================================================
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration
        implements WebSocketMessageBrokerConfigurer {

    private final String allowedOrigin;

    public WebSocketConfiguration(

            @Value("${app.cors.allowed-origin:http://localhost:5173}")
            String allowedOrigin
    ) {
        this.allowedOrigin =
                requireText(
                        allowedOrigin,
                        "WebSocket allowed origin is required."
                );
    }

    /**
     * Configures STOMP destinations and the built-in message broker.
     */
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {
        /*
         * Messages sent to administrator-specific queues are handled
         * by Spring's user-destination infrastructure.
         */
        registry.enableSimpleBroker(
                "/queue",
                "/topic"
        );

        /*
         * Client messages addressed to backend message handlers begin
         * with /app.
         */
        registry.setApplicationDestinationPrefixes(
                "/app"
        );

        /*
         * Enables subscriptions such as:
         *
         * /user/queue/notifications
         */
        registry.setUserDestinationPrefix(
                "/user"
        );
    }

    /**
     * Registers the WebSocket/STOMP handshake endpoint.
     */
    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        registry.addEndpoint(
                        "/api/v1/ws"
                )
                .setAllowedOrigins(
                        allowedOrigin
                );
    }

    private String requireText(
            String value,
            String message
    ) {
        if (
                value == null
                        || value.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}