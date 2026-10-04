package com.velisbank;
public class BankException extends RuntimeException {
    final String field;
    public BankException(String field, String message) { super(message); this.field=field; }
}
