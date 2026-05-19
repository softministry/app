package ro.church_office.info.followup;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PastoralRecommendationDismissal {
    private Long id; private Long churchId; private Long personId; private String recommendationType;
    private String recommendationKey; private LocalDate dismissedUntil; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public Long getPersonId(){return personId;} public void setPersonId(Long p){personId=p;}
    public String getRecommendationType(){return recommendationType;} public void setRecommendationType(String r){recommendationType=r;}
    public String getRecommendationKey(){return recommendationKey;} public void setRecommendationKey(String k){recommendationKey=k;}
    public LocalDate getDismissedUntil(){return dismissedUntil;} public void setDismissedUntil(LocalDate d){dismissedUntil=d;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){updatedAt=t;}
}
