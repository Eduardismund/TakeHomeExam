package ssvv.budget.validation;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryValidatorTest {
    private final CategoryValidator validator = new CategoryValidator();

    @Test 
    public void validCategoryDoesNotThrow() {
        Category category = new Category(1L, "Rent", CategoryType.EXPENSE);
        assertDoesNotThrow(() -> validator.validate(category));
    }

    @Test 
    public void nullCategoryThrows() {
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be null"));
    }

    @Test
    public void blankNameOrNullTypeThrows(){
        Category c1 = new Category(1L, "", CategoryType.EXPENSE);
        Category c2 = new Category(1L, "Rent", null);
        assertThrows(ValidationException.class, () -> validator.validate(c1));
        assertThrows(ValidationException.class, () -> validator.validate(c2));
    }
}
