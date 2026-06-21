package com.fullStack.expenseTracker.repository;

import com.fullStack.expenseTracker.models.TransactionReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionReceiptRepository extends JpaRepository<TransactionReceipt, Long> {
}
