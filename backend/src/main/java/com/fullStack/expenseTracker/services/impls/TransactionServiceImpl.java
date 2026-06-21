package com.fullStack.expenseTracker.services.impls;

import com.fullStack.expenseTracker.dto.reponses.*;
import com.fullStack.expenseTracker.enums.ApiResponseStatus;
import com.fullStack.expenseTracker.exceptions.*;
import com.fullStack.expenseTracker.services.CategoryService;
import com.fullStack.expenseTracker.services.TransactionService;
import com.fullStack.expenseTracker.services.UserService;
import com.fullStack.expenseTracker.dto.requests.TransactionRequestDto;
import com.fullStack.expenseTracker.models.Transaction;
import com.fullStack.expenseTracker.models.TransactionReceipt;
import com.fullStack.expenseTracker.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

// Modification: Added transactional annotation to improve performance
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final int MAX_RECEIPT_IMAGE_SIZE = 10 * 1024 * 1024;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    UserService userService;

    @Autowired
    CategoryService categoryService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto<?>> addTransaction(TransactionRequestDto transactionRequestDto)
            throws UserNotFoundException, CategoryNotFoundException, TransactionServiceLogicException {
        Transaction transaction = TransactionRequestDtoToTransaction(transactionRequestDto);
        try {
            if (transactionRequestDto.getReceiptImage() != null && !transactionRequestDto.getReceiptImage().isBlank()) {
                TransactionReceipt receipt = new TransactionReceipt();
                receipt.setTransaction(transaction);
                receipt.setImageData(decodeReceiptImage(transactionRequestDto.getReceiptImage()));
                transaction.setReceipt(receipt);
            }
            transactionRepository.save(transaction);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.CREATED,
                            "Transaction has been successfully recorded!"
                    )
            );

        }catch(Exception e) {
            log.error("Error happen when adding new transaction: " + e.getMessage());
            if (e instanceof TransactionServiceLogicException) {
                throw (TransactionServiceLogicException) e;
            }
            throw new TransactionServiceLogicException("Failed to record your new transaction, Try again later!");
        }

    }

    @Override
    // Modification: set readonly = true in Transactional annotation 
    // to ensure that the database connection remains securely open during the complex DTO data mapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponseDto<?>> getTransactionsByUser(String email,
                                                                   int pageNumber, int pageSize,
                                                                   String searchKey, String sortField,
                                                                   String sortDirec, String transactionType)
            throws TransactionServiceLogicException {

        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirec.equalsIgnoreCase("DESC")) {
            direction = Sort.Direction.DESC;
        }

        Pageable pageable =  PageRequest.of(pageNumber, pageSize).withSort(direction, sortField);

        Page<Transaction> transactions = transactionRepository.findByUser(email,
                pageable, searchKey, transactionType);

        try {
            if (transactions.getTotalElements() == 0) {
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                new PageResponseDto<>(
                                        new ArrayList<>(),
                                        0,
                                        0L
                                )
                        )
                );
            }

            List<TransactionResponseDto> transactionResponseDtoList = new ArrayList<>();

            for (Transaction transaction: transactions) {
                transactionResponseDtoList.add(transactionToTransactionResponseDto(transaction));
            }

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            new PageResponseDto<>(
                                    groupTransactionsByDate(transactionResponseDtoList),
                                    transactions.getTotalPages(),
                                    transactions.getTotalElements()
                            )
                    )
            );
        } catch (Exception e) {
            log.error("Error happen when retrieving transactions of a user: " + e.getMessage());
            throw new TransactionServiceLogicException("Failed to fetch your transactions! Try again later");
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponseDto<?>> getTransactionById(Long transactionId)
            throws TransactionNotFoundException {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id : " + transactionId)
        );

        return ResponseEntity.ok(
                new ApiResponseDto<>(
                        ApiResponseStatus.SUCCESS,
                        HttpStatus.OK,
                        transactionToTransactionResponseDto(transaction, true)
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto<?>> updateTransaction(Long transactionId, TransactionRequestDto transactionRequestDto)
            throws TransactionNotFoundException, UserNotFoundException, CategoryNotFoundException, TransactionServiceLogicException {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id : " + transactionId)
        );

        transaction.setAmount(transactionRequestDto.getAmount());
        transaction.setDate(transactionRequestDto.getDate());
        transaction.setUser(userService.findByEmail(transactionRequestDto.getUserEmail()));
        transaction.setCategory(categoryService.getCategoryById(transactionRequestDto.getCategoryId()));
        transaction.setDescription(transactionRequestDto.getDescription());

        if (transactionRequestDto.getReceiptImage() != null && !transactionRequestDto.getReceiptImage().isBlank()) {
            byte[] imageData = decodeReceiptImage(transactionRequestDto.getReceiptImage());
            if (transaction.getReceipt() == null) {
                TransactionReceipt receipt = new TransactionReceipt();
                receipt.setTransaction(transaction);
                receipt.setImageData(imageData);
                transaction.setReceipt(receipt);
            } else {
                transaction.getReceipt().setImageData(imageData);
            }
        } else if (Boolean.TRUE.equals(transactionRequestDto.getRemoveReceiptImage())) {
            transaction.setReceipt(null);
        }

        try {
            transactionRepository.save(transaction);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            "Transaction has been successfully updated!"
                    )
            );
        }catch(Exception e) {
            log.error("Error happen when retrieving transactions of a user: " + e.getMessage());
            if (e instanceof TransactionServiceLogicException) {
                throw (TransactionServiceLogicException) e;
            }
            throw new TransactionServiceLogicException("Failed to update your transactions! Try again later");
        }

    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> deleteTransaction(Long transactionId) throws TransactionNotFoundException, TransactionServiceLogicException {

        if (transactionRepository.existsById(transactionId)) {
            try {transactionRepository.deleteById(transactionId);
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                "Transaction has been successfully deleted!"
                        )
                );
            }catch(Exception e) {
                log.error("Error happen when retrieving transactions of a user: " + e.getMessage());
                throw new TransactionServiceLogicException("Failed to delete your transactions! Try again later");
            }
        }else {
            throw new TransactionNotFoundException("Transaction not found with id : " + transactionId);
        }

    }

    @Override
    // Modification: set readonly = true in Transactional annotation 
    // to ensure that the database connection remains securely open during the complex DTO data mapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponseDto<?>> getAllTransactions(int pageNumber, int pageSize, String searchKey) throws TransactionServiceLogicException {
        Pageable pageable =  PageRequest.of(pageNumber, pageSize).withSort(Sort.Direction.DESC, "transactionId");

        Page<Transaction> transactions = transactionRepository.findAll(pageable, searchKey);

        try {
            if (transactions.getTotalElements() == 0) {
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                new PageResponseDto<>(
                                        new ArrayList<>(),
                                        0,
                                        0L
                                )
                        )
                );
            }
            List<TransactionResponseDto> transactionResponseDtoList = new ArrayList<>();

            for (Transaction transaction: transactions) {
                transactionResponseDtoList.add(transactionToTransactionResponseDto(transaction));
            }

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            new PageResponseDto<>(
                                    transactionResponseDtoList,
                                    transactions.getTotalPages(),
                                    transactions.getTotalElements()
                            )
                    )
            );
        }catch (Exception e) {
            log.error("Failed to fetch All transactions: " + e.getMessage());
            throw new TransactionServiceLogicException("Failed to fetch All transactions: Try again later!");
        }
    }

    private Transaction TransactionRequestDtoToTransaction(TransactionRequestDto transactionRequestDto) throws UserNotFoundException, CategoryNotFoundException {
        return new Transaction(
                userService.findByEmail(transactionRequestDto.getUserEmail()),
                categoryService.getCategoryById(transactionRequestDto.getCategoryId()),
                transactionRequestDto.getDescription(),
                transactionRequestDto.getAmount(),
                transactionRequestDto.getDate()
        );
    }

    private TransactionResponseDto transactionToTransactionResponseDto(Transaction transaction) {
        return transactionToTransactionResponseDto(transaction, false);
    }

    private TransactionResponseDto transactionToTransactionResponseDto(Transaction transaction, boolean includeReceiptImage) {
        String receiptImage = null;
        if (includeReceiptImage && transaction.getReceipt() != null) {
            receiptImage = encodeReceiptImage(transaction.getReceipt().getImageData());
        }

        return new TransactionResponseDto(
                transaction.getTransactionId(),
                transaction.getCategory().getCategoryId(),
                transaction.getCategory().getCategoryName(),
                transaction.getCategory().getTransactionType().getTransactionTypeId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getUser().getEmail(),
                receiptImage
        );
    }

    private byte[] decodeReceiptImage(String base64Image) throws TransactionServiceLogicException {
        String cleaned = base64Image.trim();
        if (cleaned.contains(",")) {
            cleaned = cleaned.substring(cleaned.indexOf(",") + 1);
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            if (decoded.length > MAX_RECEIPT_IMAGE_SIZE) {
                throw new TransactionServiceLogicException("Receipt image must not exceed 10MB!");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new TransactionServiceLogicException("Invalid receipt image format!");
        }
    }

    private String encodeReceiptImage(byte[] receiptImage) {
        if (receiptImage == null || receiptImage.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(receiptImage);
    }

    private Map<String, List<TransactionResponseDto>> groupTransactionsByDate(List<TransactionResponseDto> transactionResponseDtoList) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        return transactionResponseDtoList.stream().collect(Collectors.groupingBy(t -> {

            if (t.getDate().equals(today)) {
                return "Today";
            }else if (t.getDate().equals(yesterday)) {
                return "Yesterday";
            }else {
                return t.getDate().toString();
            }
        }))
                .entrySet().stream()
                .sorted((entry1, entry2 ) -> {
                    if (entry1.getKey().equals("Today")) return -1;
                    else if (entry2.getKey().equals("Today")) return 1;
                    else if (entry1.getKey().equals("Yesterday")) return -1;
                    else if (entry2.getKey().equals("Yesterday")) return 1;
                    else return entry2.getKey().compareTo(entry1.getKey());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
}
