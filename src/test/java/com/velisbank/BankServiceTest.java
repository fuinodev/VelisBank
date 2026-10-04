package com.velisbank;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BankServiceTest {
    @Autowired BankService bank;
    @Autowired CustomerRepository customers;
    @Autowired PasswordEncoder encoder;
    @Autowired PinService pins;
    String sender,receiver; Views.BankAccount from,to;
    @BeforeEach void setup() {
        sender="s"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        receiver="r"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        from=register(sender); to=register(receiver);
        pins.set(sender,new Requests.Pin("TestingPass!2026","582941","582941"));
        pins.set(receiver,new Requests.Pin("TestingPass!2026","582941","582941"));
    }
    Views.BankAccount register(String username) {return bank.register(new Requests.Registration("Test","Customer",username+"@example.com","09171234567","Manila",username,"TestingPass!2026","TestingPass!2026"));}
    Requests.Money operation(String type,String amount,String recipient) {return new Requests.Money(type,new BigDecimal(amount),recipient,"Test",UUID.randomUUID().toString(),"582941");}
    @Test void registrationCreatesZeroBalanceAndHashesPassword() {
        assertThat(from.balance()).isEqualByComparingTo("0.00");assertThat(from.number()).matches("[0-9]{10}");assertThat(from.number()).isNotEqualTo(to.number());
        Customer c=customers.findByUsername(sender).orElseThrow();assertThat(c.passwordHash).isNotEqualTo("TestingPass!2026");assertThat(encoder.matches("TestingPass!2026",c.passwordHash)).isTrue();
    }
    @Test void duplicateIdentityAndMismatchedPasswordsAreRejected() {
        assertThatThrownBy(()->register(sender)).isInstanceOf(BankException.class);
        assertThatThrownBy(()->bank.register(new Requests.Registration("Test","User","new@example.com","09171234567","Manila","newuser","TestingPass!2026","different"))).hasMessageContaining("match");
    }
    @Test void depositWithdrawalAndTransferKeepLedgerBalances() {
        bank.transact(sender,operation("DEPOSIT","1000.00",null));bank.transact(sender,operation("WITHDRAWAL","100.25",null));
        var transfer=bank.transact(sender,operation("TRANSFER","250.50",to.number()));
        assertThat(bank.account(sender).balance()).isEqualByComparingTo("649.25");assertThat(bank.account(receiver).balance()).isEqualByComparingTo("250.50");
        var incoming=bank.history(receiver).getFirst();assertThat(incoming.reference()).isEqualTo(transfer.reference());assertThat(incoming.type()).isEqualTo("TRANSFER_IN");assertThat(incoming.amount()).isEqualByComparingTo("250.50");assertThat(bank.history(sender)).hasSize(3);
    }
    @Test void reviewDoesNotMoveMoneyAndRejectsMissingRecipientAndSelfTransfer() {
        bank.transact(sender,operation("DEPOSIT","100.00",null));bank.review(sender,operation("TRANSFER","30.00",to.number()));
        assertThat(bank.account(sender).balance()).isEqualByComparingTo("100");assertThat(bank.history(sender)).hasSize(1);
        assertThatThrownBy(()->bank.review(sender,operation("TRANSFER","30","0000000000"))).hasMessageContaining("not found");
        assertThatThrownBy(()->bank.transact(sender,operation("TRANSFER","30",from.number()))).hasMessageContaining("yourself");
    }
    @Test void overdraftsAndInvalidAmountsLeaveNoPartialRecords() {
        bank.transact(sender,operation("DEPOSIT","10.00",null));
        assertThatThrownBy(()->bank.transact(sender,operation("TRANSFER","11.00",to.number()))).hasMessageContaining("Insufficient");
        for(String amount:List.of("0","-1","0.001","1000000000")) assertThatThrownBy(()->bank.transact(sender,operation("DEPOSIT",amount,null))).isInstanceOf(BankException.class);
        assertThat(bank.account(sender).balance()).isEqualByComparingTo("10");assertThat(bank.account(receiver).balance()).isZero();assertThat(bank.history(sender)).hasSize(1);assertThat(bank.history(receiver)).isEmpty();
    }
    @Test void freezingBlocksAllMoneyOperationsAndRevalidatesAfterReview() {
        bank.transact(sender,operation("DEPOSIT","100.00",null));var transfer=operation("TRANSFER","10.00",to.number());bank.review(sender,transfer);
        bank.setStatus(from.id(),"FROZEN");
        for(String type:List.of("DEPOSIT","WITHDRAWAL","TRANSFER")) assertThatThrownBy(()->bank.transact(sender,operation(type,"10.00",to.number()))).hasMessageContaining("frozen");
        assertThat(bank.history(sender)).hasSize(1);assertThat(bank.account(sender).status()).isEqualTo("FROZEN");
        bank.setStatus(from.id(),"ACTIVE");bank.setStatus(to.id(),"FROZEN");assertThatThrownBy(()->bank.transact(sender,transfer)).hasMessageContaining("frozen");
        bank.setStatus(to.id(),"ACTIVE");bank.transact(sender,transfer);assertThat(bank.account(receiver).balance()).isEqualByComparingTo("10");
    }
    @Test void duplicateRequestIsProcessedExactlyOnceAndPayloadCannotChange() {
        var request=operation("DEPOSIT","100.00",null);var first=bank.transact(sender,request);var second=bank.transact(sender,request);
        assertThat(first.id()).isEqualTo(second.id());assertThat(bank.account(sender).balance()).isEqualByComparingTo("100");assertThat(bank.history(sender)).hasSize(1);
        assertThatThrownBy(()->bank.transact(sender,new Requests.Money("DEPOSIT",new BigDecimal("200.00"),null,"Test",request.requestKey()))).hasMessageContaining("different transaction");
    }
    @Test void anotherCustomerCannotReadTransactionDetails() {
        var t=bank.transact(sender,operation("DEPOSIT","10.00",null));assertThatThrownBy(()->bank.detail(receiver,t.id())).hasMessageContaining("not found");
    }
    @Test void profileAndPasswordChangesValidateExistingIdentity() {
        bank.editProfile(sender,new Requests.Profile("Updated","Name",sender+"@example.com","09171234567","Cebu"));assertThat(bank.profile(sender).address()).isEqualTo("Cebu");
        assertThatThrownBy(()->bank.editProfile(sender,new Requests.Profile("Updated","Name",receiver+"@example.com","09171234567","Cebu"))).hasMessageContaining("registered");
        assertThatThrownBy(()->bank.changePassword(sender,new Requests.Password("wrong","NewPassword!2026","NewPassword!2026"))).hasMessageContaining("incorrect");
        bank.changePassword(sender,new Requests.Password("TestingPass!2026","NewPassword!2026","NewPassword!2026"));assertThat(encoder.matches("NewPassword!2026",customers.findByUsername(sender).orElseThrow().passwordHash)).isTrue();
    }
    @Test void concurrentWithdrawalsCannotOverdraw() throws Exception {
        bank.transact(sender,operation("DEPOSIT","100.00",null));
        try(var pool=Executors.newFixedThreadPool(8)) {
            CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> jobs=new ArrayList<>();
            for(int i=0;i<8;i++) jobs.add(pool.submit(()->{start.await();try {bank.transact(sender,operation("WITHDRAWAL","30.00",null));return true;}catch(BankException e){return false;}}));
            start.countDown();int successes=0;for(var job:jobs)if(job.get(30,TimeUnit.SECONDS))successes++;
            assertThat(successes).isEqualTo(3);assertThat(bank.account(sender).balance()).isEqualByComparingTo("10.00");assertThat(bank.history(sender)).hasSize(4);
        }
    }
    @Test void concurrentDuplicateTransfersCreditRecipientOnce() throws Exception {
        bank.transact(sender,operation("DEPOSIT","100.00",null));var request=operation("TRANSFER","20.00",to.number());
        try(var pool=Executors.newFixedThreadPool(4)) {
            List<Future<Views.Activity>> jobs=new ArrayList<>();for(int i=0;i<4;i++)jobs.add(pool.submit(()->bank.transact(sender,request)));
            Set<Long> ids=new HashSet<>();for(var job:jobs)ids.add(job.get(30,TimeUnit.SECONDS).id());assertThat(ids).hasSize(1);
        }
        assertThat(bank.account(sender).balance()).isEqualByComparingTo("80");assertThat(bank.account(receiver).balance()).isEqualByComparingTo("20");assertThat(bank.history(receiver)).hasSize(1);
    }
}
