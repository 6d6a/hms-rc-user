package ru.majordomo.hms.rc.user.resources;

import com.google.common.base.Objects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address {
    private Long zip;
    private String street;
    private String city;

    public Address() {
    }

    public Address(Long zip, String street, String city) {
        this.zip = zip;
        this.street = street;
        this.city = city;
    }

    public Address(String address) {
        if (address != null && !address.isEmpty()) {

            this.zip = findPostalIndexInAddressString(address);
            if (this.zip != null) {
                address = address.replaceAll(this.zip + "\\s?,?\\s?", "");
            }

            String[] addressParts = address.split(",");
            if (addressParts.length < 2) {
                addressParts = address.split(" ");
            }

            if (addressParts.length >= 2) {
                StringBuilder streetBuilder = new StringBuilder();

                this.city = addressParts[0].trim();

                for (int i = 1; i < addressParts.length; i++) {
                    streetBuilder.append(addressParts[i].trim());
                    streetBuilder.append(", ");
                }

                this.street = streetBuilder.toString().trim();
                if (!this.street.isEmpty() && this.street.charAt(this.street.length() - 1) == ',') {
                    this.street = this.street.substring(0, this.street.length() - 1);
                }
            } else {
                this.street = address;
            }
        }
    }

    public Long getZip() {
        return zip;
    }

    public void setZip(Long zip) {
        this.zip = zip;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "Address{" +
                "zip=" + zip +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equal(zip, address.zip) &&
                Objects.equal(street, address.street) &&
                Objects.equal(city, address.city);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(zip, street, city);
    }

    public static Long findPostalIndexInAddressString(String address) {
        String postalIndexPattern = "\\d{4,}";
        Pattern pattern = Pattern.compile(postalIndexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.find() ? Long.valueOf(matcher.group()) : null;
    }
}
