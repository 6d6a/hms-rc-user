package ru.majordomo.hms.rc.user;

import com.sun.tools.javac.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ru.majordomo.hms.rc.user.resources.WebSite;

public class CheckClass {
    private static final Logger logger = LoggerFactory.getLogger(CheckClass.class);
    public static void main(String[] args) {
        WebSite webSite = new WebSite();
        webSite.setId(ObjectId.get().toString());
        Field[] fields = WebSite.class.getFields();

        for (Field field: fields) {
            String fieldName = field.getName();
            String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                Method method = WebSite.class.getMethod(methodName);
                try {
                    method.invoke(webSite);
                } catch (IllegalAccessException e) {
                    continue;
                } catch (InvocationTargetException e) {
                    continue;
                }
            } catch (NoSuchMethodException e) {
                continue;
            }
        }

    }
}
