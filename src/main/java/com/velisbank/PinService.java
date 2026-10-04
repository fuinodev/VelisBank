package com.velisbank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Map;

@Service
public class PinService {
    private final CustomerRepository customers;
    private final PasswordEncoder encoder;
    public PinService(CustomerRepository customers,PasswordEncoder encoder){this.customers=customers;this.encoder=encoder;}
    private Customer locked(String username){return customers.lockByUsername(username).orElseThrow();}
    private void available(Customer c){
        if(c.pinBlockedUntil!=null && c.pinBlockedUntil.isAfter(Instant.now())) {
            long seconds=Math.max(1,java.time.Duration.between(Instant.now(),c.pinBlockedUntil).toSeconds()+1);
            throw new BankException("pin","PIN verification is temporarily locked. Try again in "+(seconds/60)+":"+String.format("%02d",seconds%60)+".");
        }
        if(c.pinBlockedUntil!=null){c.pinFailures=0;c.pinBlockedUntil=null;}
    }
    private void failed(Customer c,String field,String message){
        c.pinFailures++;
        if(c.pinFailures>=3){c.pinBlockedUntil=Instant.now().plusSeconds(300);throw new BankException(field,"Too many incorrect attempts. Try again in 5 minutes.");}
        throw new BankException(field,message+" "+(3-c.pinFailures)+" attempts remaining.");
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=BankException.class)
    public void verify(String username,String pin){
        Customer c=locked(username);available(c);
        if(c.pinHash==null)throw new BankException("pin","Set up your transaction PIN in Profile → Security first.");
        if(pin==null || !pin.matches("[0-9]{6}"))throw new BankException("pin","Enter your six-digit transaction PIN.");
        if(!encoder.matches(pin,c.pinHash))failed(c,"pin","Incorrect transaction PIN.");
        c.pinFailures=0;c.pinBlockedUntil=null;
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=BankException.class)
    public void set(String username,Requests.Pin p){
        Customer c=locked(username);available(c);
        if(!encoder.matches(p.currentPassword(),c.passwordHash))failed(c,"currentPassword","Your current password is incorrect.");
        if(c.pinHash!=null)currentPin(c,p.currentPin());
        if(p.pin()==null || !p.pin().matches("[0-9]{6}"))throw new BankException("pin","Enter a six-digit PIN.");
        if(!p.pin().equals(p.confirmPin()))throw new BankException("confirmPin","PINs do not match.");
        if(c.pinHash!=null && encoder.matches(p.pin(),c.pinHash))throw new BankException("pin","Choose a PIN different from your current PIN.");
        c.pinHash=encoder.encode(p.pin());c.pinFailures=0;c.pinBlockedUntil=null;
    }
    private void currentPin(Customer c,String pin){
        if(c.pinHash==null)throw new BankException("currentPin","No PIN is configured. Set up a PIN first.");
        if(pin==null || !pin.matches("[0-9]{6}"))throw new BankException("currentPin","Enter your six-digit current PIN.");
        if(!encoder.matches(pin,c.pinHash))failed(c,"currentPin","Your current PIN is incorrect.");
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=BankException.class)
    public void verifyCredentials(String username,Requests.PinCredentials p,boolean requirePin){
        Customer c=locked(username);available(c);
        if(!encoder.matches(p.currentPassword(),c.passwordHash))failed(c,"currentPassword","Your current password is incorrect.");
        if(requirePin)currentPin(c,p.currentPin());
        // Do not reset attempts between wizard steps: repeated wrong current PINs must accumulate.
    }
    @Transactional(readOnly=true)
    public Map<String,Object> status(String username){Customer c=customers.findByUsername(username).orElseThrow();return Map.of("configured",c.pinHash!=null);}
}
