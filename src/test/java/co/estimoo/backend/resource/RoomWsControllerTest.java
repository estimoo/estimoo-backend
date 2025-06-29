package co.estimoo.backend.resource;

import co.estimoo.backend.dto.*;
import co.estimoo.backend.model.Room;
import co.estimoo.backend.model.UserSession;
import co.estimoo.backend.model.VoteValue;
import co.estimoo.backend.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomWsControllerTest {

    @InjectMocks
    private RoomWsController roomWsController;

    @Mock
    private RoomService roomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private Room testRoom;
    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setRoomCode("TEST123");
        testRoom.setRoomName("Test Room");
        testRoom.setUsers(new HashMap<>());
        testRoom.setVotesRevealed(false);
        testRoom.setLastActivity(LocalDateTime.now());

        sessionAttributes = new HashMap<>();
        sessionAttributes.put("sessionId", "session123");
    }

    @Test
    void testJoinRoom_Success() {
        JoinRoomMessage joinMessage = new JoinRoomMessage();
        joinMessage.setRoomCode("TEST123");
        joinMessage.setNickname("TestUser");

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.joinRoom(joinMessage, sessionAttributes);

        verify(roomService).getRoom("TEST123");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/TEST123"), any(RoomStateMessage.class));
        assertTrue(testRoom.getUsers().containsKey("session123"));
        assertEquals("TestUser", testRoom.getUsers().get("session123").getNickname());
    }

    @Test
    void testJoinRoom_NoSessionId() {
        JoinRoomMessage joinMessage = new JoinRoomMessage();
        joinMessage.setRoomCode("TEST123");
        joinMessage.setNickname("TestUser");

        roomWsController.joinRoom(joinMessage, new HashMap<>());

        verify(roomService, never()).getRoom(anyString());
        verify(messagingTemplate, never())
                .convertAndSend(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void testVote_Success() {
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode("TEST123");
        voteMessage.setVote("5");

        UserSession user = new UserSession("session123", "TestUser", null);
        testRoom.getUsers().put("session123", user);

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.vote(voteMessage, sessionAttributes);

        assertEquals(VoteValue.FIVE, user.getVote());
        verify(messagingTemplate).convertAndSend(eq("/topic/room/TEST123"), any(RoomStateMessage.class));
    }

    @Test
    void testVote_ToggleSameVote() {
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode("TEST123");
        voteMessage.setVote("5");

        UserSession user = new UserSession("session123", "TestUser", VoteValue.FIVE);
        testRoom.getUsers().put("session123", user);

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.vote(voteMessage, sessionAttributes);

        assertNull(user.getVote());
        verify(messagingTemplate).convertAndSend(eq("/topic/room/TEST123"), any(RoomStateMessage.class));
    }

    @Test
    void testVote_InvalidVote() {
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode("TEST123");
        voteMessage.setVote("INVALID");

        UserSession user = new UserSession("session123", "TestUser", null);
        testRoom.getUsers().put("session123", user);

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.vote(voteMessage, sessionAttributes);

        assertNull(user.getVote());
        verify(messagingTemplate, never())
                .convertAndSend(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void testVote_RevealedRoom_PreventsVote() {
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode("TEST123");
        voteMessage.setVote("5");

        testRoom.setVotesRevealed(true);
        UserSession user = new UserSession("session123", "TestUser", null);
        testRoom.getUsers().put("session123", user);

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.vote(voteMessage, sessionAttributes);

        assertNull(user.getVote());
        verify(messagingTemplate, never())
                .convertAndSend(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void testRevealVotes_Success() {
        RevealMessage reveal = new RevealMessage();
        reveal.setRoomCode("TEST123");

        testRoom.getUsers().put("a", new UserSession("a", "User1", VoteValue.FIVE));
        testRoom.getUsers().put("b", new UserSession("b", "User2", VoteValue.EIGHT));

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.revealVotes(reveal);

        assertTrue(testRoom.isVotesRevealed());
        verify(messagingTemplate).convertAndSend(eq("/topic/room/TEST123"), any(RoomStateMessage.class));
    }

    @Test
    void testResetVotes_Success() {
        ResetMessage reset = new ResetMessage();
        reset.setRoomCode("TEST123");

        UserSession u1 = new UserSession("a", "User1", VoteValue.FIVE);
        UserSession u2 = new UserSession("b", "User2", VoteValue.EIGHT);
        testRoom.getUsers().put("a", u1);
        testRoom.getUsers().put("b", u2);
        testRoom.setVotesRevealed(true);

        when(roomService.getRoom("TEST123")).thenReturn(testRoom);

        roomWsController.resetVotes(reset);

        assertFalse(testRoom.isVotesRevealed());
        assertNull(u1.getVote());
        assertNull(u2.getVote());
        verify(messagingTemplate).convertAndSend(eq("/topic/room/TEST123"), any(RoomStateMessage.class));
    }
}
