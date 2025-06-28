package co.estimoo.backend.service;

import co.estimoo.backend.model.Room;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    // roomCode → Room
    @Getter
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom() {
        String roomCode = UUID.randomUUID().toString().substring(0, 6);
        Room room = new Room();
        room.setRoomCode(roomCode);
        rooms.put(roomCode, room);
        return room;
    }

    public Room getRoom(String roomCode) {
        return rooms.get(roomCode);
    }

    public void removeRoom(String roomCode) {
        rooms.remove(roomCode);
    }
}

