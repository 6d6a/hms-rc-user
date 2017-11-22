package ru.majordomo.hms.rc.user.resources.DTO;

import java.util.List;

public class ObjectContainer<T>{
    private List<T> object;

    public List<T> getObject() {
        return object;
    }

    public void setObject(List<T> object) {
        this.object = object;
    }
}
