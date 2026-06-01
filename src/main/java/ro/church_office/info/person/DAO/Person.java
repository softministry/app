package ro.church_office.info.person.DAO;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; private Long churchId; private String firstName; private String lastName; private String email; private MemberType memberType = MemberType.MEMBER; private LocalDate birthDate;
    private String phone; private String churchRole; private String address; private String position;
    @Transient
    private Person spouse;
    @Transient
    private Set<Person> children = new LinkedHashSet<>();
    @Transient
    private Set<Person> parents = new LinkedHashSet<>();
    public MemberType getMemberType(){return memberType;} public void setMemberType(MemberType t){memberType=t;}
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public String getFirstName(){return firstName;} public void setFirstName(String s){firstName=s;} public String getLastName(){return lastName;} public void setLastName(String s){lastName=s;}
    public String getEmail(){return email;} public void setEmail(String s){email=s;}
    public LocalDate getBirthDate(){return birthDate;} public void setBirthDate(LocalDate d){birthDate=d;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getChurchRole(){return churchRole;} public void setChurchRole(String v){churchRole=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public Person getSpouse(){return spouse;} public void setSpouse(Person s){spouse=s;}
    public Set<Person> getChildren(){return children;} public void setChildren(Set<Person> c){children=c==null?new LinkedHashSet<>():c;}
    public Set<Person> getParents(){return parents;} public void setParents(Set<Person> p){parents=p==null?new LinkedHashSet<>():p;}
}
