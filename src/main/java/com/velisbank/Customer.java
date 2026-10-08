package com.velisbank;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable=false, unique=true, length=30) String username;
    @Column(nullable=false) String passwordHash;
    @Column(nullable=false, length=60) String firstName;
    @Column(nullable=false, length=60) String lastName;
    @Column(nullable=false, unique=true, length=160) String email;
    @Column(nullable=false, length=20) String phone;
    @Column(nullable=false, length=240) String address;
    @Column(nullable=false) String role = "CUSTOMER";
    @Column(nullable=false) Instant createdAt = Instant.now();
    @Column(unique=true,length=16) String mobileLogin;
    String otpHash;
    String otpBinding;
    String otpPurpose;
    Instant otpExpires;
    @Column(nullable=false,columnDefinition="integer default 0") int otpFailures;
    Instant phoneVerifiedAt;
    String pinHash;
    @Column(nullable=false,columnDefinition="integer default 0") int pinFailures = 0;
    Instant pinBlockedUntil;
    public String fullName() { return firstName + " " + lastName; }
}
