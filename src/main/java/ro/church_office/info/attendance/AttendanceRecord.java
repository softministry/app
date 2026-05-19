package ro.church_office.info.attendance;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {
    private Long id; private Long churchId; private Long sessionId;
    @Transient
    private Person person;
    private AttendanceStatus status; private LocalDate attendanceDate;
    @Transient
    private AttendanceSession serviceSession;
    @Transient
    private ChurchGroup group;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChurchId() { return churchId; }
    public void setChurchId(Long churchId) { this.churchId = churchId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    @Transient
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    @Enumerated(EnumType.STRING)
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    @Transient
    public AttendanceSession getServiceSession(){return serviceSession;} public void setServiceSession(AttendanceSession s){serviceSession=s;}
    @Transient
    public ChurchGroup getGroup(){return group;} public void setGroup(ChurchGroup g){group=g;}
}
