package ro.church_office.info.attendance;

import ro.church_office.info.groups.ChurchGroup;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    default List<AttendanceSession> getSessionsForChurch(Long churchId) { return List.of(); }
    default AttendanceSession parseSession(String session) { return AttendanceSession.MORNING; }
    default List<AttendanceSessionSummary> registeredSessions(Long churchId) { return List.of(); }
    default List<AttendanceSessionSummary> registeredSessionsForGroup(Long churchId, Long groupId) { return List.of(); }
    default void savePresence(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession, ChurchGroup group, List<Long> personIds, List<Long> presentPersonIds) {}
    default int deleteSession(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession) { return 0; }
    default Map<Long, AttendanceRecord> recordsByPerson(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession) { return Map.of(); }
    default List<Object> recentSummaries(Long churchId, int limit) { return List.of(); }
}
