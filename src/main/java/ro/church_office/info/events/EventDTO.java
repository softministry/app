package ro.church_office.info.events;

import ro.church_office.info.events.DAO.Event;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.person.DAO.Person;
import ro.church_office.info.person.DTO.PersonDTO;

import java.time.LocalDate;

public class EventDTO {
    private Long id; private Long churchId; private String title; private String eventName; private String about; private String status; private String eventType; private String priority; private String recurrenceType;
    private Integer recurrenceInterval; private LocalDate recurrenceUntil;
    private LocalDate openDate; private LocalDate endDate; private Long groupId; private String groupName; private String groupType; private PersonDTO implementedBy;
    private Boolean frontReminderEnabled; private Integer frontReminderDaysBefore;

    public static EventDTO fromEntity(Event e){ EventDTO dto = new EventDTO(); if(e!=null){dto.id=e.getId(); dto.churchId=e.getChurchId(); dto.eventName=e.getEventName(); dto.about=e.getAbout(); dto.status=e.getStatus(); dto.eventType=e.getEventType()==null?null:e.getEventType().name(); dto.priority=e.getPriority()==null?null:e.getPriority().name(); dto.recurrenceType=e.getRecurrenceType()==null?null:e.getRecurrenceType().name(); dto.openDate=e.getOpenDate(); dto.endDate=e.getEndDate(); dto.groupId=e.getAssociatedGroup()==null?null:e.getAssociatedGroup().getId(); if(e.getAssociatedGroup()!=null){dto.groupName=e.getAssociatedGroup().getName(); dto.groupType=e.getAssociatedGroup().getType()==null?null:e.getAssociatedGroup().getType().name();} if(e.getImplementedBy()!=null){dto.implementedBy=PersonDTO.fromEntity(e.getImplementedBy());}} return dto; }
    public Event toEntity(){ Event e = new Event(); e.setId(id); e.setChurchId(churchId); e.setEventName(getEventName()); e.setAbout(about); e.setStatus(status); e.setEventType(parseEventType(eventType)); e.setPriority(Priority.from(priority)); e.setRecurrenceType(RecurrenceType.from(recurrenceType)); e.setOpenDate(openDate); e.setEndDate(endDate); if(groupId!=null){ ChurchGroup g = new ChurchGroup(); g.setId(groupId); e.setAssociatedGroup(g);} if(implementedBy!=null&&implementedBy.getId()!=null){ Person p = new Person(); p.setId(implementedBy.getId()); e.setImplementedBy(p);} return e; }

    private EventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return EventType.OTHER;
        }
        try {
            return EventType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            return EventType.OTHER;
        }
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getChurchId(){return churchId;} public void setChurchId(Long v){churchId=v;}
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getEventName() { return eventName == null ? title : eventName; } public void setEventName(String eventName) { this.eventName = eventName; this.title = eventName; }
    public String getAbout() { return about; } public void setAbout(String about) { this.about = about; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getEventType() { return eventType; } public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPriority() { return priority; } public void setPriority(String priority) { this.priority = priority; }
    public String getRecurrenceType() { return recurrenceType; } public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }
    public Integer getRecurrenceInterval() { return recurrenceInterval; } public void setRecurrenceInterval(Integer recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }
    public LocalDate getRecurrenceUntil() { return recurrenceUntil; } public void setRecurrenceUntil(LocalDate recurrenceUntil) { this.recurrenceUntil = recurrenceUntil; }
    public LocalDate getOpenDate() { return openDate; } public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Long getGroupId() { return groupId; } public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName(){return groupName;} public void setGroupName(String v){groupName=v;}
    public String getGroupType(){return groupType;} public void setGroupType(String v){groupType=v;}
    public PersonDTO getImplementedBy() { return implementedBy; } public void setImplementedBy(PersonDTO implementedBy) { this.implementedBy = implementedBy; }
    public Boolean getFrontReminderEnabled(){return frontReminderEnabled;} public void setFrontReminderEnabled(Boolean v){frontReminderEnabled=v;}
    public Integer getFrontReminderDaysBefore(){return frontReminderDaysBefore;} public void setFrontReminderDaysBefore(Integer v){frontReminderDaysBefore=v;}
}
