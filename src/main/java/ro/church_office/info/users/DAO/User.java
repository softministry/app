package ro.church_office.info.users.DAO;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String password;
    private String role;
    private String email;
    private String securityAnswerOneHash;
    private String securityAnswerTwoHash;
    private LocalDateTime lastLogin;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getUsername(){return username;} public void setUsername(String u){username=u;}
    public String getPassword(){return password;} public void setPassword(String p){password=p;}
    public String getPasswordHash(){return password;} public void setPasswordHash(String p){password=p;}
    public String getRole(){return role;} public void setRole(String r){role=r;}
    public String getEmail(){return email;} public void setEmail(String e){email=e;}
    public String getSecurityAnswerOneHash(){return securityAnswerOneHash;} public void setSecurityAnswerOneHash(String h){securityAnswerOneHash=h;}
    public String getSecurityAnswerTwoHash(){return securityAnswerTwoHash;} public void setSecurityAnswerTwoHash(String h){securityAnswerTwoHash=h;}
    public LocalDateTime getLastLogin(){return lastLogin;} public void setLastLogin(LocalDateTime t){lastLogin=t;}
}
