package ru.majordomo.hms.rc.user.test.resources;

import com.mysql.management.util.Str;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.majordomo.hms.rc.user.resources.Address;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class AddressTest {

    @Test
    public void constructAddressFromCommaSeparatedString() throws Exception {
        String rawAddress = "123560, Санкт-Петербург, ул.Пушкина";
        Address address = new Address(rawAddress);
        assertThat(address.getCity(), is("Санкт-Петербург"));
        assertThat(address.getZip(), is(123560L));
        assertThat(address.getStreet(), is("ул.Пушкина"));
    }

    @Test
    public void constructAddressFromCommaSperatedStringWithHomeNumberAndFlat() throws Exception {
        String rawAddress = "197000, Санкт-Петербург, Невский пр., д. 1, кв. 5";
        Address address = new Address(rawAddress);
        assertThat(address.getCity(), is("Санкт-Петербург"));
        assertThat(address.getZip(), is(197000L));
        assertThat(address.getStreet(), is("Невский пр., д. 1, кв. 5"));
    }

    @Test
    public void constructAddressFromSpaceSeparatedString() throws Exception {
        String rawAddress = "123560 Санкт-Петербург ул.Пушкина";
        Address address = new Address(rawAddress);
        assertThat(address.getCity(), is("Санкт-Петербург"));
        assertThat(address.getZip(), is(123560L));
        assertThat(address.getStreet(), is("ул.Пушкина"));
    }

    @Test
    public void constructAddressFromRandomUserInputExpectNoExceptions() throws Exception {
        List<String> postalAddresses = new ArrayList<>();
        BufferedInputStream bf = new BufferedInputStream(
                this.getClass().getClassLoader().getResourceAsStream("postal-addresses.txt"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(bf, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                postalAddresses.add(line.trim());
            }
        }
        for (String postalAddress: postalAddresses) {
            Address address = new Address(postalAddress);
            assertNotNull(address);
        }
    }
}
