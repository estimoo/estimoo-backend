package co.estimoo.backend.resource;

import co.estimoo.backend.dto.*;
import co.estimoo.backend.model.Room;
import co.estimoo.backend.model.UserSession;
import co.estimoo.backend.model.VoteValue;
import co.estimoo.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RoomWsController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/join")
    public void joinRoom(@Payload JoinRoomMessage msg, @Header("simpSessionAttributes") Map<String, Object> attributes) {
        String sessionId = (String) attributes.get("sessionId");
        // Fallback for integration tests: try to get from nativeHeaders
        if (sessionId == null && attributes.get("nativeHeaders") instanceof Map nativeHeaders) {
            Object testSessionId = ((Map<?, ?>) nativeHeaders).get("test-session-id");
            if (testSessionId instanceof java.util.List list && !list.isEmpty()) {
                sessionId = (String) list.get(0);
            }
        }
        if (sessionId == null) return;

        Room room = roomService.getRoom(msg.getRoomCode());
        if (room == null) return;

        room.getUsers().put(sessionId, new UserSession(sessionId, msg.getNickname(), null));

        RoomStateMessage state = new RoomStateMessage();
        state.setUsers(
                room.getUsers().values().stream().map(user -> {
                    RoomStateMessage.UserVoteInfo info = new RoomStateMessage.UserVoteInfo();
                    info.setNickname(user.getNickname());
                    info.setVote(null); // JOIN aşamasında oy zaten yok
                    return info;
                }).toList()
        );
        state.setVotesRevealed(room.isVotesRevealed());

        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomCode(), state);
    }

    @MessageMapping("/vote")
    public void vote(@Payload VoteMessage msg, @Header("simpSessionAttributes") Map<String, Object> attributes) {
        String sessionId = (String) attributes.get("sessionId");
        // Fallback for integration tests: try to get from nativeHeaders
        if (sessionId == null && attributes.get("nativeHeaders") instanceof Map nativeHeaders) {
            Object testSessionId = ((Map<?, ?>) nativeHeaders).get("test-session-id");
            if (testSessionId instanceof java.util.List list && !list.isEmpty()) {
                sessionId = (String) list.get(0);
            }
        }
        if (sessionId == null) return;

        Room room = roomService.getRoom(msg.getRoomCode());
        if (room == null) return;

        UserSession user = room.getUsers().get(sessionId);
        if (user == null) return;

        // Sadece oylar açılmadıysa oylamaya izin ver
        if (room.isVotesRevealed()) return;

        VoteValue newVote;
        try {
            newVote = VoteValue.fromLabel(msg.getVote());
        } catch (IllegalArgumentException e) {
            return; // Geçersiz oy, işlem yapma
        }

        VoteValue currentVote = user.getVote();

        if (newVote == currentVote) {
            user.setVote(null); // Aynı oya tekrar tıklanırsa kaldır
        } else {
            user.setVote(newVote); // Yeni oy olarak güncelle
        }

        room.setLastActivity(LocalDateTime.now());

        // Yeni oylama durumunu hazırla
        RoomStateMessage state = new RoomStateMessage();
        state.setUsers(
                room.getUsers().values().stream().map(u -> {
                    RoomStateMessage.UserVoteInfo info = new RoomStateMessage.UserVoteInfo();
                    info.setNickname(u.getNickname());
                    info.setVote(null); // henüz reveal edilmediği için oylar gizli
                    return info;
                }).toList()
        );
        state.setVotesRevealed(room.isVotesRevealed());

        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomCode(), state);
    }


    @MessageMapping("/reveal")
    public void revealVotes(@Payload RevealMessage msg) {
        Room room = roomService.getRoom(msg.getRoomCode());
        if (room == null) return;

        room.setVotesRevealed(true);
        room.setLastActivity(LocalDateTime.now());

        RoomStateMessage state = new RoomStateMessage();

        state.setUsers(
                room.getUsers().values().stream().map(user -> {
                    RoomStateMessage.UserVoteInfo info = new RoomStateMessage.UserVoteInfo();
                    info.setNickname(user.getNickname());
                    info.setVote(user.getVote() != null ? user.getVote().getLabel() : null); // enum'dan string
                    return info;
                }).toList()
        );
        state.setVotesRevealed(room.isVotesRevealed());

        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomCode(), state);
    }

    @MessageMapping("/reset")
    public void resetVotes(@Payload ResetMessage msg) {
        Room room = roomService.getRoom(msg.getRoomCode());
        if (room == null) return;

        // Tüm kullanıcıların oyunu sıfırla
        room.getUsers().values().forEach(user -> user.setVote(null));
        room.setVotesRevealed(false);
        room.setLastActivity(LocalDateTime.now());

        // Yeni RoomStateMessage hazırla (oylar gizli)
        RoomStateMessage state = new RoomStateMessage();
        state.setUsers(
                room.getUsers().values().stream().map(u -> {
                    RoomStateMessage.UserVoteInfo info = new RoomStateMessage.UserVoteInfo();
                    info.setNickname(u.getNickname());
                    info.setVote(null); // hepsi null olacak zaten
                    return info;
                }).toList()
        );
        state.setVotesRevealed(room.isVotesRevealed());

        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomCode(), state);
    }
}