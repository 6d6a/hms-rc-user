package ru.majordomo.hms.rc.user.common;

import org.hibernate.validator.constraints.NotEmpty;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

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
}
