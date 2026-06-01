package ro.church_office.info.attendance;

import org.springframework.stereotype.Service;
import ro.church_office.info.groups.ChurchGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InMemoryAttendanceService implements AttendanceService {

    private final AttendanceRecordRepository repository;

    public InMemoryAttendanceService(AttendanceRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AttendanceSession> getSessionsForChurch(Long churchId) {
        return List.of(AttendanceSession.values());
    }

    @Override
    public AttendanceSession parseSession(String session) {
        if (session == null || session.isBlank()) return AttendanceSession.MORNING;
        try {
            return AttendanceSession.valueOf(session.trim().toUpperCase());
        } catch (Exception ex) {
            return AttendanceSession.MORNING;
        }
    }

    @Override
    public List<AttendanceSessionSummary> registeredSessions(Long churchId) {
        return buildSummaries(repository.findByChurchIdOrderByAttendanceDateDescSessionAsc(churchId), null);
    }

    @Override
    public List<AttendanceSessionSummary> registeredSessionsForGroup(Long churchId, Long groupId) {
        List<AttendanceRecord> all = repository.findByChurchIdOrderByAttendanceDateDescSessionAsc(churchId);
        return buildSummaries(all, groupId);
    }

    private List<AttendanceSessionSummary> buildSummaries(List<AttendanceRecord> records, Long groupId) {
        Map<String, long[]> counters = new LinkedHashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getAttendanceDate() == null || r.getSession() == null) continue;
            String key = r.getAttendanceDate() + "|" + r.getSession().name();
            counters.computeIfAbsent(key, k -> new long[2]);
            if (r.getStatus() == AttendanceStatus.PRESENT) counters.get(key)[0]++;
            else counters.get(key)[1]++;
        }
        List<AttendanceSessionSummary> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : counters.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            LocalDate date = LocalDate.parse(parts[0]);
            AttendanceSession session = AttendanceSession.valueOf(parts[1]);
            long present = e.getValue()[0];
            long absent  = e.getValue()[1];
            result.add(new AttendanceSessionSummary(date, session, present, absent, present + absent));
        }
        return result;
    }

    @Override
    public Map<Long, AttendanceRecord> recordsByPerson(Long churchId, LocalDate attendanceDate, AttendanceSession session) {
        List<AttendanceRecord> records = repository.findByChurchIdAndAttendanceDateAndSession(churchId, attendanceDate, session);
        Map<Long, AttendanceRecord> map = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getPersonId() != null) map.put(r.getPersonId(), r);
        }
        return map;
    }

    @Override
    public void savePresence(Long churchId, LocalDate attendanceDate, AttendanceSession session,
                             ChurchGroup group, List<Long> personIds, List<Long> presentPersonIds) {
        if (personIds == null || personIds.isEmpty()) return;

        List<AttendanceRecord> existing = repository.findByChurchIdAndAttendanceDateAndSession(churchId, attendanceDate, session);
        Map<Long, AttendanceRecord> existingByPerson = new HashMap<>();
        for (AttendanceRecord r : existing) {
            if (r.getPersonId() != null) existingByPerson.put(r.getPersonId(), r);
        }

        List<Long> present = presentPersonIds == null ? List.of() : presentPersonIds;
        List<AttendanceRecord> toSave = new ArrayList<>();

        for (Long personId : personIds) {
            AttendanceRecord record = existingByPerson.getOrDefault(personId, new AttendanceRecord());
            record.setChurchId(churchId);
            record.setPersonId(personId);
            record.setAttendanceDate(attendanceDate);
            record.setSession(session);
            if (record.getSessionId() == null) record.setSessionId((long) session.ordinal() + 1);
            record.setStatus(present.contains(personId) ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
            toSave.add(record);
        }
        repository.saveAll(toSave);
    }

    @Override
    public int deleteSession(Long churchId, LocalDate attendanceDate, AttendanceSession attendanceSession) {
        List<AttendanceRecord> records = repository.findByChurchIdAndAttendanceDateAndSession(churchId, attendanceDate, attendanceSession);
        repository.deleteAll(records);
        return records.size();
    }
}
