package ro.church_office.info.users.DAO;

public class GlobalSetting {
    private Long id;
    private String settingKey;
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
