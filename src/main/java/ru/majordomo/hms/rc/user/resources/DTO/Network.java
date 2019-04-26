package ru.majordomo.hms.rc.user.resources.DTO;

public class Network {
    public static String ipAddressInIntegerToString(Long ip) {
        return (ip >> 24 & 255L) + "." + (ip >> 16 & 255L) + "." + (ip >> 8 & 255L) + "." + (ip & 255L);
    }

    public static Long ipAddressInStringToInteger(String address) {
        long result = 0L;
        String[] ipAddressInArray = address.split("\\.");

        for(int i = 3; i >= 0; --i) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << i * 8;
        }

        return result;
    }
}
