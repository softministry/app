package ro.church_office.info.visits.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

@Entity
@Table(name = "visit")
public class Visit { private Long id; private Long churchId;
@Transient
private Person person; private LocalDate visitDate; private String notes; private String phone; private String personName; private String address;
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
@Transient
public Person getPerson(){return person;} public void setPerson(Person p){person=p;} public LocalDate getVisitDate(){return visitDate;} public void setVisitDate(LocalDate d){visitDate=d;}
public String getNotes(){return notes;} public void setNotes(String n){notes=n;}
public String getPhone(){return phone;} public void setPhone(String p){phone=p;} public String getPersonName(){return personName;} public void setPersonName(String p){personName=p;}
public String getAddress(){return address;} public void setAddress(String a){address=a;} }
