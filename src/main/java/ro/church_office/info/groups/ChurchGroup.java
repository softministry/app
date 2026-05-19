package ro.church_office.info.groups;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ro.church_office.info.person.DAO.Person;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "church_group")
public class ChurchGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; private Long churchId; private String name; private GroupType type = GroupType.SMALL_GROUP;
    private String description;
    @ManyToOne
    @JoinColumn(name = "leader_id")
    private Person leader;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "church_group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> members = new LinkedHashSet<>();
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public String getName(){return name;} public void setName(String n){name=n;}
    @Enumerated(EnumType.STRING)
    public GroupType getType(){return type;} public void setType(GroupType t){type=t;}
    public String getDescription(){return description;} public void setDescription(String d){description=d;}
    public Person getLeader(){return leader;} public void setLeader(Person p){leader=p;}
    public Set<Person> getMembers(){return members;} public void setMembers(Set<Person> m){members=m;}
}
