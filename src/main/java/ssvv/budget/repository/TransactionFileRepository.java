package ssvv.budget.repository;

import ssvv.budget.domain.Transaction;
import ssvv.budget.validation.Validator;

import java.time.LocalDate;

public class TransactionFileRepository extends AbstractFileRepository<Long, Transaction> {

    private static final String SEP = ";";

    public TransactionFileRepository(Validator<Transaction> validator, String filePath) {
        super(validator, filePath);
    }

    @Override
    protected Transaction parseEntity(String line) {
        String[] parts = line.split(SEP, -1);
        Long id = Long.parseLong(parts[0]);
        Long memberId = Long.parseLong(parts[1]);
        Long categoryId = Long.parseLong(parts[2]);
        double amount = Double.parseDouble(parts[3]);
        LocalDate date = LocalDate.parse(parts[4]);
        String description = parts[5];
        return new Transaction(id, memberId, categoryId, amount, date, description);
    }

    @Override
    protected String formatEntity(Transaction t) {
        return t.getId() + SEP +
                t.getMemberId() + SEP +
                t.getCategoryId() + SEP +
                t.getAmount() + SEP +
                t.getDate() + SEP +
                t.getDescription();
    }
}
