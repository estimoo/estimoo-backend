package co.estimoo.backend.dto;

import lombok.Data;

@Data
public class JoinRoomMessage {
    private String roomCode;
    private String nickname;
}
