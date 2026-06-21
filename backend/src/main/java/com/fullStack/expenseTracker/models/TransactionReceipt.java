package com.fullStack.expenseTracker.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReceipt {

    @Id
    private Long transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    @JsonIgnore
    private Transaction transaction;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;
}
