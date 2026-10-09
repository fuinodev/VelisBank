package com.velisbank;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/pin/reset")
public class ProfilePinResetController {
    private final MobileAuthService auth;
    private final BankService bank;
    private final PinService pins;
    public ProfilePinResetController(MobileAuthService auth,BankService bank,PinService pins){this.auth=auth;this.bank=bank;this.pins=pins;}
    record Flow(String id,String username,MobileAuthService.Grant grant,boolean currentVerified) implements java.io.Serializable {}
    public record Step(@NotBlank String id,@NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    public record Finish(@NotBlank String id,@NotBlank @Pattern(regexp="[0-9]{6}") String pin,@NotBlank @Pattern(regexp="[0-9]{6}") String confirmPin) {}
    private Flow flow(HttpServletRequest req,Authentication a,String id){
        var f=(Flow)req.getSession().getAttribute("profilePinReset");
        if(f==null||!f.id().equals(id)||!f.username().equals(a.getName()))throw new BankException("","Reset cancelled or expired. Start again from Profile.");
        return f;
    }
    @PostMapping public Map<String,Object> start(Authentication a,HttpServletRequest req){
        var session=req.getSession();synchronized(session){
            session.removeAttribute("profilePinReset");
            String phone=bank.profile(a.getName()).phone();
            var result=new HashMap<String,Object>(auth.sendProfileReset(a.getName(),session.getId()));
            String id=UUID.randomUUID().toString();
            session.setAttribute("profilePinReset",new Flow(id,a.getName(),null,false));
            result.put("id",id);result.put("maskedPhone","09*****"+phone.substring(phone.length()-4));return result;
        }
    }
    @PostMapping("/otp") public Map<String,Boolean> otp(Authentication a,@Valid @RequestBody Step p,HttpServletRequest req){
        var session=req.getSession();synchronized(session){var f=flow(req,a,p.id());
            var grant=auth.verify(bank.profile(a.getName()).phone(),"PROFILE_RESET",p.code(),session.getId());
            session.setAttribute("profilePinReset",new Flow(f.id(),f.username(),grant,false));return Map.of("ok",true);
        }
    }
    @PostMapping("/current") public Map<String,Boolean> current(Authentication a,@Valid @RequestBody Step p,HttpServletRequest req){
        var session=req.getSession();synchronized(session){var f=flow(req,a,p.id());
            if(!auth.resumable(f.grant()))throw new BankException("","OTP verification expired. Cancel and start again.");
            pins.verify(a.getName(),p.code());
            session.setAttribute("profilePinReset",new Flow(f.id(),f.username(),f.grant(),true));return Map.of("ok",true);
        }
    }
    @PostMapping("/complete") public Map<String,Boolean> complete(Authentication a,@Valid @RequestBody Finish p,HttpServletRequest req){
        var session=req.getSession();synchronized(session){var f=flow(req,a,p.id());
            if(!f.currentVerified())throw new BankException("pin","Verify your current PIN first.");
            auth.complete(f.grant(),p.pin(),p.confirmPin());session.removeAttribute("profilePinReset");return Map.of("ok",true);
        }
    }
    @DeleteMapping public Map<String,Boolean> cancel(Authentication a,HttpServletRequest req){
        var session=req.getSession();synchronized(session){session.removeAttribute("profilePinReset");return Map.of("ok",true);}
    }
}
