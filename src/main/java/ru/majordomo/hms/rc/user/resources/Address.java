package ru.majordomo.hms.rc.user.resources;

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
        if (address != null && ! address.isEmpty()) {
            StringBuilder streetBuilder = new StringBuilder();
            String[] addressParts = address.split(",");
            this.zip = Long.valueOf(addressParts[0].trim());
            this.city = addressParts[1].trim();
            for (int i = 2; i < addressParts.length; i++) {
                streetBuilder.append(addressParts[i].trim());
                streetBuilder.append(", ");
            }
            this.street = streetBuilder.toString().trim();
            if (!this.street.isEmpty() && this.street.charAt(this.street.length()-1) == ',') {
                this.street = this.street.substring(0, this.street.length()-1);
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
        return this.zip + ", " + this.city + ", " + this.street;
    }
}
