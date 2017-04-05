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
        return input.trim()
                .replace("\\","")
                .replace("<","")
                .replace(">","")
                .replace("?","")
                .replace("#","")
                .replace("~","");
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
}
