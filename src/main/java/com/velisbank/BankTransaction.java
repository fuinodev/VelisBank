package com.velisbank;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="bank_transactions", uniqueConstraints=@UniqueConstraint(columnNames={"account_id", "requestKey"}))
public class BankTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="account_id",nullable=false) Account account;
    @Column(nullable=false) String reference;
    @Column(nullable=false) String requestKey;
    @Column(nullable=false) String type;
    @Column(nullable=false,precision=19,scale=2) BigDecimal amount;
    @Column(nullable=false,precision=19,scale=2) BigDecimal balanceAfter;
    String counterpartyNumber;
    String counterpartyName;
    @Column(length=140) String description;
    @Column(nullable=false) Instant createdAt = Instant.now();
}
