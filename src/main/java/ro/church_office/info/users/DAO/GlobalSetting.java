package ro.church_office.info.users.DAO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "global_setting")
public class GlobalSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true)
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    public GlobalSetting() {}
    public GlobalSetting(String key, String value){ this.settingKey = key; this.settingValue = value; }
    public GlobalSetting(String key, int value){ this.settingKey = key; this.settingValue = String.valueOf(value); }

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getSettingKey(){return settingKey;} public void setSettingKey(String k){settingKey=k;}
    public String getKey(){return settingKey;} public void setKey(String k){settingKey=k;}
    public String getSettingValue(){return settingValue;} public void setSettingValue(String v){settingValue=v;}
    public String getStringValue(){return settingValue;}
    public void setStringValue(String value){ this.settingValue = value; }
    public Integer getIntValue(){ try { return settingValue == null ? null : Integer.valueOf(settingValue.trim()); } catch (Exception ex){ return null; } }
    public void setIntValue(int value){ this.settingValue = String.valueOf(value); }
}
