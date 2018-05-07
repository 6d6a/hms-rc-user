package ru.majordomo.hms.rc.user.test.common;

import org.junit.Assert;
import org.junit.Test;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.rc.user.common.Utils.mapContains;

public class UtilsTest {

    private static Map<String, String> map = new HashMap<>();
    static {
        map.put("accountId", "134134134");
        map.put("resourceId", "161616146");
        map.put("null", null);
        map.put("empty", "");
    }

    @Test
    public void mapContainsTrue() throws Exception{
        Assert.assertTrue(mapContains(map, "accountId", "resourceId"));
    }

    @Test
    public void mapContainsFalseCauseNull() throws Exception{
        Assert.assertFalse(mapContains(map, "null", "resourceId"));
    }

    @Test
    public void mapContainsFalseCauseEmpty() throws Exception{
        Assert.assertFalse(mapContains(map, "empty", "resourceId"));
    }

    @Test(expected = ParameterValidationException.class)
    public void mapContainsThrowException() throws Exception{
        mapContains(map);
    }
}