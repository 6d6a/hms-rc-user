package ru.majordomo.hms.rc.user.common;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberManager {
    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static Boolean phoneValid(String phone) throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber;
        phoneNumber = phoneNumberUtil.parse(phone, "RU");

        return phoneNumberUtil.isValidNumber(phoneNumber);
    }
}
