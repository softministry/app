package ro.church_office.info.person.DAO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MemberTypeConverterTest {

    private final MemberTypeConverter converter = new MemberTypeConverter();

    @Test
    void storesFriendWithLegacyDatabaseToken() {
        assertEquals("FREND", converter.convertToDatabaseColumn(MemberType.FRIEND));
    }

    @Test
    void readsLegacyFriendTokenAsFriend() {
        assertEquals(MemberType.FRIEND, converter.convertToEntityAttribute("FREND"));
        assertEquals(MemberType.FRIEND, converter.convertToEntityAttribute("FRIEND"));
    }

    @Test
    void preservesOtherMemberTypesAndNulls() {
        assertEquals("MEMBER", converter.convertToDatabaseColumn(MemberType.MEMBER));
        assertEquals(MemberType.CHILD, converter.convertToEntityAttribute("CHILD"));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
