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

    /**
     * Yeni oda oluşturur ve roomCode’a göre saklar.
     * @param roomName Kullanıcının girdiği görünen oda adı
     * @return Room nesnesi
     */
    public Room createRoom(String roomName) {
        String roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setRoomName(roomName);
        rooms.put(roomCode, room);
        return room;
    }

    /**
     * roomCode ile odanın getirilmesini sağlar.
     * @param roomCode URL'den gelen benzersiz oda kodu
     * @return Room ya da null
     */
    public Room getRoom(String roomCode) {
        return rooms.get(roomCode);
    }

    public void removeRoom(String roomCode) {
        rooms.remove(roomCode);
    }
}
