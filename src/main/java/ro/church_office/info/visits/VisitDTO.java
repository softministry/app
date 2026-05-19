package ro.church_office.info.visits;

import ro.church_office.info.visits.dao.Visit;

import java.time.LocalDate;

public class VisitDTO {
    private Long id; private String personName; private String address; private String phone; private LocalDate visitDate; private String notes;
    public VisitDTO() {}
    public VisitDTO(Visit visit) { if(visit!=null){ this.id = visit.getId(); this.personName=visit.getPersonName(); this.address=visit.getAddress(); this.phone=visit.getPhone(); this.visitDate=visit.getVisitDate(); this.notes=visit.getNotes(); } }
    public Visit toEntity(){ Visit v = new Visit(); v.setId(id); v.setPersonName(personName); v.setAddress(address); v.setPhone(phone); v.setVisitDate(visitDate); v.setNotes(notes); return v; }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getPersonName(){return personName;} public void setPersonName(String v){personName=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public LocalDate getVisitDate(){return visitDate;} public void setVisitDate(LocalDate v){visitDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
