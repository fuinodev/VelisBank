package com.velisbank;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiSecurityTest {
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired PinService pins;
    final JsonMapper mapper=new JsonMapper();
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String token;
        HttpResponse<String> request(String method,String path,String body,boolean withCsrf) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(withCsrf)request.header("X-CSRF-TOKEN",token);
            if(body!=null)request.header("Content-Type",path.equals("/api/login")?"application/x-www-form-urlencoded":"application/json");
            return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
        void csrf() throws Exception {token=mapper.readTree(request("GET","/api/csrf",null,false).body()).get("token").asString();}
        JsonNode register(String user) throws Exception {
            csrf();var result=request("POST","/api/register",mapper.writeValueAsString(Map.of("firstName","Test","lastName","Customer","username",user,"email",user+"@example.com","phone","09171234567","address","Manila","password","TestingPass!2026","confirmPassword","TestingPass!2026")),true);
            assertThat(result.statusCode()).isEqualTo(200);pins.set(user,new Requests.Pin("TestingPass!2026","582941","582941"));return mapper.readTree(result.body());
        }
        void login(String username,String password) throws Exception {csrf();assertThat(request("POST","/api/login","username="+username+"&password="+URLEncoder.encode(password,java.nio.charset.StandardCharsets.UTF_8),true).statusCode()).isEqualTo(200);csrf();}
    }
    String unique() {return "u"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    @Test void resetStagesRejectIncorrectCredentials() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        var wrong=b.request("POST","/api/pin/verify-password",mapper.writeValueAsString(Map.of("currentPassword","wrong")),true);
        assertThat(wrong.statusCode()).isEqualTo(400);
        assertThat(wrong.body()).contains("current password is incorrect");
        var password=b.request("POST","/api/pin/verify-password",mapper.writeValueAsString(Map.of("currentPassword","TestingPass!2026")),true);
        assertThat(password.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(password.body()).get("ok").asBoolean()).isTrue();
        var pin=b.request("POST","/api/pin/verify-current",mapper.writeValueAsString(Map.of("currentPassword","TestingPass!2026","currentPin","000000")),true);
        assertThat(pin.statusCode()).isEqualTo(400);
        assertThat(pin.body()).contains("current PIN is incorrect");
        var correct=b.request("POST","/api/pin/verify-current",mapper.writeValueAsString(Map.of("currentPassword","TestingPass!2026","currentPin","582941")),true);
        assertThat(correct.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(correct.body()).get("ok").asBoolean()).isTrue();
    }
    @Test void sessionProbeAndFreshTokenRecoverAfterLogout() throws Exception {
        Browser b=new Browser();
        var anonymous=b.request("GET","/api/session",null,false);
        assertThat(anonymous.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(anonymous.body()).get("authenticated").asBoolean()).isFalse();
        b.login("admin","TestingAdmin!2026");
        var signedIn=mapper.readTree(b.request("GET","/api/session",null,false).body());
        assertThat(signedIn.get("authenticated").asBoolean()).isTrue();
        assertThat(signedIn.get("user").get("role").asString()).isEqualTo("ADMIN");
        assertThat(signedIn.toString()).doesNotContain("passwordHash");
        assertThat(b.request("POST","/api/logout",null,true).statusCode()).isEqualTo(204);
        assertThat(mapper.readTree(b.request("GET","/api/session",null,false).body()).get("authenticated").asBoolean()).isFalse();
        assertThat(b.request("GET","/api/admin/accounts",null,false).statusCode()).isEqualTo(401);
        assertThat(b.request("POST","/api/login","username=admin&password=TestingAdmin!2026",true).statusCode()).isEqualTo(403);
        b.login("admin","TestingAdmin!2026");
        assertThat(b.request("GET","/api/admin/accounts",null,false).statusCode()).isEqualTo(200);
    }
    @Test void publicPageLoadsButAnonymousAccountAccessFails() throws Exception {
        Browser b=new Browser();assertThat(b.request("GET","/",null,false).statusCode()).isEqualTo(200);assertThat(b.request("GET","/api/account",null,false).statusCode()).isEqualTo(401);
    }
    @Test void loginRejectsBadCredentialsAndCustomerCannotAccessAdmin() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);assertThat(b.request("POST","/api/login","username="+user+"&password=wrong",true).statusCode()).isEqualTo(401);b.login(user,"TestingPass!2026");
        assertThat(b.request("GET","/api/admin/accounts",null,false).statusCode()).isEqualTo(403);String profile=b.request("GET","/api/me",null,false).body();assertThat(profile).doesNotContain("passwordHash");assertThat(profile).contains(user);
    }
    @Test void csrfValidationAndLogoutAreEnforced() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        String payload=mapper.writeValueAsString(Map.of("pin","582941","type","DEPOSIT","amount","20.00","requestKey",UUID.randomUUID().toString()));
        assertThat(b.request("POST","/api/money",payload,false).statusCode()).isEqualTo(403);assertThat(b.request("POST","/api/money",payload,true).statusCode()).isEqualTo(200);
        assertThat(b.request("POST","/api/logout",null,true).statusCode()).isEqualTo(204);assertThat(b.request("GET","/api/account",null,false).statusCode()).isEqualTo(401);
    }
    @Test void unchangedPasswordIsRejectedWithoutEndingSession() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        var result=b.request("PUT","/api/password",mapper.writeValueAsString(Map.of("currentPassword","TestingPass!2026","password","TestingPass!2026","confirmPassword","TestingPass!2026")),true);
        assertThat(result.statusCode()).isEqualTo(400);
        assertThat(mapper.readTree(result.body()).get("errors").get("password").asString()).contains("different from your current password");
        assertThat(b.request("GET","/api/me",null,false).statusCode()).isEqualTo(200);
        b.login(user,"TestingPass!2026");
    }
    @Test void threeBadLoginsBlockCorrectCredentialsAcrossSessions() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);
        for(int left=2;left>=0;left--){
            var result=b.request("POST","/api/login","username="+user+"&password=wrong",true);
            assertThat(result.statusCode()).isEqualTo(left==0?429:401);
            if(left>0)assertThat(mapper.readTree(result.body()).get("attemptsRemaining").asInt()).isEqualTo(left);
            else assertThat(result.headers().firstValue("Retry-After")).contains("300");
        }
        Browser fresh=new Browser();fresh.csrf();
        var blocked=fresh.request("POST","/api/login","username="+user.toUpperCase(Locale.ROOT)+"&password=TestingPass!2026",true);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(mapper.readTree(blocked.body()).get("retryAfterSeconds").asInt()).isBetween(295,300);
    }
    @Test void transactionPinProtectsFundsAndPersistsFailedAttempts() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        String deposit=mapper.writeValueAsString(Map.of("pin","582941","type","DEPOSIT","amount","100.00","requestKey",UUID.randomUUID().toString()));
        assertThat(b.request("POST","/api/money",deposit,true).statusCode()).isEqualTo(200);
        String missing=mapper.writeValueAsString(Map.of("type","WITHDRAWAL","amount","10.00","requestKey",UUID.randomUUID().toString()));
        assertThat(b.request("POST","/api/money",missing,true).body()).contains("Enter your six-digit transaction PIN");
        assertThat(b.request("PUT","/api/pin",mapper.writeValueAsString(Map.of("currentPassword","TestingPass!2026","pin","582941","confirmPin","582941")),true).statusCode()).isEqualTo(400);
        assertThat(b.request("GET","/api/pin",null,false).body()).contains("true").doesNotContain("582941");
        assertThat(b.request("POST","/api/money",missing,true).statusCode()).isEqualTo(400);
        String valid=mapper.writeValueAsString(Map.of("type","WITHDRAWAL","amount","10.00","requestKey",UUID.randomUUID().toString(),"pin","582941"));
        assertThat(b.request("POST","/api/money",valid,true).statusCode()).isEqualTo(200);
        assertThat(b.request("POST","/api/money",valid,true).statusCode()).isEqualTo(200);
        for(int i=0;i<3;i++)assertThat(b.request("POST","/api/money",mapper.writeValueAsString(Map.of("type","WITHDRAWAL","amount","10.00","requestKey",UUID.randomUUID().toString(),"pin","000000")),true).statusCode()).isEqualTo(400);
        assertThat(b.request("POST","/api/money",valid,true).body()).contains("temporarily locked");
        assertThat(mapper.readTree(b.request("GET","/api/account",null,false).body()).get("balance").decimalValue()).isEqualByComparingTo("90.00");
    }
    @Test void pinVerificationPrecedesReviewAndDoesNotMoveMoney() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        String payload=mapper.writeValueAsString(Map.of("type","DEPOSIT","amount","40.00","requestKey",UUID.randomUUID().toString(),"pin","582941"));
        assertThat(b.request("POST","/api/money/verify-pin",payload,true).statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(b.request("GET","/api/account",null,false).body()).get("balance").decimalValue()).isZero();
        assertThat(b.request("POST","/api/money",payload.replace("582941","000000"),true).statusCode()).isEqualTo(400);
        assertThat(b.request("POST","/api/money",payload,true).statusCode()).isEqualTo(200);
    }
    @Test void invalidAmountReturnsFieldErrorsAndNoBalanceChange() throws Exception {
        Browser b=new Browser();String user=unique();b.register(user);b.login(user,"TestingPass!2026");
        var result=b.request("POST","/api/money",mapper.writeValueAsString(Map.of("pin","582941","type","DEPOSIT","amount","-20","requestKey",UUID.randomUUID().toString())),true);
        assertThat(result.statusCode()).isEqualTo(400);assertThat(mapper.readTree(result.body()).get("errors").has("amount")).isTrue();assertThat(mapper.readTree(b.request("GET","/api/account",null,false).body()).get("balance").decimalValue()).isZero();
    }
    @Test void adminCanFreezeAndUnfreezeButCannotUseCustomerMoneyEndpoint() throws Exception {
        Browser c=new Browser();String user=unique();JsonNode a=c.register(user);c.login(user,"TestingPass!2026");Browser admin=new Browser();admin.login("admin","TestingAdmin!2026");
        String path="/api/admin/accounts/"+a.get("id").asLong()+"/status";
        assertThat(admin.request("PATCH",path,"{\"status\":\"FROZEN\"}",true).statusCode()).isEqualTo(200);
        String payload=mapper.writeValueAsString(Map.of("pin","582941","type","DEPOSIT","amount","20.00","requestKey",UUID.randomUUID().toString()));
        assertThat(c.request("POST","/api/money",payload,true).statusCode()).isEqualTo(400);assertThat(c.request("GET","/api/transactions",null,false).statusCode()).isEqualTo(200);
        assertThat(admin.request("POST","/api/money",payload,true).statusCode()).isEqualTo(403);
        assertThat(admin.request("PATCH",path,"{\"status\":\"ACTIVE\"}",true).statusCode()).isEqualTo(200);assertThat(c.request("POST","/api/money",payload,true).statusCode()).isEqualTo(200);
    }
}
