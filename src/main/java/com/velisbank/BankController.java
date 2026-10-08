package com.velisbank;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class BankController {
    @org.springframework.beans.factory.annotation.Value("${velisbank.legacy-auth:false}") boolean legacy;
    private final BankService bank;
    private final PinService pins;
    public BankController(BankService bank, PinService pins) { this.bank=bank;this.pins=pins; }
    @GetMapping("/pin") public Map<String,Object> pinStatus(Authentication a) {return pins.status(a.getName());}
    @PostMapping("/pin/verify-password") public Map<String,Boolean> verifyPinPassword(Authentication a,@Valid @RequestBody Requests.PinCredentials p){pins.verifyCredentials(a.getName(),p,false);return Map.of("ok",true);}
    @PostMapping("/pin/verify-current") public Map<String,Boolean> verifyCurrentPin(Authentication a,@Valid @RequestBody Requests.PinCredentials p){pins.verifyCredentials(a.getName(),p,true);return Map.of("ok",true);}
    @PutMapping("/pin") public Map<String,Boolean> setPin(Authentication a,@Valid @RequestBody Requests.Pin p) {if(!legacy)throw new BankException("","Verify an SMS code to set or reset your PIN.");pins.set(a.getName(),p);return Map.of("ok",true);}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken csrf) { return Map.of("token",csrf.getToken(),"headerName",csrf.getHeaderName()); }
    @GetMapping("/session") public Map<String,Object> session(Authentication a) {
        if(a==null || !a.isAuthenticated() || a instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) return Map.of("authenticated",false);
        return Map.of("authenticated",true,"user",bank.profile(a.getName()));
    }
    @PostMapping("/register") public Views.BankAccount register(@Valid @RequestBody Requests.Registration r) { return bank.register(r); }
    @GetMapping("/me") public Views.Person me(Authentication a) { return bank.profile(a.getName()); }
    @GetMapping("/account") public Views.BankAccount account(Authentication a) { return bank.account(a.getName()); }
    @GetMapping("/transactions") public List<Views.Activity> transactions(Authentication a) { return bank.history(a.getName()); }
    @GetMapping("/transactions/{id}") public Views.Activity detail(Authentication a,@PathVariable Long id) { return bank.detail(a.getName(),id); }
    @PostMapping("/money/verify-pin") public Map<String,Object> verifyMoneyPin(Authentication a,@Valid @RequestBody Requests.Money r) { pins.verify(a.getName(),r.pin()); return bank.review(a.getName(),r); }
    @PostMapping("/money/review") public Map<String,Object> review(Authentication a,@Valid @RequestBody Requests.Money r) { return bank.review(a.getName(),r); }
    @PostMapping("/money") public Views.Activity money(Authentication a,@Valid @RequestBody Requests.Money r) { if(r.type().equals("DEPOSIT")) pins.verify(a.getName(),r.pin()); return bank.transact(a.getName(),r); }
    @PutMapping("/profile") public Views.Person profile(Authentication a,@Valid @RequestBody Requests.Profile p) { return bank.editProfile(a.getName(),p); }
    @PutMapping("/password") public Map<String,Boolean> password(Authentication a,@Valid @RequestBody Requests.Password p,HttpServletRequest request) {
        bank.changePassword(a.getName(),p); request.getSession().invalidate(); return Map.of("ok",true);
    }
    @GetMapping("/admin/accounts") public List<Views.BankAccount> accounts() { return bank.allAccounts(); }
    @GetMapping("/admin/accounts/{id}") public Views.BankAccount adminAccount(@PathVariable Long id) { return bank.adminAccount(id); }
    @PatchMapping("/admin/accounts/{id}/status") public Views.BankAccount status(@PathVariable Long id,@Valid @RequestBody Requests.Status s) { return bank.setStatus(id,s.status()); }
    @GetMapping("/admin/transactions") public List<Views.Activity> allTransactions() { return bank.allTransactions(); }
}
