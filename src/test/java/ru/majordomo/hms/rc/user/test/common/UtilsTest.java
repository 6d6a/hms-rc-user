package ru.majordomo.hms.rc.user.test.common;

import org.apache.commons.validator.routines.DomainValidator;
import org.junit.Assert;
import org.junit.Test;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.common.Utils;

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
    public void mapContainsTrue() throws Exception {
        Assert.assertTrue(mapContains(map, "accountId", "resourceId"));
    }

    @Test
    public void mapContainsFalseCauseNull() throws Exception {
        Assert.assertFalse(mapContains(map, "null", "resourceId"));
    }

    @Test
    public void mapContainsFalseCauseEmpty() throws Exception {
        Assert.assertFalse(mapContains(map, "empty", "resourceId"));
    }

    @Test(expected = ParameterValidationException.class)
    public void mapContainsThrowException() throws Exception {
        mapContains(map);
    }

    private final static DomainValidator apacheDomainValidator = DomainValidator.getInstance(true);

    @Test
    public void testDomainValidWithNonExistentTldValidSameApacheValidator() {
        String[] validDomains = {
                "1.tt",
                "yandex.ru",
                "я.рф",
                "a-1.tt",
//                "20130519032151pm._domainkey.postmarkapp.com", // почему-то apache DomainValidator такое не пропускает
                "1.tt",
                "1.tt.",
        };
        for (String validDomain : validDomains) {
            boolean customValid = Utils.domainValidWithNonExistentTld(validDomain, false, false);
            boolean apacheValid = apacheDomainValidator.isValid(validDomain);
            Assert.assertTrue(String.format(
                    "Must be right. Domain: %s, customValid: %b, apacheValid: %b",
                    validDomain,
                    customValid,
                    apacheValid
                    ), customValid && apacheValid);
        }
    }

    @Test
    public void testDomainValidWithNonExistentTldValid() {
        String[] validDomains = {
                "я.ру",
                "mail.kristall.local",
                "kristall.ololo",
                "a.t-t",
                "a.t1",
                "a.t-1",
                "aaaa.",
                "20130519032151pm._domainkey.postmarkapp.com",
                "localhost."
        };
        for (String validDomain : validDomains) {
            Assert.assertTrue(
                    "Must be right. Domain: " + validDomain,
                    Utils.domainValidWithNonExistentTld(validDomain, false, false)
            );
        }
    }

    @Test
    public void testDomainValidWithNonExistentTldInvalid() {
        String[] invalidDomains = {
                "a.only-maximum-sixty-three-symbols-length-domain-name-allowed-test-here-more",
                "a.t_t",
                null,
                "",
                "a-.tt",
                "-a.tt",
                "a.1t",
                "a.1",
                "a.t-",
                "only-maximum-sixty-three-symbols-length-domain-name-allowed-test-here-more.com"
        };
        for (String invalidDomain : invalidDomains) {
            boolean customValid = Utils.domainValidWithNonExistentTld(invalidDomain, false, false);
            boolean apacheValid = apacheDomainValidator.isValid(invalidDomain);
            Assert.assertFalse(String.format(
                    "Must be wrong. Domain: %s, customValid: %b, apacheValid: %b",
                    invalidDomain,
                    customValid,
                    apacheValid
            ), customValid || apacheValid);
        }
        Assert.assertFalse("must be invalid", Utils.domainValidWithNonExistentTld("a.a.", false, true));
        Assert.assertFalse("must be invalid", Utils.domainValidWithNonExistentTld("localhost", true, false));
    }

    @Test
    public void testDomainValidatorLocalPlus() {
        Assert.assertTrue(Utils.domainValidatorLocalPlus.isValid("test.local"));
        Assert.assertTrue(Utils.domainValidatorLocalPlus.isValid("localhost"));
        Assert.assertTrue(Utils.domainValidatorLocalPlus.isValid("test.intr"));
        Assert.assertTrue(Utils.domainValidatorLocalPlus.isValid("test.bit"));
    }
}