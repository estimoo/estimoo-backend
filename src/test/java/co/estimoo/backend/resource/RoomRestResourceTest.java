package co.estimoo.backend.resource;

import co.estimoo.backend.model.Room;
import co.estimoo.backend.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomRestResourceTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomRestResource roomRestResource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomRestResource).build();
    }

    @Test
    void createRoom_ShouldReturnCreatedRoom() throws Exception {
        // Given
        Room expectedRoom = new Room();
        expectedRoom.setRoomCode("ABC123");
        when(roomService.createRoom()).thenReturn(expectedRoom);

        // When & Then
        mockMvc.perform(post("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.votesRevealed").value(false))
                .andExpect(jsonPath("$.lastActivity").exists());

        verify(roomService, times(1)).createRoom();
    }

    @Test
    void getRoom_WithValidRoomCode_ShouldReturnRoom() throws Exception {
        // Given
        String roomCode = "ABC123";
        Room expectedRoom = new Room();
        expectedRoom.setRoomCode(roomCode);
        when(roomService.getRoom(roomCode)).thenReturn(expectedRoom);

        // When & Then
        mockMvc.perform(get("/rooms/{roomCode}", roomCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.votesRevealed").value(false))
                .andExpect(jsonPath("$.lastActivity").exists());

        verify(roomService, times(1)).getRoom(roomCode);
    }

    @Test
    void getRoom_WithNonExistentRoomCode_ShouldReturnNull() throws Exception {
        // Given
        String roomCode = "NONEXISTENT";
        when(roomService.getRoom(roomCode)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/rooms/{roomCode}", roomCode))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(roomService, times(1)).getRoom(roomCode);
    }

    @Test
    void createRoom_ShouldHandleServiceException() {
        // Given
        when(roomService.createRoom()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            roomRestResource.createRoom();
            assert false : "Should have thrown an exception";
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Service error");
        }

        verify(roomService, times(1)).createRoom();
    }

    @Test
    void getRoom_ShouldHandleServiceException() {
        // Given
        String roomCode = "ABC123";
        when(roomService.getRoom(roomCode)).thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            roomRestResource.getRoom(roomCode);
            assert false : "Should have thrown an exception";
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Service error");
        }

        verify(roomService, times(1)).getRoom(roomCode);
    }
}
