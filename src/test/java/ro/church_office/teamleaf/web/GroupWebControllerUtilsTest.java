package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;
import ro.church_office.info.groups.ChurchGroup;
import ro.church_office.info.groups.GroupType;
import ro.church_office.info.person.DAO.Person;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupWebControllerUtilsTest {

    @Test
    void displayNameHandlesNullAndEmptyNames() {
        assertEquals("—", GroupWebController.displayName(null));

        Person empty = new Person();
        assertEquals("Persoană fără nume", GroupWebController.displayName(empty));

        Person person = new Person();
        person.setFirstName("Ana");
        person.setLastName("Pop");
        assertEquals("Ana Pop", GroupWebController.displayName(person));
    }

    @Test
    void groupEditDataAndFormAreBuiltFromEntity() {
        Person leader = new Person();
        leader.setId(11L);
        leader.setFirstName("Lead");

        Person m1 = new Person();
        m1.setId(21L);
        Person m2 = new Person();
        m2.setId(22L);

        ChurchGroup group = new ChurchGroup();
        group.setId(5L);
        group.setName("Tineri");
        group.setType(GroupType.MINISTRY);
        group.setLeader(leader);
        group.setDescription("Descriere");
        group.setMembers(new LinkedHashSet<>(List.of(m1, m2)));

        GroupWebController.GroupEditData editData = GroupWebController.GroupEditData.from(group);
        assertEquals(5L, editData.id());
        assertEquals("Tineri", editData.name());
        assertEquals("MINISTRY", editData.type());
        assertEquals(11L, editData.leaderId());
        assertEquals("Descriere", editData.description());
        assertEquals("21,22", editData.memberIdsCsv());

        GroupWebController.GroupForm form = GroupWebController.GroupForm.from(group);
        assertEquals(5L, form.getId());
        assertEquals("Tineri", form.getName());
        assertEquals(GroupType.MINISTRY, form.getType());
        assertEquals(11L, form.getLeaderId());
        assertEquals("Descriere", form.getDescription());
        assertEquals(List.of(21L, 22L), form.getMemberIds());
    }
}
