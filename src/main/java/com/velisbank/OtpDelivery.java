package com.velisbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.file.*;

@Service
public class OtpDelivery {
    private static final Logger log = LoggerFactory.getLogger(OtpDelivery.class);
    @Value("${velisbank.sms.mode:disabled}") String mode;
    @Value("${velisbank.sms.account-sid:}") String sid;
    @Value("${velisbank.sms.auth-token:}") String token;
    @Value("${velisbank.sms.sender:}") String sender;
    public boolean local() { return "local".equals(mode); }
    public void send(String phone,String code) {
        String message="VelisBank: Your verification code is "+code+". Valid for 30 seconds. Never share this code. If you did not request it, ignore this message.";
        try {
            if(local()) {
                Path folder=Path.of("data","local-sms");Files.createDirectories(folder);
                Files.writeString(folder.resolve(phone.substring(1)+".txt"),"LOCAL TEST ONLY — no SMS was sent.\n"+message);
                log.info("LOCAL TEST ONLY — VelisBank OTP: {} (mobile ending {}). Valid for 30 seconds. No SMS was sent.",
                    code, phone.substring(phone.length()-4));
                return;
            }
            if(!"twilio".equals(mode)||!sid.matches("AC[a-fA-F0-9]{32}")||token.isBlank()||sender.isBlank())
                throw new BankException("","SMS delivery is not configured. Contact the app administrator.");
            String body="To="+encode(phone)+"&From="+encode(sender)+"&Body="+encode(message);
            var request=HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+sid+"/Messages.json"))
                .timeout(Duration.ofSeconds(15)).header("Authorization","Basic "+java.util.Base64.getEncoder().encodeToString((sid+":"+token).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(request,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()!=201)throw new BankException("","The SMS provider could not accept your message. Please contact support.");
        } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new BankException("","SMS delivery was interrupted. Try again later.");}
        catch(java.io.IOException e) {throw new BankException("","SMS delivery is unavailable. Try again later.");}
    }
    private String encode(String text){return URLEncoder.encode(text,StandardCharsets.UTF_8);}
}
