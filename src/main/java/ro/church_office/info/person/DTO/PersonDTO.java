package ro.church_office.info.person.DTO;

import ro.church_office.info.person.DAO.MemberType;
import ro.church_office.info.person.DAO.Person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PersonDTO {
    private Long id; private String firstName; private String lastName; private MemberType memberType = MemberType.MEMBER;
    private Long spouseId; private List<Long> parentIds = new ArrayList<>(); private List<Long> childrenIds = new ArrayList<>();
    private String phone; private String churchRole; private String address; private String position; private LocalDate birthDate;
    public static PersonDTO fromEntity(Person person){
        PersonDTO dto = new PersonDTO();
        if (person == null) return dto;
        dto.setId(person.getId()); dto.setFirstName(person.getFirstName()); dto.setLastName(person.getLastName()); dto.setMemberType(person.getMemberType());
        dto.setPhone(person.getPhone()); dto.setChurchRole(person.getChurchRole()); dto.setAddress(person.getAddress()); dto.setPosition(person.getPosition()); dto.setBirthDate(person.getBirthDate());
        if(person.getSpouse()!=null) dto.setSpouseId(person.getSpouse().getId());
        return dto;
    }
    public String getFullName(){ return ((firstName==null?"":firstName)+" "+(lastName==null?"":lastName)).trim(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public String getFirstName(){return firstName;} public void setFirstName(String s){firstName=s;}
    public String getLastName(){return lastName;} public void setLastName(String s){lastName=s;} public MemberType getMemberType(){return memberType;} public void setMemberType(MemberType t){memberType=t;}
    public Long getSpouseId(){return spouseId;} public void setSpouseId(Long v){spouseId=v;}
    public List<Long> getParentIds(){return parentIds;} public void setParentIds(List<Long> v){parentIds=v==null?new ArrayList<>():v;}
    public List<Long> getChildrenIds(){return childrenIds;} public void setChildrenIds(List<Long> v){childrenIds=v==null?new ArrayList<>():v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getChurchRole(){return churchRole;} public void setChurchRole(String v){churchRole=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public LocalDate getBirthDate(){return birthDate;} public void setBirthDate(LocalDate v){birthDate=v;}
}
