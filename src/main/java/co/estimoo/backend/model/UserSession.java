package co.estimoo.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSession {
    private String sessionId;
    private String nickname;
    private VoteValue vote;
}
