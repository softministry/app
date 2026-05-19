package ro.church_office.info.followup;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pastoral_follow_up")
public class PastoralFollowUp {
    private Long id; private Long churchId;
    private Person person;
    private ChurchGroup group;
    private PastoralFollowUpStatus status = PastoralFollowUpStatus.OPEN;
    private LocalDate lastContactDate; private LocalDate nextContactDate; private String notes; private LocalDateTime updatedAt; private LocalDateTime createdAt = LocalDateTime.now();
    private String contactMethod;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long churchId){this.churchId=churchId;}
    @ManyToOne
    public Person getPerson(){return person;} public void setPerson(Person p){person=p;}
    @ManyToOne
    public ChurchGroup getGroup(){return group;} public void setGroup(ChurchGroup g){group=g;}
    @Enumerated(EnumType.STRING)
    public PastoralFollowUpStatus getStatus(){return status;} public void setStatus(PastoralFollowUpStatus s){status=s;}
    public LocalDate getLastContactDate(){return lastContactDate;} public void setLastContactDate(LocalDate d){lastContactDate=d;}
    public LocalDate getNextContactDate(){return nextContactDate;} public void setNextContactDate(LocalDate d){nextContactDate=d;}
    public String getNotes(){return notes;} public void setNotes(String n){notes=n;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){updatedAt=t;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){createdAt=t;}
    public String getContactMethod(){return contactMethod;} public void setContactMethod(String c){contactMethod=c;}
}
