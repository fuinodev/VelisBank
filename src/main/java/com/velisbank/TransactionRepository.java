package com.velisbank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TransactionRepository extends JpaRepository<BankTransaction,Long> {
    List<BankTransaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
    List<BankTransaction> findAllByOrderByCreatedAtDescIdDesc();
    Optional<BankTransaction> findByAccountIdAndRequestKey(Long accountId, String requestKey);
}
