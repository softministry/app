package ro.church_office.info.events;

public enum Priority {
    LOW(1), MEDIUM(2), HIGH(3);
    private final int value;
    Priority(int value) { this.value = value; }
    public int getValue() { return value; }
    public static Priority from(String value) {
        if (value == null) return MEDIUM;
        for (Priority p : values()) if (p.name().equalsIgnoreCase(value)) return p;
        return MEDIUM;
    }
}
