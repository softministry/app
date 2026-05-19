package ro.church_office.info.events.DAO;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ro.church_office.info.events.EventType;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; private Long churchId; private String priority; private String recurrenceType;
    private String status; private EventType eventType; private String eventName; private LocalDate openDate; private LocalDate endDate;
    @Transient
    private ChurchGroup associatedGroup;
    @Transient
    private Person implementedBy;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public String getPriority(){return priority;} public void setPriority(String p){priority=p;}
    public String getRecurrenceType(){return recurrenceType;} public void setRecurrenceType(String r){recurrenceType=r;}
    public String getStatus(){return status;} public void setStatus(String s){status=s;}
    @Enumerated(EnumType.STRING)
    public EventType getEventType(){return eventType;} public void setEventType(EventType e){eventType=e;}
    public String getEventName(){return eventName;} public void setEventName(String e){eventName=e;}
    public LocalDate getOpenDate(){return openDate;} public void setOpenDate(LocalDate d){openDate=d;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate d){endDate=d;}
    public ChurchGroup getAssociatedGroup(){return associatedGroup;} public void setAssociatedGroup(ChurchGroup g){associatedGroup=g;}
    public Person getImplementedBy(){return implementedBy;} public void setImplementedBy(Person p){implementedBy=p;}
}
