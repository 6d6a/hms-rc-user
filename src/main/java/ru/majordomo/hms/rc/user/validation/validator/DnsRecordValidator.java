package ru.majordomo.hms.rc.user.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.validation.ValidDnsRecord;

public class DnsRecordValidator implements ConstraintValidator<ValidDnsRecord, DNSResourceRecord> {
    @Override
    public void initialize(ValidDnsRecord validDnsRecord) {
    }

    @Override
    public boolean isValid(final DNSResourceRecord record, ConstraintValidatorContext constraintValidatorContext) {
        return record.getOwnerName().endsWith(record.getName());
    }
}
