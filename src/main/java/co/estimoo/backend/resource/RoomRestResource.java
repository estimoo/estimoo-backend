package co.estimoo.backend.resource;

import co.estimoo.backend.model.Room;
import co.estimoo.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomRestResource {

    private final RoomService roomService;

    @PostMapping
    public Room createRoom() {
        return roomService.createRoom();
    }

    @GetMapping("/{roomCode}")
    public Room getRoom(@PathVariable String roomCode) {
        return roomService.getRoom(roomCode);
    }
}
