package co.estimoo.backend.resource;

import co.estimoo.backend.model.Room;
import co.estimoo.backend.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomRestResourceTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomRestResource roomRestResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomRestResource).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createRoom_ShouldReturnCreatedRoom() throws Exception {
        // Given
        String roomName = "Deneme";
        Room expectedRoom = new Room();
        expectedRoom.setRoomCode("ABC123");
        expectedRoom.setRoomName(roomName);

        when(roomService.createRoom(roomName)).thenReturn(expectedRoom);

        // When & Then
        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"Deneme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.roomName").value("Deneme"))
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.votesRevealed").value(false))
                .andExpect(jsonPath("$.lastActivity").exists());

        verify(roomService, times(1)).createRoom("Deneme");
    }

    @Test
    void getRoom_WithValidRoomCode_ShouldReturnRoom() throws Exception {
        String roomCode = "ABC123";
        Room expectedRoom = new Room();
        expectedRoom.setRoomCode(roomCode);

        when(roomService.getRoom(roomCode)).thenReturn(expectedRoom);

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
        String roomCode = "NONEXISTENT";
        when(roomService.getRoom(roomCode)).thenReturn(null);

        mockMvc.perform(get("/rooms/{roomCode}", roomCode))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(roomService, times(1)).getRoom(roomCode);
    }

    @Test
    void createRoom_ShouldHandleServiceException() throws Exception {
        when(roomService.createRoom("Deneme")).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"Deneme\"}"))
                .andExpect(status().isInternalServerError());

        verify(roomService, times(1)).createRoom("Deneme");
    }

    @Test
    void getRoom_ShouldHandleServiceException() throws Exception {
        String roomCode = "ABC123";
        when(roomService.getRoom(roomCode)).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/rooms/{roomCode}", roomCode))
                .andExpect(status().isInternalServerError());

        verify(roomService, times(1)).getRoom(roomCode);
    }
}
