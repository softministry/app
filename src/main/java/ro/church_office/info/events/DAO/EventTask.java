package ro.church_office.info.events.DAO;

import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;

public class EventTask {
    private Long id; private Event event; private String title; private String notes; private LocalDate dueDate; private String status; private Integer orderIndex; private Person assignedTo;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Event getEvent(){return event;} public void setEvent(Event e){event=e;}
    public String getTitle(){return title;} public void setTitle(String t){title=t;}
    public String getNotes(){return notes;} public void setNotes(String n){notes=n;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate d){dueDate=d;}
    public String getStatus(){return status;} public void setStatus(String s){status=s;}
    public Integer getOrderIndex(){return orderIndex;} public void setOrderIndex(Integer i){orderIndex=i;}
    public Person getAssignedTo(){return assignedTo;} public void setAssignedTo(Person p){assignedTo=p;}
}
