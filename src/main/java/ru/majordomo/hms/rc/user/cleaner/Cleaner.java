package ru.majordomo.hms.rc.user.cleaner;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Cleaner {
    public String cleanString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    public List<String> cleanListWithStrings(List<String> stringList) {
        List<String> cleanedStringList = new ArrayList<>();
        if (stringList == null) {
            return cleanedStringList;
        }
        for (String aStringList : stringList) {
            String element = cleanString(aStringList);
            if (!element.equals("")) {
                cleanedStringList.add(element);
            }
        }
        return cleanedStringList;
    }

    public Boolean cleanBoolean(Object booleanObject) {
        if (booleanObject == null) {
            return null;
        } else if (booleanObject instanceof String) {
            return Boolean.valueOf((String) booleanObject);
        } else {
            return (Boolean) booleanObject;
        }
    }

    public Integer cleanInteger(Object integerObject) {
        if (integerObject == null) {
            return null;
        } else if (integerObject instanceof String) {
            return Integer.parseInt((String) integerObject);
        } else {
            return (Integer) integerObject;
        }
    }
}
