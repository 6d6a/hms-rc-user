package ru.majordomo.hms.rc.user.test.resources;

import org.bson.types.ObjectId;
import org.junit.Test;

import ru.majordomo.hms.rc.user.resources.Passport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PassportTest extends Object {
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
        Passport standard = new Passport();
        standard.setNumber("4001825395");
        standard.setIssuedDate("2010-04-09");
        standard.setBirthday("1990-03-08");
        standard.setIssuedOrg("ОВД по г. Ижевск");
        standard.addPage("http://storage/" + ObjectId.get().toString());

        Passport tested = new Passport();
        tested.setNumber(standard.getNumber());
        tested.setIssuedDate(standard.getIssuedDate());
        tested.setBirthday(standard.getBirthday());
        tested.setIssuedOrg(standard.getIssuedOrg());
        tested.setPages(standard.getPages());

        assertTrue(standard != tested);
        assertTrue(standard.equals(tested));
    }
}
