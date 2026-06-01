package ro.church_office.info.church.DAO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "church_info")
public class ChurchInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "avatar_url")
    private String avatarUrl;
    private String address;
    @Column(name = "pastor_name")
    private String pastorName;
    @Column(name = "pastor_phone")
    private String pastorPhone;
    @Column(name = "secretary_name")
    private String secretaryName;
    @Column(name = "secretary_phone")
    private String secretaryPhone;
    @Column(name = "treasurer_name")
    private String treasurerName;
    @Column(name = "treasurer_phone")
    private String treasurerPhone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPastorName() { return pastorName; }
    public void setPastorName(String pastorName) { this.pastorName = pastorName; }
    public String getPastorPhone() { return pastorPhone; }
    public void setPastorPhone(String pastorPhone) { this.pastorPhone = pastorPhone; }
    public String getSecretaryName() { return secretaryName; }
    public void setSecretaryName(String secretaryName) { this.secretaryName = secretaryName; }
    public String getSecretaryPhone() { return secretaryPhone; }
    public void setSecretaryPhone(String secretaryPhone) { this.secretaryPhone = secretaryPhone; }
    public String getTreasurerName() { return treasurerName; }
    public void setTreasurerName(String treasurerName) { this.treasurerName = treasurerName; }
    public String getTreasurerPhone() { return treasurerPhone; }
    public void setTreasurerPhone(String treasurerPhone) { this.treasurerPhone = treasurerPhone; }
}
