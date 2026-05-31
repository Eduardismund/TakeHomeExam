package ssvv.budget.validation;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Transaction;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionValidatorTest {
    private final TransactionValidator validator = new TransactionValidator();

    @Test
    public void validTransactionDoesNotThrow() {
        Transaction transaction = new Transaction(1L, 1L, 1L, 100.0, LocalDate.now(), "desc");
        validator.validate(transaction);
    }

    @Test 
    public void nullTransactionThrows() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    public void missingMemberOrCategoryOrDescriptionOrDateOrAmountCollectsErrors() {
        Transaction transaction = new Transaction(1L, 0L, -1L, -5.0, null, null);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(transaction));
        String message = ex.getMessage().toLowerCase();
        assertTrue(message.contains("member id") || message.contains("category id") || message.contains("amount") || message.contains("date") || message.contains("description"));
    }

    @Test 
    public void futureDateThrows() {
        Transaction transaction = new Transaction(1L, 1L, 1L, 10.0, LocalDate.now().plusDays(1), "d");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(transaction));
        assertTrue(ex.getMessage().toLowerCase().contains("date cannot be in the future"));
    }

    @Test
    public void zeroOrNegativeAmountThrows() {
        Transaction t = new Transaction(1L, 1L, 1L, 0.0, LocalDate.now(), "d");
        assertThrows(ValidationException.class, () -> validator.validate(t));
    }
}
