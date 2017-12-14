package ru.majordomo.hms.rc.user.resources.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.validation.ValidDnsRecord;

public class DnsRecordValidator implements ConstraintValidator<ValidDnsRecord, DNSResourceRecord> {
    @Override
    public void initialize(ValidDnsRecord validDnsRecord) {
    }

    @Override
    public boolean isValid(final DNSResourceRecord record, ConstraintValidatorContext constraintValidatorContext) {
        return record.getOwnerName() == null || record.getName() == null || record.getOwnerName().endsWith(record.getName());
    }
}
