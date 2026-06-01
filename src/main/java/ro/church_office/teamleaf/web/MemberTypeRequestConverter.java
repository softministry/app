package ro.church_office.teamleaf.web;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import ro.church_office.info.person.DAO.MemberType;

@Component
public class MemberTypeRequestConverter implements Converter<String, MemberType> {

    @Override
    public MemberType convert(String source) {
        return MemberType.fromExternalValue(source);
    }
}
