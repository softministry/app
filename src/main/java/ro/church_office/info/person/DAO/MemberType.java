package ro.church_office.info.person.DAO;
public enum MemberType {
    MEMBER,
    CHILD,
    FRIEND;

    public static MemberType fromExternalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("FREND".equals(normalized)) {
            return FRIEND;
        }
        return MemberType.valueOf(normalized);
    }

    public String toDatabaseValue() {
        return this == FRIEND ? "FREND" : name();
    }
}
