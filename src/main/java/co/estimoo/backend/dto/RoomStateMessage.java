package co.estimoo.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoomStateMessage {
    private List<UserVoteInfo> users;

    @Data
    public static class UserVoteInfo {
        private String nickname;
        private String vote;
    }
}