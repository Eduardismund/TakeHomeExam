package blackBoxTesting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.CategoryValidator;
import ssvv.budget.validation.MemberValidator;
import ssvv.budget.validation.TransactionValidator;
import ssvv.budget.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CategoryServiceTest {
    private BudgetService budgetService;

    @BeforeEach
    void setUp(){
        budgetService = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
    }

    // add category tests
    @Test
    void testAddCategory_validData_addsSuccessfully(){
        // equivalence partitioning

        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        assertNotNull(c);
        assertEquals("Salary", c.getName());
        assertEquals(CategoryType.INCOME, c.getType());
    }

    @Test
    void testAddCategory_emptyName_throwsException(){
        // equivalence partitioning

        assertThrows(ValidationException.class, () -> {
            budgetService.addCategory("", CategoryType.INCOME);
        }, "Category name cannot be empty! ");
    }

    @Test
    void testAddCategory_nullName_throwsException(){
        // equivalence partitioning

        assertThrows(ValidationException.class, () -> {
            budgetService.addCategory(null, CategoryType.INCOME);
        }, "Category name cannot be empty! ");
    }


    @Test
    void testAddCategory_emptyType_throwsException(){
        // equivalence partitioning

        assertThrows(ValidationException.class, () -> {
            budgetService.addCategory("Salary", null);
        }, "Category type must be INCOME or EXPENSE! ");
    }

    @Test
    void testAddCategory_stateTransition_categoriesIncrease() {
        // state transition testing: if initial size of the category list is n, after adding one category it should be n+1

        List<Category> initialList = budgetService.listCategories();
        int initialSize = initialList.size();

        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        List<Category> updatedList = budgetService.listCategories();
        assertEquals(initialSize + 1, updatedList.size(), "Repository size should increase by 1");

        boolean found = updatedList.stream().anyMatch(category -> category.getId().equals(c.getId()));
        assertTrue(found, "The new category state must persist in the repository");
    }

    @Test
    void testAddCategory_specialCharactersName_addsSuccessfully() {
        // error guessing
        String weirdName = "J0hn-D0e_#1! 🎉";
        Category c = budgetService.addCategory(weirdName, CategoryType.INCOME);
        assertNotNull(c);
        assertEquals(weirdName, c.getName());
    }

    @Test
    void testAddCategory_duplicateCategories_addsSuccessfully() {
        // error guessing
        budgetService.addCategory("Duplicate", CategoryType.INCOME);
        assertNotNull(budgetService.addCategory("Duplicate", CategoryType.INCOME));
    }

    // list categories tests
    @Test
    void testListCategories_addsAndDeletes_sizeUpdates(){
        // state transition testing
        List<Category> emptyList = budgetService.listCategories();
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size(), "Initial list should be empty");

        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        List<Category> populatedList = budgetService.listCategories();
        assertEquals(1, populatedList.size(), "List should contain exactly 1 category");
        assertEquals(c.getId(), populatedList.get(0).getId());

        budgetService.deleteCategory(c.getId());

        assertEquals(0, budgetService.listCategories().size(), "List should return to 0 after deletion");
    }

    @Test
    void testListCategories_clearsReturnedList_remainsTheSame(){
        // error guessing
        budgetService.addCategory("Salary", CategoryType.INCOME);

        List<Category> list = budgetService.listCategories();

        try {
            list.clear();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertEquals(1, budgetService.listCategories().size(), "Modifying returned list shouldn't break internal state");
    }



    // update category tests
    @Test
    void testUpdateCategory_validData_updatesSuccessfully(){
        // equivalence partitioning
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Category updated = budgetService.updateCategory(c.getId(), "Television", CategoryType.EXPENSE);

        assertNotNull(updated);
        assertEquals("Television", updated.getName());
        assertEquals(CategoryType.EXPENSE, updated.getType());

        Category fetched = budgetService.listCategories().get(0);
        assertEquals("Television", fetched.getName());
    }

    @Test
    void testUpdateCategory_usesInvalidId_throwsException() {
        // equivalence partitioning
        Long nonExistentId = 999L;

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateCategory(nonExistentId, "None", CategoryType.EXPENSE);
        }, "Category " + nonExistentId + " not found");
    }

    @Test
    void testUpdateCategory_blankName_throwsException() {
        // equivalence partitioning
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        assertThrows(ValidationException.class, () -> {
            budgetService.updateCategory(c.getId(), "   ", CategoryType.EXPENSE);
        }, "Category name cannot be empty! ");
    }

    @Test
    void testUpdateCategory_nullName_throwsException() {
        // equivalence partitioning
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        assertThrows(ValidationException.class, () -> {
            budgetService.updateCategory(c.getId(), null, CategoryType.EXPENSE);
        }, "Category name cannot be empty! ");
    }

    @Test
    void testUpdateCategory_nullType_throwsException() {
        // equivalence partitioning
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        assertThrows(ValidationException.class, () -> {
            budgetService.updateCategory(c.getId(), "NullSalary", null);
        }, "Category type cannot be empty! ");
    }

    @Test
    void testUpdateCategory_updateExistingCategory_noAdditionalCategoriesAdded() {
        // error guessing
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Category updated = budgetService.updateCategory(c.getId(), "Television", CategoryType.EXPENSE);

        assertNotNull(updated);
        assertEquals(1, budgetService.listCategories().size(), "Should still remain a single record in system");
    }

    // delete category tests
    @Test
    void testDeleteCategory_deletesExistingCategory_deletesSuccessfully(){
        // equivalence partitioning
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Category deletedCategory = budgetService.deleteCategory(c.getId());
        assertNotNull(deletedCategory);
        assertEquals(c.getId(), deletedCategory.getId());
        assertFalse(budgetService.listCategories().stream().anyMatch(category -> category.getId().equals(c.getId())));
    }

    @Test
    void testDeleteCategory_usesInvalidId_throwsException(){
        // equivalence partitioning
        Long nonExistentId = 999L;

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.deleteCategory(nonExistentId);
        }, "Category 999 not found");
    }

    @Test
    void testDeleteCategory_hasTransactions_throwsException() {
        // state transition testing
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");
        budgetService.addTransaction(m.getId(), c.getId(), 30.0, LocalDate.now(), "Water bill");

        assertEquals(2, budgetService.listTransactions().size(), "Pre-condition: 2 transactions should exist");

        assertThrows(IllegalStateException.class, () -> {
            budgetService.deleteCategory(c.getId());
        }, "Cannot delete category " + c.getId() + ": transactions exist for it");

        assertEquals(2, budgetService.listTransactions().size(), "The 2 transactions should still exist");
        assertTrue(budgetService.listCategories().stream().anyMatch(category -> category.getId().equals(c.getId())));
    }

    @Test
    void testDeleteCategory_deletesTwice_throwsException(){
        // error guessing
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        assertNotNull(budgetService.deleteCategory(c.getId()));

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.deleteCategory(c.getId());
        }, "Category" + c.getId() + " not found");
    }
}
