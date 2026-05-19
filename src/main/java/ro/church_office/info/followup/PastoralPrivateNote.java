package ro.church_office.info.followup;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDateTime;

@Entity
@Table(name = "pastoral_private_note")
public class PastoralPrivateNote {
    private Long id; private Long churchId;
    @Transient
    private Person person;
    private String noteText; private String allowedRoles; private String createdByUsername; private String createdByRole;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getNote(){return noteText;} public void setNote(String n){noteText=n;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    @Transient
    public Person getPerson(){return person;} public void setPerson(Person p){person=p;}
    public String getNoteText(){return noteText;} public void setNoteText(String v){noteText=v;}
    public String getAllowedRoles(){return allowedRoles;} public void setAllowedRoles(String v){allowedRoles=v;}
    public String getCreatedByUsername(){return createdByUsername;} public void setCreatedByUsername(String v){createdByUsername=v;}
    public String getCreatedByRole(){return createdByRole;} public void setCreatedByRole(String v){createdByRole=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
