package ru.majordomo.hms.rc.user.test.resources;

import org.junit.Test;

import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PassportTest {
    @Test
    public void setIssuedAsString() {
        Passport passport = new Passport();
        String date = "2016-08-30";
        passport.setIssuedDate(date);
        assertThat(passport.getIssuedDate().toString(), is("2016-08-30"));
    }

    @Test
    public void setBirthdayAsString() {
        Passport passport = new Passport();
        passport.setBirthday("1990-03-08");
        assertThat(passport.getBirthday().toString(), is("1990-03-08"));
    }

    @Test
    public void equals() {
        Passport standard = ResourceGenerator.generatePassportIndividual();

        Passport tested = new Passport();
        tested.setNumber(standard.getNumber());
        tested.setIssuedDate(standard.getIssuedDate());
        tested.setBirthday(standard.getBirthday());
        tested.setIssuedOrg(standard.getIssuedOrg());
        tested.setMainPage(standard.getMainPage());
        tested.setRegisterPage(standard.getRegisterPage());

        assertNotSame(standard, tested);
        assertEquals(standard, tested);
    }
}
