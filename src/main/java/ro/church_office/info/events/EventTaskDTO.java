package ro.church_office.info.events;

import ro.church_office.info.events.DAO.EventTask;

public class EventTaskDTO {
    private Long id; private String title;
    public static EventTaskDTO fromEntity(EventTask task){ EventTaskDTO dto = new EventTaskDTO(); if(task!=null){dto.id=task.getId(); dto.title=task.getTitle();} return dto; }
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public String getTitle(){return title;} public void setTitle(String t){this.title=t;}
}
