package ro.church_office.info.person.DAO;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MemberTypeConverter implements AttributeConverter<MemberType, String> {

    @Override
    public String convertToDatabaseColumn(MemberType attribute) {
        return attribute == null ? null : attribute.toDatabaseValue();
    }

    @Override
    public MemberType convertToEntityAttribute(String dbData) {
        return MemberType.fromExternalValue(dbData);
    }
}
