package com.velisbank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Optional<Customer> findByUsername(String username);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Customer c where c.username = :username")
    Optional<Customer> lockByUsername(@org.springframework.data.repository.query.Param("username") String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
