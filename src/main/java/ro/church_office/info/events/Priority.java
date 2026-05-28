package ro.church_office.info.events;

public enum Priority {
    LOW,
    MEDIUM_LOW,
    MEDIUM,
    HIGH_MEDIUM,
    HIGH;
    
    public int getValue() { 
        return ordinal() + 1;
    }
    
    public static Priority from(String value) {
        if (value == null) return MEDIUM;
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(value)) return p;
        }
        return MEDIUM;
    }
    
    public static Priority fromValue(int value) {
        if (value < 1 || value > values().length) return MEDIUM;
        return values()[value - 1];
    }
}
