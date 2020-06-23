package ru.majordomo.hms.rc.user.cleaner;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
public class Cleaner {
    @Nonnull
    public String cleanString(Object input) {
        if (input == null) {
            return "";
        } else if (input instanceof String) {
            return ((String) input).trim();
        } else {
            return input.toString();
        }
    }

    @Nonnull
    public Map<String, String> cleanMapWithStrings(Object stringMap) {
        Map<String, String> clearedMap = new HashMap<>();
        if (stringMap instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) stringMap;
            map.forEach((key, value) -> {
                clearedMap.put(cleanString(key), cleanString(value));
            });
        }
        return clearedMap;
    }

    @Nonnull
    public List<String> cleanListWithStrings(Object stringList) {
        List<String> cleanedStringList = new ArrayList<>();
        if (stringList instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) stringList;
            for (Object object : list) {
                String element = cleanString(object);
                if (!element.equals("")) {
                    cleanedStringList.add(element);
                }
            }
        }
        return cleanedStringList;
    }

    @Nullable
    public Boolean cleanBoolean(Object booleanObject) {
        if (booleanObject == null) {
            return null;
        } else if (booleanObject instanceof String ) {
            if (((String) booleanObject).equalsIgnoreCase("true") || ((String) booleanObject).equalsIgnoreCase("false")) {
                return Boolean.valueOf((String) booleanObject);
            } else {
                return null;
            }
        } else {
            return (Boolean) booleanObject;
        }
    }

    @Nullable
    public Integer cleanInteger(Object integerObject) {
        if (integerObject == null) {
            return null;
        } else if (integerObject instanceof String) {
            try {
                return Integer.parseInt((String) integerObject);
            } catch (NumberFormatException e) {
                throw new ParameterValidationException("Один из параметров не является числом");
            }
        } else {
            return (Integer) integerObject;
        }
    }
}
