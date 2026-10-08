package com.velisbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.*;

@Service
public class MobileAuthService {
    private final CustomerRepository customers;
    private final BankService bank;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final SecureRandom random=new SecureRandom();
    public MobileAuthService(CustomerRepository customers,BankService bank,PasswordEncoder encoder,OtpDelivery delivery){this.customers=customers;this.bank=bank;this.encoder=encoder;this.delivery=delivery;}
    Customer find(String phone) {
        String normalized=MobileNumbers.normalize(phone);
        var existing=customers.findByMobileLogin(normalized);
        if(existing.isPresent())return existing.get();
        // Existing installations can contain duplicates; never guess which account owns a number.
        var matches=customers.findAll().stream().filter(c->c.role.equals("CUSTOMER")).filter(c->{try{return MobileNumbers.normalize(c.phone).equals(normalized);}catch(BankException e){return false;}}).toList();
        if(matches.size()!=1)throw new BankException("phone","This number cannot be used to sign in. Register or contact support if you already have an account.");
        return matches.getFirst();
    }
    @Transactional
    public void register(MobileAuthController.Registration p) {
        String phone=MobileNumbers.normalize(p.phone());
        boolean used=customers.findAll().stream().anyMatch(c->{try{return MobileNumbers.normalize(c.phone).equals(phone);}catch(BankException e){return false;}});
        if(used)throw new BankException("phone","This mobile number is already registered. Log in or use Forgot PIN.");
        String id="m"+UUID.randomUUID().toString().replace("-","").substring(0,28),secret=UUID.randomUUID().toString();
        bank.register(new Requests.Registration(p.firstName(),p.lastName(),p.email(),phone,p.address(),id,secret,secret));
        customers.findByUsername(id).orElseThrow().mobileLogin=phone;
        customers.flush();
    }
    @Transactional(noRollbackFor=BankException.class)
    public Map<String,Object> send(String phone,String purpose,String binding) {
        if(!Set.of("LOGIN","RESET").contains(purpose))throw new BankException("","Invalid verification purpose.");
        Customer found=find(phone),c=customers.lockByUsername(found.username).orElseThrow();
        Instant now=Instant.now();
        if(c.otpExpires!=null && c.otpExpires.isAfter(now)) {
            // Reopening the same flow resumes its countdown without sending another SMS.
            if(Objects.equals(c.otpBinding,binding)&&Objects.equals(c.otpPurpose,purpose))return status(c);
            throw new BankException("","A code was already requested. Wait until "+c.otpExpires+" before requesting another.");
        }
        String code=String.format("%06d",random.nextInt(1_000_000));
        delivery.send(MobileNumbers.normalize(phone),code);
        c.otpHash=encoder.encode(code);c.otpBinding=binding;c.otpPurpose=purpose;c.otpExpires=now.plusSeconds(30);c.otpFailures=0;
        return status(c);
    }
    Map<String,Object> status(Customer c){return Map.of("expiresAt",c.otpExpires.toString(),"localTest",delivery.local());}
    public record Grant(String username,String purpose,Instant expires,String pinVersion) implements java.io.Serializable {}
    @Transactional(noRollbackFor=BankException.class)
    public Grant verify(String phone,String purpose,String code,String binding) {
        Customer c=customers.lockByUsername(find(phone).username).orElseThrow();
        if(c.otpExpires==null||!c.otpExpires.isAfter(Instant.now())||c.otpHash==null||!Objects.equals(c.otpBinding,binding)||!Objects.equals(c.otpPurpose,purpose))
            throw new BankException("code","This code is expired or unavailable. Request a new code after the countdown.");
        if(c.otpFailures>=3)throw new BankException("code","Too many incorrect codes. Wait for this code to expire before requesting another.");
        if(code==null||!code.matches("[0-9]{6}")||!encoder.matches(code,c.otpHash)) {c.otpFailures++;throw new BankException("code","Incorrect code. "+(3-c.otpFailures)+" attempts remaining.");}
        c.otpHash=null;c.phoneVerifiedAt=Instant.now();c.mobileLogin=MobileNumbers.normalize(phone);
        return new Grant(c.username,purpose,Instant.now().plusSeconds(300),c.pinHash);
    }
    public boolean needsPin(Grant grant){return grant.pinVersion()==null||grant.purpose().equals("RESET");}
    @Transactional(noRollbackFor=BankException.class)
    public String complete(Grant grant,String pin,String confirmation) {
        if(grant==null||!grant.expires().isAfter(Instant.now()))throw new BankException("","Verification expired. Start again with a new OTP.");
        Customer c=customers.lockByUsername(grant.username()).orElseThrow();
        if(!Objects.equals(c.pinHash,grant.pinVersion()))throw new BankException("","Your PIN changed. Start again with a new OTP.");
        if(c.pinBlockedUntil!=null&&c.pinBlockedUntil.isAfter(Instant.now()))throw new BankException("pin","PIN is locked. Try again after "+c.pinBlockedUntil+".");
        if(c.pinBlockedUntil!=null){c.pinFailures=0;c.pinBlockedUntil=null;}
        if(pin==null||!pin.matches("[0-9]{6}"))throw new BankException("pin","Enter a six-digit PIN.");
        if(needsPin(grant)) {
            if(!pin.equals(confirmation))throw new BankException("confirmPin","PINs do not match.");
            if(c.pinHash!=null&&encoder.matches(pin,c.pinHash))throw new BankException("pin","Choose a PIN different from your current PIN.");
            c.pinHash=encoder.encode(pin);
        } else if(!encoder.matches(pin,c.pinHash)) {
            c.pinFailures++;if(c.pinFailures>=3)c.pinBlockedUntil=Instant.now().plusSeconds(300);
            throw new BankException("pin",c.pinFailures>=3?"Too many incorrect PINs. Try again in 5 minutes.":"Incorrect PIN. "+(3-c.pinFailures)+" attempts remaining.");
        }
        c.pinFailures=0;c.pinBlockedUntil=null;return c.username;
    }
}
