package com.fullStack.expenseTracker.repository;

import com.fullStack.expenseTracker.models.Category;
import com.fullStack.expenseTracker.models.TransactionType;

// Modification: Imported EntityGraph for optimisation
import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByCategoryNameAndTransactionType(String categoryName, TransactionType transactionType);

    // Modification: Overridden findAll() method and equipped with an @EntityGraph annotation
    // to resolve the cascading "N+1+1" execution path when categories are fetched
    @Override
    @EntityGraph(attributePaths = {"transactionType"})
    List<Category> findAll();

}
