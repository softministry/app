package ro.church_office.info.attendance;

import org.springframework.stereotype.Service;
import ro.church_office.info.groups.ChurchGroup;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InMemoryAttendanceService implements AttendanceService {

    @Override
    public List<AttendanceSession> getSessionsForChurch(Long churchId) {
        return List.of(AttendanceSession.values());
    }

    @Override
    public AttendanceSession parseSession(String session) {
        if (session == null || session.isBlank()) {
            return AttendanceSession.MORNING;
        }
        try {
            return AttendanceSession.valueOf(session.trim().toUpperCase());
        } catch (Exception ex) {
            return AttendanceSession.MORNING;
        }
    }

    @Override
    public List<AttendanceSession> registeredSessions(Long churchId) {
        return List.of(AttendanceSession.values());
    }

    @Override
    public List<AttendanceSession> registeredSessionsForGroup(Long churchId, Long groupId) {
        return List.of(AttendanceSession.values());
    }

    @Override
    public void savePresence(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession, ChurchGroup group, List<Long> personIds, List<Long> presentPersonIds) {
        // Compatibility no-op implementation.
    }

    @Override
    public Map<Long, AttendanceRecord> recordsByPerson(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession) {
        return new HashMap<>();
    }
}
