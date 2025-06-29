package co.estimoo.backend.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    private String roomCode;
    private String roomName;
    private Map<String, UserSession> users = new ConcurrentHashMap<>();
    private boolean votesRevealed = false;
    private LocalDateTime lastActivity = LocalDateTime.now();
}