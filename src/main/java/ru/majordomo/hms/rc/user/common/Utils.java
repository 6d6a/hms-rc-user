package ru.majordomo.hms.rc.user.common;

import javax.validation.constraints.NotEmpty;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.math.BigDecimal;
import java.util.Map;

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
}
