package ro.church_office.info.events;

public class EventMigrationDTO {
    private Long targetGroupId;
    private Long targetChurchId;
    private Boolean clearImplementedBy;
    public Long getTargetGroupId(){return targetGroupId;} public void setTargetGroupId(Long v){targetGroupId=v;}
    public Long getTargetChurchId(){return targetChurchId;} public void setTargetChurchId(Long v){targetChurchId=v;}
    public Boolean getClearImplementedBy(){return clearImplementedBy;} public void setClearImplementedBy(Boolean v){clearImplementedBy=v;}
}
