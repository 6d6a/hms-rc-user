package ru.majordomo.hms.rc.user.mappers;

import org.springframework.core.convert.converter.Converter;
import ru.majordomo.hms.rc.user.resources.Address;

public class StringToAddressConverter implements Converter<String,Address> {
    @Override
    public Address convert(String source) {
        return source == null || source.isEmpty() ? null : new Address(source);
    }
}
