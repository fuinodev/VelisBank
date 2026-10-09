package com.velisbank;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.util.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileAuthController {
    private final MobileAuthService auth;
    private final HttpSessionSecurityContextRepository contexts=new HttpSessionSecurityContextRepository();
    public MobileAuthController(MobileAuthService auth){this.auth=auth;}
    public record Registration(@NotBlank @Size(max=60) String firstName,@NotBlank @Size(max=60) String lastName,
        @NotBlank @Email @Size(max=160) String email,@NotBlank @Size(max=24) String phone,@NotBlank @Size(max=240) String address) {}
    public record Start(@NotBlank @Size(max=24) String phone,@Pattern(regexp="LOGIN|RESET") @NotNull String purpose) {}
    public record Verify(@NotBlank @Size(max=24) String phone,@NotNull @Pattern(regexp="LOGIN|RESET") String purpose,@NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    public record Complete(@NotBlank @Pattern(regexp="[0-9]{6}") String pin,@Size(max=6) String confirmPin) {}
    @PostMapping("/register") public Map<String,Boolean> register(@Valid @RequestBody Registration p){auth.register(p);return Map.of("ok",true);}
    @PostMapping("/otp") public Map<String,Object> send(@Valid @RequestBody Start p,HttpServletRequest req){
        var session=req.getSession();synchronized(session){var result=auth.send(p.phone(),p.purpose(),session.getId());session.removeAttribute("mobileGrant");return result;}
    }
    @PostMapping("/verify") public Map<String,Object> verify(@Valid @RequestBody Verify p,HttpServletRequest req){
        var session=req.getSession();synchronized(session){var grant=auth.verify(p.phone(),p.purpose(),p.code(),session.getId());session.setAttribute("mobileGrant",grant);return Map.of("createPin",auth.needsPin(grant));}
    }
    @GetMapping("/verification") public Map<String,Object> verification(HttpServletRequest req,HttpServletResponse res) {
        res.setHeader("Cache-Control","no-store");
        var session=req.getSession(false);
        if(session==null)return Map.of("verified",false);
        synchronized(session) {
            var grant=(MobileAuthService.Grant)session.getAttribute("mobileGrant");
            if(!auth.resumable(grant)) {session.removeAttribute("mobileGrant");return Map.of("verified",false);}
            return Map.of("verified",true,"createPin",auth.needsPin(grant),"phone",auth.verifiedPhone(grant));
        }
    }
    @DeleteMapping("/verification") public Map<String,Boolean> clearVerification(HttpServletRequest req) {
        var session=req.getSession(false);
        if(session!=null)synchronized(session){session.removeAttribute("mobileGrant");}
        return Map.of("ok",true);
    }
    @PostMapping("/complete") public Map<String,Boolean> complete(@Valid @RequestBody Complete p,HttpServletRequest req,HttpServletResponse res){
        var session=req.getSession();synchronized(session){
            var grant=(MobileAuthService.Grant)session.getAttribute("mobileGrant");
            String user=auth.complete(grant,p.pin(),p.confirmPin());session.removeAttribute("mobileGrant");
            req.changeSessionId();var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
            SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);return Map.of("ok",true);
        }
    }
}
