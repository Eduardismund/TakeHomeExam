package ssvv.budget.repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ssvv.budget.domain.Transaction;
import ssvv.budget.validation.Validator;
import ssvv.budget.validation.ValidationException;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionFileRepositoryTest {
    @TempDir
    Path tempDir;

    static class NoopValidator implements Validator<Transaction> {
        @Override public void validate(Transaction entity) throws ValidationException { }
    }

    @Test
    public void parseEntityParsesCorrectFormat() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Transaction t = repo.parseEntity("1;2;3;150.50;2024-05-15;payment");
        assertEquals(1L, t.getId());
        assertEquals(2L, t.getMemberId());
        assertEquals(3L, t.getCategoryId());
        assertEquals(150.50, t.getAmount());
        assertEquals(LocalDate.of(2024, 5, 15), t.getDate());
        assertEquals("payment", t.getDescription());
    }

    @Test
    public void parseEntityWithBlankDescriptionWorks() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Transaction t = repo.parseEntity("1;1;1;10.0;2024-01-01;");
        assertEquals("", t.getDescription());
    }

    @Test
    public void parseEntityThrowsOnMalformedDate() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        assertThrows(Exception.class, () -> repo.parseEntity("1;1;1;10.0;not-a-date;desc"));
    }

    @Test
    public void parseEntityThrowsOnMalformedAmount() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        assertThrows(Exception.class, () -> repo.parseEntity("1;1;1;notamount;2024-01-01;desc"));
    }

    @Test
    public void formatEntityFormatsCorrectly() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Transaction t = new Transaction(1L, 2L, 3L, 99.99, LocalDate.of(2024, 3, 20), "test");
        String formatted = repo.formatEntity(t);
        assertEquals("1;2;3;99.99;2024-03-20;test", formatted);
    }

    @Test
    public void roundTripPreservesData() {
        TransactionFileRepository repo = new TransactionFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        String original = "42;10;20;555.75;2024-12-25;holiday";
        Transaction t = repo.parseEntity(original);
        String formatted = repo.formatEntity(t);
        assertEquals(original, formatted);
    }
}
