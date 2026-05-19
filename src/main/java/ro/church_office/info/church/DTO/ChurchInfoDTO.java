package ro.church_office.info.church.DTO;

public class ChurchInfoDTO {
    public Long id;
    public String name;
    public String avatarUrl;
    public String address;
    public String pastorName;
    public String pastorPhone;
    public String secretaryName;
    public String secretaryPhone;
    public String treasurerName;
    public String treasurerPhone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
