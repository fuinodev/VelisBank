package com.velisbank;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="accounts")
public class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @OneToOne(optional=false) @JoinColumn(nullable=false, unique=true) Customer customer;
    @Column(nullable=false, unique=true, length=10) String number;
    @Column(nullable=false, precision=19, scale=2) BigDecimal balance = new BigDecimal("0.00");
    @Column(nullable=false) String status = "ACTIVE";
    @Column(nullable=false) Instant createdAt = Instant.now();
}
