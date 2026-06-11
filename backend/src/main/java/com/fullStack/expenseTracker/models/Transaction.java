package com.fullStack.expenseTracker.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    @JsonIgnore
    private User user;

    // Modification: Category entity relationship mapping is transitioned from FetchType.EAGER to FetchType.LAZY
    @ManyToOne(fetch = FetchType.LAZY) // lazy = fetch category data from db only when accessed
    @JoinColumn(name = "categoryId")
    private Category category;
    private String description;
    private double amount;
    private LocalDate date;

    public Transaction(User user, Category category, String description, double amount, LocalDate date) {
        this.user = user;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }


}
