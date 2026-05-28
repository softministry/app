package ro.church_office.info.events.DAO;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import ro.church_office.info.events.Priority;
import ro.church_office.info.events.PriorityDatabaseConverter;
import ro.church_office.info.events.RecurrenceType;
import ro.church_office.info.events.EventType;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long churchId;
    @Convert(converter = PriorityDatabaseConverter.class)
    private Priority priority;
    @Enumerated(EnumType.STRING)
    private RecurrenceType recurrenceType;
    private String status;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private String eventName;
    private String about;
    private LocalDate openDate;
    private LocalDate endDate;
    @ManyToOne
    private ChurchGroup associatedGroup;
    @ManyToOne
    private Person implementedBy;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public Priority getPriority(){return priority;} public void setPriority(Priority p){priority=p;}
    public RecurrenceType getRecurrenceType(){return recurrenceType;} public void setRecurrenceType(RecurrenceType r){recurrenceType=r;}
    public String getStatus(){return status;} public void setStatus(String s){status=s;}
    public EventType getEventType(){return eventType;} public void setEventType(EventType e){eventType=e;}
    public String getEventName(){return eventName;} public void setEventName(String e){eventName=e;}
    public String getAbout(){return about;} public void setAbout(String about){this.about=about;}
    public LocalDate getOpenDate(){return openDate;} public void setOpenDate(LocalDate d){openDate=d;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate d){endDate=d;}
    public ChurchGroup getAssociatedGroup(){return associatedGroup;} public void setAssociatedGroup(ChurchGroup g){associatedGroup=g;}
    public Person getImplementedBy(){return implementedBy;} public void setImplementedBy(Person p){implementedBy=p;}
}
