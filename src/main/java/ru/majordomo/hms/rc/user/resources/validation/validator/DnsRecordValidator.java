package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import ru.majordomo.hms.rc.user.common.Utils;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;
import ru.majordomo.hms.rc.user.resources.validation.ValidDnsRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;
import java.util.regex.Pattern;

public class DnsRecordValidator implements ConstraintValidator<ValidDnsRecord, DNSResourceRecord> {
    private final static Pattern ONLY_ENG_DNS = Pattern.compile("^[_a-zA-Z0-9-]+$");
    private final static List<String> ALLOWED_NAME_SERVERS = Collections.unmodifiableList(Arrays.asList(
            "ns.majordomo.ru", "ns2.majordomo.ru", "ns3.majordomo.ru", "ns4.majordomo.ru"
    )); // todo вынести список ns majordomo в config из всех мест в rc-user

    @Override
    public void initialize(ValidDnsRecord validDnsRecord) {
    }

    @Override
    public boolean isValid(
            @Nullable final DNSResourceRecord record,
            @Nonnull ConstraintValidatorContext constraintValidatorContext
    ) {
        if (record == null) {
            return false;
        }
        if (record.getOwnerName() == null || !record.getOwnerName().endsWith(record.getName())) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Запись должна кончаться именем домена").addConstraintViolation();
            return false;
        }

        if (record.getName() == null) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Доменное имя не может быть пустым").addConstraintViolation();
            return false;
        }

        DNSResourceRecordType type = record.getRrType();
        if (type != null) {

            if (record.getData() == null || record.getData().isEmpty()) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("Значение DNS-записи не должно быть пустым").addConstraintViolation();
                return false;
            } else if (record.getData().length() > 512) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("Значение DNS-записи слишком длинное").addConstraintViolation();
                return false;
            }

            switch (type) {
                case A:
                    if (!InetAddressValidator.getInstance().isValidInet4Address(record.getData())) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный формат IP-адреса").addConstraintViolation();
                        return false;
                    }
                    break;
                case AAAA:
                    if (!InetAddressValidator.getInstance().isValidInet6Address(record.getData())) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный формат IPv6-адреса").addConstraintViolation();
                        return false;
                    }
                    break;
                case CNAME:
                case MX:
                    if (!Utils.domainValidWithNonExistentTld(record.getData())) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный формат доменного имени").addConstraintViolation();
                        return false;
                    }
                    break;
                case SRV:
                    String fullRecordSRV = record.getData();
                    String[] partsRecordSRV = fullRecordSRV.trim().split("\\s+");
                    if (partsRecordSRV.length != 3
                            || !NumberUtils.isNumber(partsRecordSRV[0])
                            || Integer.parseInt(partsRecordSRV[0]) < 0 || Integer.parseInt(partsRecordSRV[0]) > 65535
                            || !NumberUtils.isNumber(partsRecordSRV[1])
                            || Integer.parseInt(partsRecordSRV[1]) < 0 || Integer.parseInt(partsRecordSRV[1]) > 65535
                            || !Utils.domainValidWithNonExistentTld(partsRecordSRV[2])) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный формат SRV DNS-записи").addConstraintViolation();
                        return false;
                    }
                    break;
                case TXT:
                    break;
                case CAA:
                    String fullRecordCAA = record.getData();
                    String[] partsRecordCAA = fullRecordCAA.trim().split("\\s+");
                    if (partsRecordCAA.length != 3
                            || !NumberUtils.isNumber(partsRecordCAA[0])
                            || Integer.parseInt(partsRecordCAA[0]) > 255 || Integer.parseInt(partsRecordCAA[0]) < 0
                            || !java.util.Arrays.asList("issue", "issuewild", "iodef").contains(partsRecordCAA[1])
                            || !DomainValidator.getInstance().isValid(partsRecordCAA[2].replace("\"", ""))) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный формат CAA DNS-записи").addConstraintViolation();
                        return false;
                    }
                    break;
                case NS:
                    String subdomainPart = record.getOwnerName().substring(0, Math.max(record.getOwnerName().length() - record.getName().length(), 0));
                    if (subdomainPart.contains("*")) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext.buildConstraintViolationWithTemplate("Запрещено создавать NS DNS-записи с '*'")
                                .addConstraintViolation();
                        return false;
                    } else if (subdomainPart.isEmpty()) {
                        if (!ALLOWED_NAME_SERVERS.contains(record.getData())) {
                            constraintValidatorContext.disableDefaultConstraintViolation();
                            constraintValidatorContext.buildConstraintViolationWithTemplate("Запрещено редактировать NS DNS-записи для основного домена")
                                    .addConstraintViolation();
                            return false;
                        }
                    } else {
                        if (!DomainValidator.getInstance().isValid(record.getData()) && !ONLY_ENG_DNS.matcher(record.getData()).matches()) {
                            constraintValidatorContext.disableDefaultConstraintViolation();
                            constraintValidatorContext.buildConstraintViolationWithTemplate("Значением NS DNS-записи должен быть корректный домен")
                                    .addConstraintViolation();
                            return false;
                        }
                    }
                    break;
                default:
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext.buildConstraintViolationWithTemplate("Неверный тип DNS-записи").addConstraintViolation();
                    return false;
            }
        } else {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Тип DNS-записи должен быть указан").addConstraintViolation();
            return false;
        }

        return true;
    }
}
