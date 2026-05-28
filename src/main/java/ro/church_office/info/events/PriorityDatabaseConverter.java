package ro.church_office.info.events;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PriorityDatabaseConverter implements AttributeConverter<Priority, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Priority attribute) {
        Priority value = attribute == null ? Priority.MEDIUM : attribute;
        // Persist as 1..5 to stay compatible with production snapshots.
        return value.getValue();
    }

    @Override
    public Priority convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return Priority.MEDIUM;
        }
        // Backward-compatible reads:
        // - legacy 0-based ordinal rows: 0..4
        // - production rows observed as 1..5
        if (dbData >= 1 && dbData <= Priority.values().length) {
            return Priority.fromValue(dbData);
        }
        if (dbData >= 0 && dbData < Priority.values().length) {
            return Priority.values()[dbData];
        }
        return Priority.MEDIUM;
    }
}
