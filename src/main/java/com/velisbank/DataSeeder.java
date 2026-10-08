package com.velisbank;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private final BankService bank; private final CustomerRepository customers; private final PasswordEncoder encoder;
    @Value("${velisbank.seed-demo:false}") boolean demo;
    @Value("${velisbank.admin.username}") String adminUsername;
    @Value("${velisbank.admin.password}") String adminPassword;
    public DataSeeder(BankService bank,CustomerRepository customers,PasswordEncoder encoder) { this.bank=bank; this.customers=customers; this.encoder=encoder; }
    @Override @Transactional public void run(String... args) {
        String username=adminUsername.trim().toLowerCase(java.util.Locale.ROOT);
        if(!adminPassword.isBlank() && !customers.existsByUsername(username)) {
            if(adminPassword.length()<10 || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 10 characters and at most 72 UTF-8 bytes.");
            Customer c=new Customer(); c.username=username; c.passwordHash=encoder.encode(adminPassword); c.firstName="Bank"; c.lastName="Administrator"; c.email=username+"@admin.velisbank.invalid"; c.phone="00000000000"; c.address="VelisBank"; c.role="ADMIN"; customers.save(c);
        }
        if(!demo || customers.existsByUsername("joshua")) return;
        bank.register(new Requests.Registration("Joshua Andrew","Aboga","joshua@example.com","09171234567","Quezon City, Philippines","joshua","Customer!2026","Customer!2026"));
        bank.register(new Requests.Registration("Juan","Dela Cruz","juan@example.com","09179876543","Manila, Philippines","juan","Customer!2026","Customer!2026"));
        bank.transact("joshua",new Requests.Money("DEPOSIT",new BigDecimal("12500.00"),null,"Opening deposit",UUID.randomUUID().toString()));
        bank.transact("juan",new Requests.Money("DEPOSIT",new BigDecimal("6500.00"),null,"Opening deposit",UUID.randomUUID().toString()));
    }
}
