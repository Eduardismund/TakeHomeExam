package ssvv.budget.validation;

import ssvv.budget.domain.Transaction;

import java.time.LocalDate;

public class TransactionValidator implements Validator<Transaction> {

    @Override
    public void validate(Transaction transaction) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        if (transaction == null) {
            throw new ValidationException("Transaction cannot be null!");
        }
        if (transaction.getMemberId() == null || transaction.getMemberId() <= 0) {
            errors.append("Member id must be a positive number! ");
        }
        if (transaction.getCategoryId() == null || transaction.getCategoryId() <= 0) {
            errors.append("Category id must be a positive number! ");
        }
        if (transaction.getAmount() <= 0) {
            errors.append("Amount must be strictly positive! ");
        }
        if (transaction.getDate() == null) {
            errors.append("Date is required! ");
        } else if (transaction.getDate().isAfter(LocalDate.now())) {
            errors.append("Date cannot be in the future! ");
        }
        if (transaction.getDescription() == null) {
            errors.append("Description cannot be null! ");
        }
        if (errors.length() > 0) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}
