package ssvv.budget.repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.validation.Validator;
import ssvv.budget.validation.ValidationException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryFileRepositoryTest {
    @TempDir
    Path tempDir;

    static class NoopValidator implements Validator<Category> {
        @Override
        public void validate(Category entity) throws ValidationException {
        }
    }

    @Test
    public void parseEntityParsesIncome() {
        CategoryFileRepository repo = new CategoryFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Category c = repo.parseEntity("1;Salary;INCOME");
        assertEquals(1L, c.getId());
        assertEquals("Salary", c.getName());
        assertEquals(CategoryType.INCOME, c.getType());
    }

    @Test
    public void parseEntityParsesExpense() {
        CategoryFileRepository repo = new CategoryFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Category c = repo.parseEntity("2;Rent;EXPENSE");
        assertEquals(CategoryType.EXPENSE, c.getType());
    }

    @Test
    public void parseEntityThrowsOnInvalidCategoryType() {
        CategoryFileRepository repo = new CategoryFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        assertThrows(Exception.class, () -> repo.parseEntity("1;Name;INVALID"));
    } 

    @Test
    public void formatEntityFormatsCorrectly() {
        CategoryFileRepository repo = new CategoryFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Category c = new Category(5L, "Food", CategoryType.EXPENSE);
        String formatted = repo.formatEntity(c);
        assertEquals("5;Food;EXPENSE", formatted);
    }

    @Test
    public void roundTripPreservesData() {
        CategoryFileRepository repo = new CategoryFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        String original = "10;Bonus;INCOME";
        Category c = repo.parseEntity(original);
        String formatted = repo.formatEntity(c);
        assertEquals(original, formatted);
    }
}
