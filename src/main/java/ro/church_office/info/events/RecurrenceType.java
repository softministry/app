package ro.church_office.info.events;

public enum RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY;
    public static RecurrenceType from(String value) {
        if (value == null || value.isBlank()) return NONE;
        try { return RecurrenceType.valueOf(value.toUpperCase()); } catch (Exception ex) { return NONE; }
    }
}
