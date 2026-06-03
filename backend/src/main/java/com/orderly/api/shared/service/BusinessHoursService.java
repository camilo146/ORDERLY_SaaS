package com.orderly.api.shared.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-tenant business hours. Hours are stored in memory.
 * A null schedule means 24/7 (no restriction).
 */
@Service
public class BusinessHoursService {

    private final Map<UUID, List<DaySchedule>> hoursMap = new ConcurrentHashMap<>();

    public List<DaySchedule> getHours(UUID businessId) {
        return hoursMap.getOrDefault(businessId, defaultSchedule());
    }

    public List<DaySchedule> saveHours(UUID businessId, List<DaySchedule> schedule) {
        hoursMap.put(businessId, schedule);
        return schedule;
    }

    /**
     * Returns true if the business is currently open based on the configured schedule
     * and the business timezone.
     */
    public boolean isOpenNow(UUID businessId, String timezone) {
        List<DaySchedule> schedule = hoursMap.get(businessId);
        if (schedule == null || schedule.isEmpty()) return true;

        ZoneId zone = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        String today = now.getDayOfWeek().name();
        LocalTime currentTime = now.toLocalTime();

        return schedule.stream()
                .filter(d -> d.day().equalsIgnoreCase(today))
                .findFirst()
                .map(d -> {
                    if (d.closed()) return false;
                    LocalTime open = LocalTime.parse(d.openTime());
                    LocalTime close = LocalTime.parse(d.closeTime());
                    return !currentTime.isBefore(open) && currentTime.isBefore(close);
                })
                .orElse(true);
    }

    public String buildHoursMessage(UUID businessId) {
        List<DaySchedule> schedule = getHours(businessId);
        if (schedule.isEmpty()) return "Atendemos todos los días.";

        StringBuilder sb = new StringBuilder();
        for (DaySchedule d : schedule) {
            sb.append(translateDay(d.day())).append(": ");
            if (d.closed()) {
                sb.append("Cerrado");
            } else {
                sb.append(d.openTime()).append(" – ").append(d.closeTime());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("America/Bogota");
        }
    }

    private String translateDay(String day) {
        return switch (day.toUpperCase()) {
            case "MONDAY" -> "Lunes";
            case "TUESDAY" -> "Martes";
            case "WEDNESDAY" -> "Miércoles";
            case "THURSDAY" -> "Jueves";
            case "FRIDAY" -> "Viernes";
            case "SATURDAY" -> "Sábado";
            case "SUNDAY" -> "Domingo";
            default -> day;
        };
    }

    private List<DaySchedule> defaultSchedule() {
        return List.of();
    }

    public record DaySchedule(String day, String openTime, String closeTime, boolean closed) {}
}
