package ru.majordomo.hms.rc.user.resources;

import java.util.List;

public interface Hashable {
    default List<Integer> hashes() {
        return null;
    }
}
