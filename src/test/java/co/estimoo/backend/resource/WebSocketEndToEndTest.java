package co.estimoo.backend.resource;

import co.estimoo.backend.dto.JoinRoomMessage;
import co.estimoo.backend.dto.ResetMessage;
import co.estimoo.backend.dto.RevealMessage;
import co.estimoo.backend.dto.VoteMessage;
import co.estimoo.backend.model.Room;
import co.estimoo.backend.model.UserSession;
import co.estimoo.backend.model.VoteValue;
import co.estimoo.backend.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WebSocketEndToEndTest {

    @Autowired
    private RoomWsController roomWsController;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Map<String, Room> testRooms;
    private Map<String, Map<String, Object>> sessionAttributes;

    @BeforeEach
    void setUp() {
        testRooms = new HashMap<>();
        sessionAttributes = new HashMap<>();

        // Create 3 test rooms and store their actual room codes
        String[] roomNames = {"Development Team", "Design Team", "QA Team"};

        for (int i = 0; i < roomNames.length; i++) {
            Room room = roomService.createRoom(roomNames[i]);
            testRooms.put(room.getRoomCode(), room);
        }

        // Create session attributes for multiple users
        String[] sessionIds = {"session1", "session2", "session3", "session4", "session5"};
        for (String sessionId : sessionIds) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sessionId", sessionId);
            sessionAttributes.put(sessionId, attributes);
        }
    }

    @Test
    void testEndToEndPlanningPokerSession() {
        // Get actual room codes from created rooms
        String[] roomCodes = testRooms.keySet().toArray(new String[0]);

        // Test data for 3 rooms with 4-5 participants each
        String[][] participants = {
                {"Alice", "Bob", "Charlie", "Diana"},           // Room 1: 4 participants
                {"Eve", "Frank", "Grace", "Henry", "Ivy"},      // Room 2: 5 participants
                {"Jack", "Kate", "Liam", "Mia", "Noah"}         // Room 3: 5 participants
        };

        String[][] votes = {
                {"5", "8", "3", "5"},                           // Room 1 votes
                {"13", "8", "5", "8", "13"},                    // Room 2 votes
                {"3", "5", "8", "3", "5"}                       // Room 3 votes
        };

        // Phase 1: Join all participants to their respective rooms
        joinAllParticipants(roomCodes, participants);

        // Verify all participants joined successfully
        verifyAllParticipantsJoined(roomCodes, participants);

        // Phase 2: All participants vote
        castAllVotes(roomCodes, participants, votes);

        // Verify votes are cast but not revealed
        verifyVotesCastButNotRevealed(roomCodes, participants, votes);

        // Phase 3: Reveal votes in all rooms
        revealAllVotes(roomCodes);

        // Verify votes are revealed
        verifyVotesRevealed(roomCodes, participants, votes);

        // Phase 4: Reset votes in all rooms
        resetAllVotes(roomCodes);

        // Verify votes are reset
        verifyVotesReset(roomCodes, participants);

        // Phase 5: Test vote toggling (same vote removes it)
        testVoteToggling(roomCodes[0], participants[0][0], "5");

        // Phase 6: Test invalid vote handling
        testInvalidVoteHandling(roomCodes[0], participants[0][1], "INVALID");

        // Phase 7: Test voting when votes are already revealed
        testVotingWhenRevealed(roomCodes[0], participants[0][2], "8");
    }

    private void joinAllParticipants(String[] roomCodes, String[][] participants) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];

            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String nickname = roomParticipants[userIndex];
                String sessionId = "session" + (userIndex + 1);

                JoinRoomMessage joinMessage = new JoinRoomMessage();
                joinMessage.setRoomCode(roomCode);
                joinMessage.setNickname(nickname);

                roomWsController.joinRoom(joinMessage, sessionAttributes.get(sessionId));
            }
        }
    }

    private void verifyAllParticipantsJoined(String[] roomCodes, String[][] participants) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];
            Room room = roomService.getRoom(roomCode);

            assertNotNull(room, "Room " + roomCode + " should exist");
            assertEquals(roomParticipants.length, room.getUsers().size(),
                    "Room " + roomCode + " should have " + roomParticipants.length + " participants");

            // Verify each participant is in the room
            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String sessionId = "session" + (userIndex + 1);
                String expectedNickname = roomParticipants[userIndex];

                UserSession userSession = room.getUsers().get(sessionId);
                assertNotNull(userSession, "User session should exist for " + sessionId);
                assertEquals(expectedNickname, userSession.getNickname(),
                        "Nickname should match for user " + sessionId);
                assertNull(userSession.getVote(), "User should not have voted yet");
            }
        }
    }

    private void castAllVotes(String[] roomCodes, String[][] participants, String[][] votes) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];
            String[] roomVotes = votes[roomIndex];

            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String sessionId = "session" + (userIndex + 1);
                String vote = roomVotes[userIndex];

                VoteMessage voteMessage = new VoteMessage();
                voteMessage.setRoomCode(roomCode);
                voteMessage.setVote(vote);

                roomWsController.vote(voteMessage, sessionAttributes.get(sessionId));
            }
        }
    }

    private void verifyVotesCastButNotRevealed(String[] roomCodes, String[][] participants, String[][] votes) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];
            String[] roomVotes = votes[roomIndex];
            Room room = roomService.getRoom(roomCode);

            assertFalse(room.isVotesRevealed(), "Votes should not be revealed in room " + roomCode);

            // Verify each participant has voted
            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String sessionId = "session" + (userIndex + 1);
                String expectedVote = roomVotes[userIndex];

                UserSession userSession = room.getUsers().get(sessionId);
                assertNotNull(userSession.getVote(), "User should have voted");
                assertEquals(VoteValue.fromLabel(expectedVote), userSession.getVote(),
                        "Vote should match for user " + sessionId);
            }
        }
    }

    private void revealAllVotes(String[] roomCodes) {
        for (String roomCode : roomCodes) {
            RevealMessage revealMessage = new RevealMessage();
            revealMessage.setRoomCode(roomCode);

            roomWsController.revealVotes(revealMessage);
        }
    }

    private void verifyVotesRevealed(String[] roomCodes, String[][] participants, String[][] votes) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];
            String[] roomVotes = votes[roomIndex];
            Room room = roomService.getRoom(roomCode);

            assertTrue(room.isVotesRevealed(), "Votes should be revealed in room " + roomCode);

            // Verify all votes are still there
            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String sessionId = "session" + (userIndex + 1);
                String expectedVote = roomVotes[userIndex];

                UserSession userSession = room.getUsers().get(sessionId);
                assertNotNull(userSession.getVote(), "User should still have vote after reveal");
                assertEquals(VoteValue.fromLabel(expectedVote), userSession.getVote(),
                        "Vote should still match after reveal for user " + sessionId);
            }
        }
    }

    private void resetAllVotes(String[] roomCodes) {
        for (String roomCode : roomCodes) {
            ResetMessage resetMessage = new ResetMessage();
            resetMessage.setRoomCode(roomCode);

            roomWsController.resetVotes(resetMessage);
        }
    }

    private void verifyVotesReset(String[] roomCodes, String[][] participants) {
        for (int roomIndex = 0; roomIndex < roomCodes.length; roomIndex++) {
            String roomCode = roomCodes[roomIndex];
            String[] roomParticipants = participants[roomIndex];
            Room room = roomService.getRoom(roomCode);

            assertFalse(room.isVotesRevealed(), "Votes should not be revealed after reset in room " + roomCode);

            // Verify all votes are reset
            for (int userIndex = 0; userIndex < roomParticipants.length; userIndex++) {
                String sessionId = "session" + (userIndex + 1);

                UserSession userSession = room.getUsers().get(sessionId);
                assertNull(userSession.getVote(), "User vote should be reset for " + sessionId);
            }
        }
    }

    private void testVoteToggling(String roomCode, String nickname, String vote) {
        String sessionId = "session1";
        Room room = roomService.getRoom(roomCode);

        // First vote
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode(roomCode);
        voteMessage.setVote(vote);

        roomWsController.vote(voteMessage, sessionAttributes.get(sessionId));

        UserSession userSession = room.getUsers().get(sessionId);
        assertEquals(VoteValue.fromLabel(vote), userSession.getVote(), "First vote should be set");

        // Same vote again should remove it
        roomWsController.vote(voteMessage, sessionAttributes.get(sessionId));

        assertNull(userSession.getVote(), "Same vote should remove the vote");
    }

    private void testInvalidVoteHandling(String roomCode, String nickname, String invalidVote) {
        String sessionId = "session2";
        Room room = roomService.getRoom(roomCode);
        UserSession userSession = room.getUsers().get(sessionId);

        // Clear any existing vote
        userSession.setVote(null);

        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode(roomCode);
        voteMessage.setVote(invalidVote);

        roomWsController.vote(voteMessage, sessionAttributes.get(sessionId));

        assertNull(userSession.getVote(), "Invalid vote should not be set");
    }

    private void testVotingWhenRevealed(String roomCode, String nickname, String vote) {
        String sessionId = "session3";
        Room room = roomService.getRoom(roomCode);
        UserSession userSession = room.getUsers().get(sessionId);

        // Ensure votes are revealed
        room.setVotesRevealed(true);

        // Clear any existing vote
        userSession.setVote(null);

        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoomCode(roomCode);
        voteMessage.setVote(vote);

        roomWsController.vote(voteMessage, sessionAttributes.get(sessionId));

        assertNull(userSession.getVote(), "Vote should not be allowed when votes are revealed");
    }
} 