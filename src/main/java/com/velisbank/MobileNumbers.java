package com.velisbank;

/** Canonical Philippine mobile numbers used by mobile authentication. */
public final class MobileNumbers {
    private MobileNumbers() {}

    public static String normalize(String value) {
        if (value == null || !value.matches("[+0-9 ()-]{10,24}")) {
            throw new BankException("phone", "Enter a valid Philippine mobile number, such as 0917 123 4567.");
        }
        String number = value.replaceAll("[ ()-]", "");
        if (number.matches("09[0-9]{9}")) number = "+63" + number.substring(1);
        else if (number.matches("9[0-9]{9}")) number = "+63" + number;
        else if (number.matches("639[0-9]{9}")) number = "+" + number;
        if (!number.matches("\\+639[0-9]{9}")) {
            throw new BankException("phone", "Enter a valid Philippine mobile number, such as 0917 123 4567.");
        }
        return number;
    }
}
