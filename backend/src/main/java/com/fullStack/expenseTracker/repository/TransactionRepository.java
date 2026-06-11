package com.fullStack.expenseTracker.repository;

import com.fullStack.expenseTracker.dto.reponses.TransactionsMonthlySummaryDto;
import com.fullStack.expenseTracker.models.Transaction;
import com.fullStack.expenseTracker.models.TransactionType;
import com.fullStack.expenseTracker.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Modification: Imported EntityGraph for optimisation
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Modification: Inefficient native SQL queries rewritten into JPQL queries (optimised) & EntityGraph used
    @EntityGraph(attributePaths = {"category", "category.transactionType", "user"})
    @Query("SELECT t FROM Transaction t " +
            "JOIN t.category c " +
            "JOIN c.transactionType tt " +
            "JOIN t.user u " +
            "WHERE u.email = :email AND STR(tt.transactionTypeName) LIKE %:transactionType% AND " +
            "(t.description LIKE %:searchKey% OR c.categoryName LIKE %:searchKey%)")
    Page<Transaction> findByUser(@Param("email") String email, Pageable pageable, @Param("searchKey") String searchKey, @Param("transactionType") String transactionType);

    @EntityGraph(attributePaths = {"category", "category.transactionType", "user"})
    @Query("SELECT t FROM Transaction t " +
            "JOIN t.category c " +
            "JOIN c.transactionType tt " +
            "JOIN t.user u " +
            "WHERE t.description LIKE %:searchKey% OR c.categoryName LIKE %:searchKey% OR " +
            "STR(tt.transactionTypeName) LIKE %:searchKey% OR u.email LIKE %:searchKey%")
    Page<Transaction> findAll(Pageable pageable, @Param("searchKey") String searchKey);


    @Query(value = "SELECT SUM(amount) FROM `transaction` t " +
            "JOIN users u ON t.user_id = u.id " +
            "JOIN category c ON t.category_id = c.category_id " +
            "JOIN transaction_type tt ON c.transaction_type_id = tt.transaction_type_id " +
            "WHERE u.id = :userId AND tt.transaction_type_id = :transactionTypeId " +
            "AND MONTH(t.date) = :month AND YEAR(t.date) = :year", nativeQuery = true)
    Double findTotalByUserAndTransactionType(@Param("userId") long userId,
                                             @Param("transactionTypeId") Integer transactionTypeId,
                                             @Param("month") int month,
                                             @Param("year") int year);

    @Query(value = "SELECT COUNT(*) FROM `transaction` t JOIN users u ON t.user_id = u.id " +
            "WHERE u.id = :userId AND MONTH(t.date) = :month AND YEAR(t.date) = :year", nativeQuery = true)
    Integer findTotalNoOfTransactionsByUser(@Param("userId") long userId, @Param("month") int month, @Param("year") int year);

    @Query(value = "SELECT SUM(amount) FROM `transaction` t " +
            "JOIN users u ON t.user_id = u.id " +
            "JOIN category c ON t.category_id = c.category_id " +
            "WHERE u.email = :email and c.category_id = :categoryId " +
            "AND MONTH(t.date) = :month AND YEAR(t.date) = :year", nativeQuery = true)
    Double findTotalByUserAndCategory(@Param("email") String email,
                                      @Param("categoryId") int categoryId,
                                      @Param("month") int month,
                                      @Param("year") int year);

    @Query(value = "SELECT " +
            "MONTH(t.date), " +
            "SUM(CASE WHEN tt.transaction_type_id = 1 THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN tt.transaction_type_id = 2 THEN t.amount ELSE 0 END) " +
            "FROM transaction t " +
            "JOIN users u on t.user_id = u.id " +
            "JOIN category c on t.category_id = c.category_id " +
            "JOIN transaction_type tt on c.transaction_type_id = tt.transaction_type_id " +
            "WHERE u.email = :email and t.date >= DATE_SUB(CURRENT_DATE(), INTERVAL 5 MONTH) " +
            "GROUP BY YEAR(t.date), MONTH(t.date)", nativeQuery = true)
    List<Object[]> findMonthlySummaryByUser(@Param("email") String email);
}