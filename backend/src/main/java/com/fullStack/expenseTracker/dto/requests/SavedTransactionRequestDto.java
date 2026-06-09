package com.fullStack.expenseTracker.dto.requests;

import com.fullStack.expenseTracker.enums.ETransactionFrequency;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

// Modification: Added import for @NotNull validation
import jakarta.validation.constraints.NotNull; 

@Data
@AllArgsConstructor
public class SavedTransactionRequestDto {
    private long userId;

    private int categoryId;

    private double amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private ETransactionFrequency frequency;
    
    // Modification: Added @NotNull validation
    @NotNull(message = "Start Date is required") 
    private LocalDate upcomingDate;

}
