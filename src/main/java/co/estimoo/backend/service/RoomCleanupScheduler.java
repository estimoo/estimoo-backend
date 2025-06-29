package co.estimoo.backend.service;

import co.estimoo.backend.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCleanupScheduler {

    private final RoomService roomService;

    @Scheduled(fixedRate = 60_000) // her 60 saniyede bir çalışır
    public void cleanUpInactiveRooms() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;

        Iterator<Map.Entry<String, Room>> iterator = roomService.getRooms().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Room> entry = iterator.next();
            Room room = entry.getValue();

            if (room.getLastActivity().isBefore(now.minusMinutes(10))) {
                log.info("Inaktif oda temizleniyor: {}", room.getRoomCode());
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Toplam {} inaktif oda silindi.", removed);
        }
    }
}
