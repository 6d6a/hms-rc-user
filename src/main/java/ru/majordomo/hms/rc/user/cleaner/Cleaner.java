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
        if (stringList == null) {
            return new ArrayList<>();
        }
        for (int i = 0; i < stringList.size(); i++) {
            String element = cleanString(stringList.get(i));
            if (element == "") {
                stringList.remove(i);
            } else {
                stringList.set(i,element);
            }
        }
        return stringList;
    }
}
