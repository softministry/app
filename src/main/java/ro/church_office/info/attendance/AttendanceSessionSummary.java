package ro.church_office.info.attendance;

import java.time.LocalDate;

public record AttendanceSessionSummary(
        LocalDate attendanceDate,
        AttendanceSession serviceSession,
        long presentCount,
        long absentCount,
        long totalCount
) {}
