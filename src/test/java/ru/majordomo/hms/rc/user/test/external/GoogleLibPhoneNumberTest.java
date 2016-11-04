package ru.majordomo.hms.rc.user.test.external;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class GoogleLibPhoneNumberTest {

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private Phonenumber.PhoneNumber phoneNumber;

    @Test
    public void validateCityPhone() throws Exception {
        String input = "7-3412-22-1502";

        assertThat(phoneValid(input), is(true));
    }

    @Test
    public void validateMobilePhone() throws Exception {
        String input = "+79052033565";

        assertThat(phoneValid(input), is(true));
    }

    @Test
    public void validateUkrainianCityPhone() throws Exception {
        String input = "+380 44 123-45-67";
        assertThat(phoneValid(input), is(true));
    }

    @Test
    public void validateUkrainianMobilePhone() throws Exception {
        String input = "+380 99 123-45-67";
        assertThat(phoneValid(input), is(true));
    }

    @Test
    public void validateBelarusianCityPhone() throws Exception {
        String input = "+375 162204117";
        assertThat(phoneValid(input), is(true));
    }

    @Test
    public void validateBelarusianMobilePhone() throws Exception {
        String input = "+375 29-220-41-17";
        assertThat(phoneValid(input), is(true));
    }

    private Boolean phoneValid(String phone) throws Exception {
        phoneNumber = phoneNumberUtil.parse(phone, "RU");
        return phoneNumberUtil.isValidNumber(phoneNumber);
    }
}
