package com.velisbank;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class Requests {
    private Requests() {}
    public record Registration(
        @NotBlank @Size(max=60) String firstName, @NotBlank @Size(max=60) String lastName,
        @NotBlank @Email @Size(max=160) String email,
        @NotBlank @Pattern(regexp="[+0-9 ()-]{7,20}",message="Enter a valid phone number.") String phone,
        @NotBlank @Size(max=240) String address,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,30}",message="Use 3–30 letters, numbers or underscores.") String username,
        @NotBlank @Size(min=10,max=64) String password, @NotBlank String confirmPassword) {}
    public record Money(
        @NotBlank @Pattern(regexp="DEPOSIT|WITHDRAWAL|TRANSFER") String type,
        @NotNull @DecimalMin(value="0.01") @DecimalMax("999999999.99") @Digits(integer=9,fraction=2) BigDecimal amount,
        @Size(max=10) String recipient, @Size(max=140) String description,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9-]{16,64}") String requestKey, @Size(max=6) String pin) {
        public Money(String type, BigDecimal amount, String recipient, String description, String requestKey) {this(type,amount,recipient,description,requestKey,null);}
    }
    public record Profile(
        @NotBlank @Size(max=60) String firstName, @NotBlank @Size(max=60) String lastName,
        @NotBlank @Email @Size(max=160) String email,
        @NotBlank @Pattern(regexp="[+0-9 ()-]{7,20}") String phone,
        @NotBlank @Size(max=240) String address) {}
    public record Password(@NotBlank String currentPassword, @NotBlank @Size(min=10,max=64) String password, @NotBlank String confirmPassword) {}
    public record Pin(@NotBlank String currentPassword, @NotBlank @Pattern(regexp="[0-9]{6}",message="Enter a six-digit PIN.") String pin, @NotBlank String confirmPin, @Size(max=6) String currentPin) {
        public Pin(String currentPassword,String pin,String confirmPin){this(currentPassword,pin,confirmPin,null);}
    }
    public record PinCredentials(@NotBlank String currentPassword, @Size(max=6) String currentPin) {}
    public record Status(@NotBlank @Pattern(regexp="ACTIVE|FROZEN") String status) {}
}
