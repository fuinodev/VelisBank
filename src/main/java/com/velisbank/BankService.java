package com.velisbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional
public class BankService {
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final PasswordEncoder passwords;
    private final PinService pins;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final SecureRandom random = new SecureRandom();
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");
    public BankService(CustomerRepository customers, AccountRepository accounts, TransactionRepository transactions, PasswordEncoder passwords, PinService pins) {
        this.pins=pins;
        this.customers=customers; this.accounts=accounts; this.transactions=transactions; this.passwords=passwords;
    }
    public Views.BankAccount register(Requests.Registration r) {
        checkPassword(r.password(),r.confirmPassword());
        String username=r.username().trim().toLowerCase(Locale.ROOT), email=r.email().trim().toLowerCase(Locale.ROOT);
        if(customers.existsByUsername(username)) throw new BankException("username","This username is already taken.");
        if(customers.existsByEmail(email)) throw new BankException("email","This email is already registered.");
        Customer c = new Customer();
        c.username=username; c.email=email; c.firstName=r.firstName().trim(); c.lastName=r.lastName().trim();
        c.phone=r.phone().trim(); c.address=r.address().trim(); c.passwordHash=passwords.encode(r.password());
        customers.save(c);
        Account a=new Account(); a.customer=c;
        do { a.number=String.valueOf(1_000_000_000L + random.nextLong(9_000_000_000L)); } while(accounts.findByNumber(a.number).isPresent());
        return Views.BankAccount.of(accounts.saveAndFlush(a));
    }
    Customer person(String username) { return customers.findByUsername(username).orElseThrow(() -> new BankException("","Customer was not found.")); }
    Account own(String username) { return accounts.findByCustomerUsername(username).orElseThrow(() -> new BankException("","Account was not found.")); }
    public Views.Person profile(String username) { return Views.Person.of(person(username)); }
    public Views.BankAccount account(String username) { return Views.BankAccount.of(own(username)); }
    public List<Views.Activity> history(String username) { return transactions.findByAccountIdOrderByCreatedAtDescIdDesc(own(username).id).stream().map(Views.Activity::of).toList(); }
    public Views.Activity detail(String username, Long id) {
        BankTransaction t=transactions.findById(id).orElseThrow(() -> new BankException("","Transaction was not found."));
        if(!t.account.customer.username.equals(username)) throw new BankException("","Transaction was not found.");
        return Views.Activity.of(t);
    }
    private void active(Account a) { if(!"ACTIVE".equals(a.status)) throw new BankException("","This account is frozen. Transactions are currently unavailable."); }
    private Account recipient(Requests.Money r, Account from) {
        Account to=accounts.findByNumber(r.recipient()==null ? "" : r.recipient().trim()).orElseThrow(() -> new BankException("recipient","Recipient account was not found."));
        if(to.id.equals(from.id)) throw new BankException("recipient","Choose a different account. You cannot transfer to yourself.");
        return to;
    }
    private void validateMoney(Requests.Money r, Account from, Account to) {
        active(from);
        if(!Set.of("DEPOSIT","WITHDRAWAL","TRANSFER").contains(r.type())) throw new BankException("type","Choose a valid transaction type.");
        if(r.amount()==null || r.amount().signum()<=0 || r.amount().scale()>2 || r.amount().compareTo(new BigDecimal("999999999.99"))>0)
            throw new BankException("amount","Enter an amount from ₱0.01 to ₱999,999,999.99 with at most two decimals.");
        if(!r.type().equals("DEPOSIT") && from.balance.compareTo(r.amount())<0) throw new BankException("amount","Insufficient balance for this transaction.");
        if(to!=null) {
            if(!to.status.equals("ACTIVE")) throw new BankException("recipient","The recipient account is frozen and cannot receive funds.");
            if(to.balance.add(r.amount()).compareTo(MAX_BALANCE)>0) throw new BankException("amount","The recipient account cannot accept this amount.");
        }
        if(r.type().equals("DEPOSIT") && from.balance.add(r.amount()).compareTo(MAX_BALANCE)>0) throw new BankException("amount","This amount exceeds the account balance limit.");
    }
    public Map<String,Object> review(String username, Requests.Money r) {
        Account from=own(username), to=r.type().equals("TRANSFER") ? recipient(r,from) : null;
        validateMoney(r,from,to);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("amount",r.amount()); result.put("type",r.type()); result.put("balance",from.balance);
        if(to!=null) { result.put("recipientName",to.customer.fullName()); result.put("recipientNumber",to.number); }
        return result;
    }
    public Views.Activity transact(String username, Requests.Money r) {
        if(!r.type().equals("DEPOSIT")) pins.verify(username,r.pin());
        Account snapshot=own(username);
        Account toSnapshot=r.type().equals("TRANSFER") ? recipient(r,snapshot) : null;
        // A consistent lock order prevents two opposing transfers from deadlocking.
        List<Long> ids=new ArrayList<>(); ids.add(snapshot.id); if(toSnapshot!=null) ids.add(toSnapshot.id); ids.sort(Long::compareTo);
        Map<Long,Account> locked=new HashMap<>();
        for(Long id:ids) {
            Account current=accounts.lockById(id).orElseThrow();
            entityManager.refresh(current);
            locked.put(id,current);
        }
        Account from=locked.get(snapshot.id), to=toSnapshot==null ? null : locked.get(toSnapshot.id);
        Optional<BankTransaction> prior=transactions.findByAccountIdAndRequestKey(from.id,r.requestKey());
        if(prior.isPresent()) {
            BankTransaction t=prior.get();
            String expectedType=r.type().equals("TRANSFER") ? "TRANSFER_OUT" : r.type();
            if(!t.type.equals(expectedType) || t.amount.abs().compareTo(r.amount())!=0 || !Objects.equals(t.counterpartyNumber,to==null ? null : to.number) || !Objects.equals(t.description,description(r)))
                throw new BankException("","This request reference was already used for a different transaction.");
            return Views.Activity.of(t);
        }
        validateMoney(r,from,to);
        String reference="VEL-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        BigDecimal signed=r.type().equals("DEPOSIT") ? r.amount() : r.amount().negate();
        from.balance=from.balance.add(signed);
        BankTransaction debit=record(from,r.type().equals("TRANSFER") ? "TRANSFER_OUT" : r.type(),signed,to,reference,r.requestKey(),description(r));
        if(to!=null) {
            to.balance=to.balance.add(r.amount());
            record(to,"TRANSFER_IN",r.amount(),from,reference,"incoming-"+UUID.randomUUID(),description(r));
        }
        accounts.flush();
        return Views.Activity.of(debit);
    }
    private String description(Requests.Money r) { return r.description()==null ? "" : r.description().trim(); }
    private BankTransaction record(Account a,String type,BigDecimal amount,Account other,String reference,String key,String description) {
        BankTransaction t=new BankTransaction(); t.account=a; t.type=type; t.amount=amount; t.balanceAfter=a.balance; t.reference=reference; t.requestKey=key; t.description=description;
        if(other!=null) { t.counterpartyNumber=other.number; t.counterpartyName=other.customer.fullName(); }
        return transactions.save(t);
    }
    public Views.Person editProfile(String username,Requests.Profile p) {
        Customer c=person(username); String email=p.email().trim().toLowerCase(Locale.ROOT);
        if(!c.email.equals(email) && customers.existsByEmail(email)) throw new BankException("email","This email is already registered.");
        c.firstName=p.firstName().trim(); c.lastName=p.lastName().trim(); c.email=email; c.phone=p.phone().trim(); c.address=p.address().trim();
        customers.flush(); return Views.Person.of(c);
    }
    private void checkPassword(String password,String confirmation) {
        if(!password.equals(confirmation)) throw new BankException("confirmPassword","Passwords do not match.");
        if(password.length()<10 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72) throw new BankException("password","Use 10–64 characters (at most 72 UTF-8 bytes).");
    }
    public void changePassword(String username,Requests.Password p) {
        Customer c=person(username);
        if(!passwords.matches(p.currentPassword(),c.passwordHash)) throw new BankException("currentPassword","Your current password is incorrect.");
        checkPassword(p.password(),p.confirmPassword());
        if(passwords.matches(p.password(),c.passwordHash)) throw new BankException("password","Your new password must be different from your current password.");
        c.passwordHash=passwords.encode(p.password());
    }
    public List<Views.BankAccount> allAccounts() { return accounts.findAll().stream().map(Views.BankAccount::of).toList(); }
    public Views.BankAccount adminAccount(Long id) { return Views.BankAccount.of(accounts.findById(id).orElseThrow(() -> new BankException("","Account was not found."))); }
    public List<Views.Activity> allTransactions() { return transactions.findAllByOrderByCreatedAtDescIdDesc().stream().map(Views.Activity::of).toList(); }
    public Views.BankAccount setStatus(Long id,String status) {
        Account a=accounts.lockById(id).orElseThrow(() -> new BankException("","Account was not found.")); a.status=status; return Views.BankAccount.of(a);
    }
}

