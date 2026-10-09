package com.velisbank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="velisbank.legacy-auth=false")
@ActiveProfiles("test")
class MobileAuthTest {
    @TestConfiguration static class Config {
        @Bean @Primary OtpDelivery fakeSms(){return new OtpDelivery(){@Override public void send(String phone,String code){codes.put(phone,code);}@Override public boolean local(){return false;}};}
    }
    static final Map<String,String> codes=new java.util.concurrent.ConcurrentHashMap<>();
    @Value("${local.server.port}") int port;
    @Autowired CustomerRepository customers;
    final JsonMapper json=new JsonMapper();
    class Browser {
        HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> call(String method,String path,Object data) throws Exception {
            var csrf=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
            String token=json.readTree(csrf.body()).get("token").asString();
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("X-CSRF-TOKEN",token).header("Content-Type","application/json")
                .method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
    String register(Browser b) throws Exception {
        String phone="+639"+String.format("%09d",new java.security.SecureRandom().nextInt(1_000_000_000));
        assertThat(b.call("POST","/api/mobile/register",Map.of("firstName","Mobile","lastName","Test","phone",phone,"email",UUID.randomUUID()+"@example.com","address","Manila")).statusCode()).isEqualTo(200);
        return phone;
    }
    void expire(String phone){var c=customers.findByMobileLogin(phone).orElseThrow();c.otpExpires=java.time.Instant.now().minusSeconds(1);customers.saveAndFlush(c);}
    @Autowired BankService bank;
    @Test void administratorFormRejectsCustomerEvenWithCorrectPassword() throws Exception {
        String username="test"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        bank.register(new Requests.Registration("Customer","Test",username+"@example.com","09171234567","Manila",username,"TestingPass!2026","TestingPass!2026"));
        Browser b=new Browser();
        for(String[] credentials:new String[][]{{username,"TestingPass!2026"},{"admin","TestingAdmin!2026"}}) {
            var csrf=b.call("GET","/api/csrf",null);
            String token=json.readTree(csrf.body()).get("token").asString();
            var response=b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/login"))
                .header("X-CSRF-TOKEN",token).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+credentials[0]+"&password="+URLEncoder.encode(credentials[1],java.nio.charset.StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
            boolean admin=credentials[0].equals("admin");
            assertThat(response.statusCode()).isEqualTo(admin?200:401);
            assertThat(b.call("GET","/api/admin/accounts",null).statusCode()).isEqualTo(admin?200:401);
        }
    }
    @Test void firstLoginRequiresOtpThenPinAndEveryLaterLoginRequiresBoth() throws Exception {
        Browser b=new Browser();String phone=register(b);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN")).statusCode()).isEqualTo(200);
        String code=codes.get(phone);
        assertThat(b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN")).statusCode()).isEqualTo(200);assertThat(codes.get(phone)).isEqualTo(code);
        Browser other=new Browser();assertThat(other.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",code)).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",code)).body()).contains("\"createPin\":true");
        assertThat(b.call("GET","/api/account",null).statusCode()).isEqualTo(401);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","111111")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941")).statusCode()).isEqualTo(200);
        assertThat(b.call("GET","/api/account",null).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941")).statusCode()).isEqualTo(400);
        expire(phone);
        assertThat(other.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN")).statusCode()).isEqualTo(200);
        assertThat(other.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",codes.get(phone))).body()).contains("\"createPin\":false");
        assertThat(other.call("POST","/api/mobile/complete",Map.of("pin","000000")).statusCode()).isEqualTo(400);
        assertThat(other.call("POST","/api/mobile/complete",Map.of("pin","582941")).statusCode()).isEqualTo(200);
        expire(phone);Browser reset=new Browser();reset.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","RESET"));
        reset.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","RESET","code",codes.get(phone)));
        assertThat(reset.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941")).statusCode()).isEqualTo(400);
        assertThat(reset.call("POST","/api/mobile/complete",Map.of("pin","739251","confirmPin","739251")).statusCode()).isEqualTo(200);
    }
    @Autowired MobileAuthService mobileAuth;
    @Test void verifiedSessionResumesPinWithoutAnotherOtp() throws Exception {
        Browser b=new Browser();String phone=register(b);
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":false");
        b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        String code=codes.get(phone);
        b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",code));
        for(int i=0;i<2;i++) {
            var status=b.call("GET","/api/mobile/verification",null);
            assertThat(status.body()).contains("\"verified\":true","\"createPin\":true");
            assertThat(status.headers().firstValue("Cache-Control")).contains("no-store");
        }
        assertThat(codes.get(phone)).isEqualTo(code);
        assertThat(new Browser().call("GET","/api/mobile/verification",null).body()).contains("\"verified\":false");
        assertThat(b.call("GET","/api/account",null).statusCode()).isEqualTo(401);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941")).statusCode()).isEqualTo(200);
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":false");
        b.call("POST","/api/logout",null);
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":false");
        expire(phone);
        b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",codes.get(phone)));
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":true","\"createPin\":false");
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","000000")).statusCode()).isEqualTo(400);
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":true");
        assertThat(b.call("DELETE","/api/mobile/verification",null).statusCode()).isEqualTo(200);
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains("\"verified\":false");
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941")).statusCode()).isEqualTo(400);
        var customer=customers.findByMobileLogin(phone).orElseThrow();
        assertThat(mobileAuth.resumable(new MobileAuthService.Grant(customer.username,"LOGIN",java.time.Instant.now().minusSeconds(1),customer.pinHash))).isFalse();
        assertThat(mobileAuth.resumable(new MobileAuthService.Grant(customer.username,"LOGIN",java.time.Instant.now().plusSeconds(300),"old-pin-version"))).isFalse();
    }
    @Test void forgotPinUsesFreshResetOtpImmediatelyAfterVerifiedLogin() throws Exception {
        Browser b=new Browser();String phone=register(b);
        b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",codes.get(phone)));
        b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941"));
        b.call("POST","/api/logout",null);expire(phone);
        b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",codes.get(phone)));
        assertThat(b.call("GET","/api/mobile/verification",null).body()).contains(phone);
        assertThat(new Browser().call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","RESET")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","RESET")).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","739251","confirmPin","739251")).statusCode()).isEqualTo(400);
        String resetCode=codes.get(phone);
        assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",resetCode)).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","RESET","code",resetCode)).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/complete",Map.of("pin","739251","confirmPin","739251")).statusCode()).isEqualTo(200);
        assertThat(b.call("GET","/api/account",null).statusCode()).isEqualTo(200);
    }
    @Test void profileResetRequiresOtpCurrentPinAndCancellationInvalidatesFlow() throws Exception {
        Browser b=new Browser();String phone=register(b);
        b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",codes.get(phone)));
        b.call("POST","/api/mobile/complete",Map.of("pin","582941","confirmPin","582941"));
        assertThat(new Browser().call("POST","/api/pin/reset",null).statusCode()).isEqualTo(401);
        var started=b.call("POST","/api/pin/reset",null);assertThat(started.statusCode()).isEqualTo(200);
        String id=json.readTree(started.body()).get("id").asString();
        assertThat(b.call("POST","/api/pin/reset/current",Map.of("id",id,"code","582941")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/pin/reset/otp",Map.of("id",id,"code",codes.get(phone))).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/pin/reset/complete",Map.of("id",id,"pin","739251","confirmPin","739251")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/pin/reset/current",Map.of("id",id,"code","000000")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/pin/reset/current",Map.of("id",id,"code","582941")).statusCode()).isEqualTo(200);
        assertThat(b.call("DELETE","/api/pin/reset",null).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/pin/reset/complete",Map.of("id",id,"pin","739251","confirmPin","739251")).statusCode()).isEqualTo(400);
        started=b.call("POST","/api/pin/reset",null);assertThat(started.statusCode()).isEqualTo(200);
        id=json.readTree(started.body()).get("id").asString();
        assertThat(b.call("POST","/api/pin/reset/otp",Map.of("id",id,"code",codes.get(phone))).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/pin/reset/current",Map.of("id",id,"code","582941")).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/pin/reset/complete",Map.of("id",id,"pin","739251","confirmPin","111111")).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/pin/reset/complete",Map.of("id",id,"pin","739251","confirmPin","739251")).statusCode()).isEqualTo(200);
        assertThat(b.call("POST","/api/pin/reset/complete",Map.of("id",id,"pin","582941","confirmPin","582941")).statusCode()).isEqualTo(400);
        assertThat(b.call("GET","/api/account",null).statusCode()).isEqualTo(200);
    }
    @Test void otpExpiresLocksAndCannotBeResentEarly() throws Exception {
        Browser b=new Browser();String phone=register(b);b.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN"));
        Browser other=new Browser();assertThat(other.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN")).statusCode()).isEqualTo(400);
        String good=codes.get(phone),wrong=good.equals("000000")?"111111":"000000";
        for(int i=0;i<3;i++)assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",wrong)).statusCode()).isEqualTo(400);
        assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",good)).statusCode()).isEqualTo(400);
        expire(phone);assertThat(b.call("POST","/api/mobile/verify",Map.of("phone",phone,"purpose","LOGIN","code",good)).statusCode()).isEqualTo(400);
        assertThat(other.call("POST","/api/mobile/otp",Map.of("phone",phone,"purpose","LOGIN")).statusCode()).isEqualTo(200);
    }
}
