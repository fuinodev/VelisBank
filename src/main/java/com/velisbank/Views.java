package com.velisbank;

import java.math.BigDecimal;
import java.time.Instant;

public final class Views {
    private Views() {}
    public record Person(Long id,String username,String firstName,String lastName,String email,String phone,String address,String role,Instant createdAt) {
        static Person of(Customer c) { return new Person(c.id,c.username,c.firstName,c.lastName,c.email,c.phone,c.address,c.role,c.createdAt); }
    }
    public record BankAccount(Long id,String number,String type,BigDecimal balance,String status,Instant createdAt,Person customer) {
        static BankAccount of(Account a) { return new BankAccount(a.id,a.number,"Savings",a.balance,a.status,a.createdAt,Person.of(a.customer)); }
    }
    public record Activity(Long id,String reference,String type,BigDecimal amount,BigDecimal balanceAfter,String accountNumber,String counterpartyNumber,String counterpartyName,String description,Instant createdAt,String status) {
        static Activity of(BankTransaction t) { return new Activity(t.id,t.reference,t.type,t.amount,t.balanceAfter,t.account.number,t.counterpartyNumber,t.counterpartyName,t.description,t.createdAt,"COMPLETED"); }
    }
}
