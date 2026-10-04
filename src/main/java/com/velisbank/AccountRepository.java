package com.velisbank;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByCustomerUsername(String username);
    Optional<Account> findByNumber(String number);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockById(Long id);
}
