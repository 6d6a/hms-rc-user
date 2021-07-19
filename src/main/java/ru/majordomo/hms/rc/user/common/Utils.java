package ru.majordomo.hms.rc.user.common;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;

import java.math.BigDecimal;
import java.net.IDN;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class Utils {

    public static Long getLongFromUnexpectedInput(Object input){
        Long longValue;
        if (input instanceof Integer) {
            longValue = Long.valueOf((Integer) input);
        } else if (input instanceof String) {
            longValue = Long.valueOf((String) input);
        } else {
            try {
                longValue = (Long) input;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ClassCastException("Ошибка при попытке получить Long из входных данных");
            }
        }

        return longValue;
    }

    public static boolean mapContains(Map<String, String> map, @NotEmpty String... keys) throws ParameterValidationException {
        if (keys.length == 0) {
            throw new ParameterValidationException("Необходимо передать список ключей для проверки");
        }
        for (String key : keys) {
            if (map.get(key) == null || map.get(key).isEmpty()){
                return false;
            }
        }
        return true;
    }

    public static BigDecimal getBigDecimalFromUnexpectedInput(Object input) throws ParameterValidationException {
        if (input == null) {
            return null;
        }
        BigDecimal bigDecimal;

        if (input instanceof Integer) {
            bigDecimal = BigDecimal.valueOf((Integer) input);
        } else if (input instanceof Long) {
            bigDecimal = BigDecimal.valueOf((Long) input);
        } else if (input instanceof Double) {
            bigDecimal = BigDecimal.valueOf((Double) input);
        } else if (input instanceof String) {
            bigDecimal = new BigDecimal((String) input);
        } else {
            try {
                bigDecimal = (BigDecimal) input;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при попытке получить BigDecimal из входных данных");
            }
        }

        return bigDecimal;
    }

    private final static Pattern CIDR_OR_IP_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/([1-9]|[1-2]\\d|3[0-2]))?$");
    private final static Pattern CIDR_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])/([1-9]|[1-2]\\d|3[0-2])$");

    public static boolean cidrOrIpValid(String cidrOrIp) {
        return CIDR_OR_IP_PATTERN.matcher(cidrOrIp).matches();
    }

    public static boolean cidrValid(String cidr) {
        return CIDR_PATTERN.matcher(cidr).matches();
    }

    public static ServiceMessage makeSuccessResponse(@Nullable String operationIdentity, @Nullable String actionIdentity, @Nullable String objRef, @Nullable String accountId) {
        ServiceMessage response = new ServiceMessage();
        response.setAccountId(accountId);
        response.setObjRef(objRef);
        response.setActionIdentity(actionIdentity);
        response.setOperationIdentity(operationIdentity);
        response.addParam(Constants.SUCCESS_KEY, true);
        return response;
    }

    public static ServiceMessage makeSuccessResponse() {
        return makeSuccessResponse(null, null, null, null);
    }

    private final static int MAX_DOMAIN_LENGTH = 253;
    private final static Pattern TLD_PATTERN = Pattern.compile("^[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?$", Pattern.CASE_INSENSITIVE);
    private final static Pattern DOMAIN_PART_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$", Pattern.CASE_INSENSITIVE);
    private final static Pattern SUBDOMAIN_PART_PATTERN = Pattern.compile("^[_a-z0-9-]{0,63}$", Pattern.CASE_INSENSITIVE);

    /** Валидация домена допускающая любой TLD и символ _, по возможности лучше использовать {@link org.apache.commons.validator.routines.DomainValidator} */
    public static boolean domainValidWithNonExistentTld(@Nullable String domainUnicode) {
        return domainValidWithNonExistentTld(domainUnicode, false, false);
    }

    /** Валидация домена допускающая любой TLD и символ _, по возможности лучше использовать {@link org.apache.commons.validator.routines.DomainValidator} */
    public static boolean domainValidWithNonExistentTld(@Nullable String domainUnicode, boolean denyTldOnly, boolean denyLastDot) {
        if (StringUtils.isEmpty(domainUnicode)) return false;
        String domainPunycode;
        try {
            domainPunycode = IDN.toASCII(domainUnicode);
        } catch (IllegalArgumentException ignore) {
            // IllegalArgumentException("The label in the input is too long") если часть домена больше 63 символов и подобные ошибки
            return false;
        }
        if (domainPunycode.endsWith(".")) {
            if (denyLastDot) {
                return false;
            } else {
                domainPunycode = domainPunycode.substring(0, domainPunycode.length() - 1);
            }
        }
        if (domainPunycode.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        String[] domainParts = domainPunycode.split("\\.");
        if (domainParts.length == 0 || (domainParts.length == 1 && denyTldOnly)) {
            return false;
        }
        String tld = domainParts[domainParts.length - 1];
        if (StringUtils.isEmpty(tld) || !TLD_PATTERN.matcher(tld).matches()) {
            return false;
        }
        if (domainParts.length > 1) {
            String domainPart = domainParts[domainParts.length - 2];
            if (StringUtils.isEmpty(domainPart) || !DOMAIN_PART_PATTERN.matcher(domainPart).matches()) {
                return false;
            }
        }
        for (int i = 0; i < domainParts.length - 2; i++) {
            if (StringUtils.isEmpty(domainParts[i]) || !SUBDOMAIN_PART_PATTERN.matcher(domainParts[i]).matches()) {
                return false;
            }
        }
        return true;
    }

    /** валидатор умеющий .local, .intr, .bit и некоторые другие зоны */
    public static final DomainValidator domainValidatorLocalPlus = DomainValidator.getInstance(
            true,
            Collections.singletonList(new DomainValidator.Item(DomainValidator.ArrayType.LOCAL_PLUS,
                    new String[]{"local", "intr", "onion", "i2p", "bit"})));

}
