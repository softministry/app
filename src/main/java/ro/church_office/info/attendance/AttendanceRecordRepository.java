package ro.church_office.info.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    default List<AttendanceRecord> findByChurchIdAndAttendanceDateBetweenOrderByAttendanceDateDescServiceSessionAscRecordedAtDesc(Long churchId, LocalDate start, LocalDate end) { return List.of(); }
    default List<AttendanceRecord> findTop20ByChurchIdAndPerson_IdOrderByAttendanceDateDescServiceSessionAscRecordedAtDesc(Long churchId, Long personId){ return List.of(); }
}
