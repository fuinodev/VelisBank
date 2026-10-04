package com.velisbank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
class PinResetTest {
 @Autowired BankService bank;
 @Autowired PinService pins;
 @Autowired CustomerRepository customers;
 String create(){String u="p"+UUID.randomUUID().toString().replace("-","").substring(0,20);bank.register(new Requests.Registration("PIN","Test",u+"@example.com","09171234567","Manila",u,"TestingPass!2026","TestingPass!2026"));pins.set(u,new Requests.Pin("TestingPass!2026","582941","582941"));return u;}
 @Test void resetRequiresBothCredentialsAndSelectionErrorsDoNotCount(){
  String u=create();
  assertThatThrownBy(()->pins.verifyCredentials(u,new Requests.PinCredentials("wrong",null),false)).hasMessageContaining("password is incorrect");
  pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026",null),false);
  assertThatThrownBy(()->pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026","000000"),true)).hasMessageContaining("PIN is incorrect");
  pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026","582941"),true);
  assertThatThrownBy(()->pins.set(u,new Requests.Pin("TestingPass!2026","739251","739251"))).hasMessageContaining("current PIN");
  for(int i=0;i<4;i++){
   assertThatThrownBy(()->pins.set(u,new Requests.Pin("TestingPass!2026","582941","582941","582941"))).hasMessageContaining("different");
   assertThatThrownBy(()->pins.set(u,new Requests.Pin("TestingPass!2026","739251","739252","582941"))).hasMessageContaining("do not match");
  }
  assertThat(customers.findByUsername(u).orElseThrow().pinFailures).isEqualTo(2);
  pins.set(u,new Requests.Pin("TestingPass!2026","739251","739251","582941"));
  pins.verify(u,"739251");assertThat(customers.findByUsername(u).orElseThrow().pinFailures).isZero();
 }
 @Test void repeatingPasswordStepDoesNotBypassCurrentPinLock(){
  String u=create();for(int i=0;i<3;i++){pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026",null),false);assertThatThrownBy(()->pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026","000000"),true)).isInstanceOf(BankException.class);}
  assertThatThrownBy(()->pins.verifyCredentials(u,new Requests.PinCredentials("TestingPass!2026","582941"),true)).hasMessageContaining("temporarily locked");
 }
}
