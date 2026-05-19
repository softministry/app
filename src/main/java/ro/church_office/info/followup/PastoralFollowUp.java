package ro.church_office.info.followup;

import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PastoralFollowUp {
    private Long id; private Long churchId; private Person person; private ChurchGroup group; private PastoralFollowUpStatus status = PastoralFollowUpStatus.OPEN;
    private LocalDate lastContactDate; private LocalDate nextContactDate; private String notes; private LocalDateTime updatedAt; private LocalDateTime createdAt = LocalDateTime.now();
    private String contactMethod;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long churchId){this.churchId=churchId;}
    public Person getPerson(){return person;} public void setPerson(Person p){person=p;}
    public ChurchGroup getGroup(){return group;} public void setGroup(ChurchGroup g){group=g;}
    public PastoralFollowUpStatus getStatus(){return status;} public void setStatus(PastoralFollowUpStatus s){status=s;}
    public LocalDate getLastContactDate(){return lastContactDate;} public void setLastContactDate(LocalDate d){lastContactDate=d;}
    public LocalDate getNextContactDate(){return nextContactDate;} public void setNextContactDate(LocalDate d){nextContactDate=d;}
    public String getNotes(){return notes;} public void setNotes(String n){notes=n;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){updatedAt=t;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){createdAt=t;}
    public String getContactMethod(){return contactMethod;} public void setContactMethod(String c){contactMethod=c;}
}
