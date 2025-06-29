package co.estimoo.backend.resource;

import co.estimoo.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final RoomService roomService;

    @GetMapping
    public Map<String, Object> health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        int totalUsers = roomService.getRooms().values().stream()
                .mapToInt(room -> room.getUsers().size())
                .sum();

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("activeRooms", roomService.getRooms().size());
        health.put("totalUsers", totalUsers);
        health.put("memoryUsageMB", usedMemory / (1024 * 1024));
        health.put("maxMemoryMB", maxMemory / (1024 * 1024));
        health.put("memoryUsagePercent", (double) usedMemory / maxMemory * 100);
        health.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        
        log.info("Health check - Rooms: {}, Users: {}, Memory: {}MB/{}MB ({}%)", 
                roomService.getRooms().size(), totalUsers, 
                usedMemory / (1024 * 1024), maxMemory / (1024 * 1024),
                String.format("%.1f", (double) usedMemory / maxMemory * 100));
        
        return health;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("rooms", roomService.getRooms().size());
        metrics.put("users", roomService.getRooms().values().stream()
                .mapToInt(room -> room.getUsers().size()).sum());
        metrics.put("avgUsersPerRoom", roomService.getRooms().isEmpty() ? 0 : 
                (double) roomService.getRooms().values().stream()
                        .mapToInt(room -> room.getUsers().size()).sum() / roomService.getRooms().size());
        return metrics;
    }
} 