package ro.church_office.info.groups;

import ro.church_office.info.person.DAO.Person;

import java.util.LinkedHashSet;
import java.util.Set;

public class ChurchGroup {
    private Long id; private Long churchId; private String name; private GroupType type = GroupType.SMALL_GROUP;
    private String description; private Person leader; private Set<Person> members = new LinkedHashSet<>();
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public String getName(){return name;} public void setName(String n){name=n;} public GroupType getType(){return type;} public void setType(GroupType t){type=t;}
    public String getDescription(){return description;} public void setDescription(String d){description=d;}
    public Person getLeader(){return leader;} public void setLeader(Person p){leader=p;}
    public Set<Person> getMembers(){return members;} public void setMembers(Set<Person> m){members=m;}
}
