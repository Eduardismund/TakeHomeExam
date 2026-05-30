package blackBoxTesting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.CategoryValidator;
import ssvv.budget.validation.MemberValidator;
import ssvv.budget.validation.TransactionValidator;
import ssvv.budget.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransactionServiceTest {
    private BudgetService budgetService;

    @BeforeEach
    void setUp(){
        budgetService = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
    }

    // list transactions tests
    @Test
    void listTransactions_addsAndDeletes_sizeUpdates(){
        List<Transaction> emptyList = budgetService.listTransactions();
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size(), "Initial list should be empty");

        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Transaction t =  budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        List<Transaction> populatedList = budgetService.listTransactions();
        assertEquals(1, populatedList.size(), "List should contain exactly 1 category");
        assertEquals(t.getId(), populatedList.get(0).getId());

        budgetService.deleteTransaction(t.getId());

        assertEquals(0, budgetService.listTransactions().size(), "List should return to 0 after deletion");
    }

    @Test
    void listTransactions_clearsReturnedList_remainsTheSame(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        List<Transaction> list = budgetService.listTransactions();

        try {
            list.clear();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertEquals(1, budgetService.listTransactions().size(), "Modifying returned list shouldn't break internal state");
    }


    // update transaction tests
    @Test
    void updateTransaction_validData_updatesSuccessfully(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Member newMember = budgetService.addMember("Bob", "Parent", 2000.0);
        Category newCategory = budgetService.addCategory("Television", CategoryType.EXPENSE);

        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        Transaction updated = budgetService.updateTransaction(t.getId(), newMember.getId(), newCategory.getId(), 120, LocalDate.of(2024, 10,10), "Old Bill");

        assertNotNull(updated);
        assertEquals(newMember.getId(), updated.getMemberId());
        assertEquals(newCategory.getId(), updated.getCategoryId());
        assertEquals(120, updated.getAmount());
        assertEquals(LocalDate.of(2024, 10,10), updated.getDate());
        assertEquals("Old Bill", updated.getDescription());

        Transaction fetched = budgetService.listTransactions().get(0);
        assertEquals("Old Bill", fetched.getDescription());
    }

    @Test
    void updateTransaction_usesInvalidId_throwsException() {
        Long nonExistentId = 999L;
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateTransaction(
                    nonExistentId,
                    m.getId(),
                    c.getId(),
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Transaction " + nonExistentId + " not found");
    }

    @Test
    void updateTransaction_usesInvalidMemberId_throwsException() {
        Long nonExistentId = 999L;
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    nonExistentId,
                    c.getId(),
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Member " + nonExistentId + " does not exist");
    }

    @Test
    void updateTransaction_usesInvalidCategoryId_throwsException() {
        Long nonExistentId = 999L;
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    nonExistentId,
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Category " + nonExistentId + " does not exist");
    }

    @Test
    void updateTransaction_usesNullMemberId_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    null,
                    c.getId(),
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Member id must be a positive number! ");
    }

    @Test
    void updateTransaction_usesNullCategoryId_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    null,
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Category id must be a positive number! ");
    }

    @Test
    void updateTransaction_usesNegativeMemberId_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    -1L,
                    c.getId(),
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Member id must be a positive number! ");
    }

    @Test
    void updateTransaction_usesNegativeCategoryId_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    -1L,
                    120,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Category id must be a positive number! ");
    }

    @Test
    void updateTransaction_usesNegativeAmount_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    c.getId(),
                    -1L,
                    LocalDate.of(2024, 10,10),
                    "Old Bill"
            );
        }, "Amount must be strictly positive!");
    }

    @Test
    void updateTransaction_usesDateInFuture_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    c.getId(),
                    120,
                    LocalDate.of(2099, 10,10),
                    "Old Bill"
            );
        }, "Date cannot be in the future! ");
    }

    @Test
    void updateTransaction_usesNullDate_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    c.getId(),
                    120,
                    null,
                    "Old Bill"
            );
        }, "Date is required! ");
    }

    @Test
    void updateTransaction_usesNullDescription_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(
                    t.getId(),
                    m.getId(),
                    c.getId(),
                    120,
                    LocalDate.now(),
                    null
            );
        }, "Description cannot be null! ");
    }

    @Test
    void updateTransaction_amountExactlyZero_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Tx");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(t.getId(), m.getId(), c.getId(), 0.0, LocalDate.now(), "Update with 0");
        }, "Amount must be strictly positive!");
    }

    @Test
    void updateTransaction_invalidMemberAndCategoryId_throwsValidationException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Tx");

        assertThrows(ValidationException.class, () -> {
            budgetService.updateTransaction(t.getId(), -5L, -10L, 50.0, LocalDate.now(), "Double fail");
        },"Member " + -5L + " does not exist");
    }

    // delete transaction tests
    @Test
    void deleteTransaction_validData_deletesSuccessfully() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Gas bill");

        Transaction deleted = budgetService.deleteTransaction(t.getId());
        assertNotNull(deleted);
        assertEquals(t.getId(), deleted.getId());

        assertTrue(budgetService.listTransactions().isEmpty());
    }

    @Test
    void deleteTransaction_invalidId_throwsException() {
        Long nonExistentId = 88888L;

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.deleteTransaction(nonExistentId);
        });
    }

    @Test
    void deleteTransaction_deletesTwice_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Gas bill");

        assertNotNull(budgetService.deleteTransaction(t.getId()));

        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.deleteTransaction(t.getId());
        });
    }

    // add transaction tests

    @Test
    void addTransaction_validData_addsSuccessfully() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");

        assertNotNull(t);
        assertEquals(m.getId(), t.getMemberId());
        assertEquals(c.getId(), t.getCategoryId());
        assertEquals(50.0, t.getAmount());
    }

    @Test
    void addTransaction_nonExistentMemberId_throwsException() {
        Long nonExistentId = 999L;
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class, () -> {
            budgetService.addTransaction(nonExistentId, c.getId(), 50.0, LocalDate.now(), "Valid description");
        });
    }

    @Test
    void addTransaction_nonExistentCategoryId_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Long nonExistentId = 999L;

        assertThrows(ValidationException.class, () -> {
            budgetService.addTransaction(m.getId(), nonExistentId, 50.0, LocalDate.now(), "Valid description");
        });
    }

    @Test
    void addTransaction_amountExactlyZero_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class, () -> {
            budgetService.addTransaction(m.getId(), c.getId(), 0.0, LocalDate.now(), "Zero amount tx");
        });
    }

    @Test
    void addTransaction_amountMinimumPositive_addsSuccessfully() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 0.01, LocalDate.now(), "Min positive amount");
        assertNotNull(t);
        assertEquals(0.01, t.getAmount());
    }

    @Test
    void addTransaction_dateExactlyToday_addsSuccessfully() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 10.0, LocalDate.now(), "Today");
        assertNotNull(t);
    }

    @Test
    void addTransaction_dateTomorrow_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThrows(ValidationException.class, () -> {
            budgetService.addTransaction(m.getId(), c.getId(), 10.0, tomorrow, "Future date");
        });
    }

    @Test
    void addTransaction_blankDescription_addsSuccessfully() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Salary", CategoryType.INCOME);

        Transaction t = budgetService.addTransaction(m.getId(), c.getId(), 10.0, LocalDate.now(), "   ");
        assertNotNull(t);
        assertEquals("   ", t.getDescription());
    }

}
