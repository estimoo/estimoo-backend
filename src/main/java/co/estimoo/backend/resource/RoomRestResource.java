package co.estimoo.backend.resource;

import co.estimoo.backend.dto.RoomCreateRequest;
import co.estimoo.backend.model.Room;
import co.estimoo.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomRestResource {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody RoomCreateRequest request) {
        Room room = roomService.createRoom(request.getRoomName());
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room != null) {
            return ResponseEntity.ok(room);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{roomCode}/exists")
    public ResponseEntity<Room> roomExists(@PathVariable String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room != null) {
            return ResponseEntity.ok(room);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
