package ro.church_office.info.attendance;

import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

public class AttendanceRecord {
    private Long id; private Long churchId; private Long sessionId; private Person person; private AttendanceStatus status; private LocalDate attendanceDate;
    private AttendanceSession serviceSession; private ChurchGroup group;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChurchId() { return churchId; }
    public void setChurchId(Long churchId) { this.churchId = churchId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public AttendanceSession getServiceSession(){return serviceSession;} public void setServiceSession(AttendanceSession s){serviceSession=s;}
    public ChurchGroup getGroup(){return group;} public void setGroup(ChurchGroup g){group=g;}
}
