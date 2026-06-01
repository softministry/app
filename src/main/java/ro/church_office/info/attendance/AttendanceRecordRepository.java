package ro.church_office.info.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByChurchIdAndAttendanceDateAndSession(
            Long churchId, LocalDate attendanceDate, AttendanceSession session);

    List<AttendanceRecord> findByChurchIdOrderByAttendanceDateDescSessionAsc(Long churchId);

    List<AttendanceRecord> findByChurchIdAndAttendanceDateBetweenOrderByAttendanceDateDescSessionAsc(
            Long churchId, LocalDate start, LocalDate end);

    List<AttendanceRecord> findTop20ByChurchIdAndPersonIdOrderByAttendanceDateDescSessionAsc(
            Long churchId, Long personId);

    List<AttendanceRecord> findByChurchIdAndPersonIdOrderByAttendanceDateDesc(Long churchId, Long personId);
}
