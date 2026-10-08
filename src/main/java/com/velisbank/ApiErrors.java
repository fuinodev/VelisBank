package com.velisbank;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BankException.class)
    public ResponseEntity<?> bank(BankException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getMessage(),"errors",e.field.isEmpty() ? Map.of() : Map.of(e.field,e.getMessage()))); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String,String> errors=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message","Please check the highlighted fields.","errors",errors));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message","These details are already in use. Check your username and email, then try again.")); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> malformed() { return ResponseEntity.badRequest().body(Map.of("message","Enter valid values for each field.")); }
}
