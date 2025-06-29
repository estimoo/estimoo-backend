package co.estimoo.backend.dto;

import lombok.Data;

@Data
public class VoteMessage {
    private String roomCode;
    private String vote;
}