package com.velisbank;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttempts {
    private static final long COOLDOWN = 300_000;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    public LoginAttempts() { this(Clock.systemUTC()); }
    LoginAttempts(Clock clock) { this.clock=clock; }
    static class Attempt { int failures; long expires; }
    String key(String username) { return username==null?"":username.trim().toLowerCase(Locale.ROOT); }
    Attempt entry(String username) {
        long now=clock.millis();
        // Remove expired counters without evicting active cooldowns.
        attempts.entrySet().removeIf(e -> { synchronized(e.getValue()) { return e.getValue().expires < now; } });
        return attempts.computeIfAbsent(key(username),k -> {var a=new Attempt();a.expires=now+COOLDOWN;return a;});
    }
    long remaining(Attempt a) { return a.failures>=3?Math.max(0,(a.expires-clock.millis()+999)/1000):0; }
    int failed(Attempt a) { if(a.expires<=clock.millis())a.failures=0;a.failures++;a.expires=clock.millis()+COOLDOWN;return Math.max(0,3-a.failures); }
    void success(Attempt a) { a.failures=0;a.expires=clock.millis()+COOLDOWN; }
    static void blocked(HttpServletResponse response,long seconds) throws IOException {
        response.setStatus(429);response.setContentType("application/json");response.setHeader("Retry-After",Long.toString(seconds));
        response.getWriter().write("{\"message\":\"Too many unsuccessful login attempts. Please wait before trying again.\",\"retryAfterSeconds\":"+seconds+"}");
    }
    public OncePerRequestFilter filter() {
        return new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                if(!request.getServletPath().equals("/api/login") || !request.getMethod().equals("POST")){chain.doFilter(request,response);return;}
                Attempt a=entry(request.getParameter("username"));
                // Serialize authentication for the same username to prevent parallel attempts bypassing the limit.
                synchronized(a) {
                    long wait=remaining(a);if(wait>0){blocked(response,wait);return;}
                    request.setAttribute("loginAttempt",a);chain.doFilter(request,response);
                }
            }
        };
    }
    public void onFailure(HttpServletRequest request,HttpServletResponse response) throws IOException {
        Attempt a=(Attempt)request.getAttribute("loginAttempt");int left=failed(a);
        if(left==0){blocked(response,remaining(a));return;}
        response.setStatus(401);response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Incorrect username or password.\",\"attemptsRemaining\":"+left+"}");
    }
    public void onSuccess(HttpServletRequest request) {success((Attempt)request.getAttribute("loginAttempt"));}
}
