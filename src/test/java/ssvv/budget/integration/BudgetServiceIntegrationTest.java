package ssvv.budget.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.service.MemberBudgetReport;
import ssvv.budget.validation.CategoryValidator;
import ssvv.budget.validation.MemberValidator;
import ssvv.budget.validation.TransactionValidator;
import ssvv.budget.validation.ValidationException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests: real Validator + real InMemoryRepository + real BudgetService,
 * no mocks. Verifies cross-component contracts.
 */
class BudgetServiceIntegrationTest {

    private BudgetService service;
    private InMemoryRepository<Long, Member> memberRepo;
    private InMemoryRepository<Long, Category> categoryRepo;
    private InMemoryRepository<Long, Transaction> transactionRepo;

    @BeforeEach
    void setUp() {
        memberRepo = new InMemoryRepository<>(new MemberValidator());
        categoryRepo = new InMemoryRepository<>(new CategoryValidator());
        transactionRepo = new InMemoryRepository<>(new TransactionValidator());
        service = new BudgetService(memberRepo, categoryRepo, transactionRepo);
    }

    // ── Member ────────────────────────────────────────────────────────────────

    @Test
    void addMember_validData_isSavedAndRetrievable() {
        Member m = service.addMember("Alice", "Parent", 3000.0);

        List<Member> members = service.listMembers();
        assertEquals(1, members.size());
        assertEquals("Alice", members.get(0).getName());
        assertEquals("Parent", members.get(0).getRole());
        assertEquals(3000.0, members.get(0).getMonthlyIncome());
        assertNotNull(m.getId());
    }

    @Test
    void addMember_emptyName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addMember("", "Parent", 2000.0));
        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    void addMember_negativeIncome_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addMember("Bob", "Child", -100.0));
        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    void addMember_zeroIncome_isAllowed() {
        assertDoesNotThrow(() -> service.addMember("Child", "Child", 0.0));
    }

    @Test
    void deleteMember_cascadesTransactions() {
        Member m = service.addMember("Alice", "Parent", 3000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);
        service.addTransaction(m.getId(), c.getId(), 500.0, LocalDate.now(), "bonus");

        service.deleteMember(m.getId());

        assertTrue(service.listMembers().isEmpty());
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    void deleteMember_nonExistentId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteMember(999L));
    }

    // ── Category ──────────────────────────────────────────────────────────────

    @Test
    void addCategory_validData_isSaved() {
        Category c = service.addCategory("Groceries", CategoryType.EXPENSE);

        List<Category> cats = service.listCategories();
        assertEquals(1, cats.size());
        assertEquals("Groceries", cats.get(0).getName());
        assertEquals(CategoryType.EXPENSE, cats.get(0).getType());
    }

    @Test
    void addCategory_emptyName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addCategory("", CategoryType.INCOME));
    }

    @Test
    void deleteCategory_whileTransactionReferencesIt_throwsIllegalStateException() {
        Member m = service.addMember("Alice", "Parent", 3000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);
        service.addTransaction(m.getId(), c.getId(), 1000.0, LocalDate.now(), "");

        assertThrows(IllegalStateException.class,
                () -> service.deleteCategory(c.getId()));
        assertEquals(1, service.listCategories().size());
    }

    @Test
    void deleteCategory_withNoTransactions_succeeds() {
        Category c = service.addCategory("Misc", CategoryType.EXPENSE);
        assertDoesNotThrow(() -> service.deleteCategory(c.getId()));
        assertTrue(service.listCategories().isEmpty());
    }

    // ── Transaction (addTransaction links all 3 entities) ─────────────────────

    @Test
    void addTransaction_validData_isSavedAndLinksAllEntities() {
        Member m = service.addMember("Alice", "Parent", 3000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        Transaction t = service.addTransaction(m.getId(), c.getId(), 1500.0, LocalDate.now(), "monthly");

        List<Transaction> txs = service.listTransactions();
        assertEquals(1, txs.size());
        assertEquals(m.getId(), txs.get(0).getMemberId());
        assertEquals(c.getId(), txs.get(0).getCategoryId());
        assertEquals(1500.0, txs.get(0).getAmount());
    }

    @Test
    void addTransaction_nonExistentMember_throwsValidationException() {
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class,
                () -> service.addTransaction(999L, c.getId(), 500.0, LocalDate.now(), ""));
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    void addTransaction_nonExistentCategory_throwsValidationException() {
        Member m = service.addMember("Alice", "Parent", 1000.0);

        assertThrows(ValidationException.class,
                () -> service.addTransaction(m.getId(), 999L, 500.0, LocalDate.now(), ""));
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    void addTransaction_zeroAmount_throwsValidationException() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class,
                () -> service.addTransaction(m.getId(), c.getId(), 0.0, LocalDate.now(), ""));
    }

    @Test
    void addTransaction_futureDate_throwsValidationException() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class,
                () -> service.addTransaction(m.getId(), c.getId(), 100.0,
                        LocalDate.now().plusDays(1), ""));
    }

    @Test
    void addTransaction_nullDescription_throwsValidationException() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class,
                () -> service.addTransaction(m.getId(), c.getId(), 100.0, LocalDate.now(), null));
    }

    @Test
    void addTransaction_emptyDescriptionAllowed() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);

        assertDoesNotThrow(() -> service.addTransaction(m.getId(), c.getId(), 100.0, LocalDate.now(), ""));
    }

    // ── Monthly Report (all 3 entities) ───────────────────────────────────────

    @Test
    void monthlyReport_memberWithIncomeAndExpense_correctBalance() {
        Member m = service.addMember("Alice", "Parent", 3000.0);
        Category income = service.addCategory("Salary", CategoryType.INCOME);
        Category expense = service.addCategory("Rent", CategoryType.EXPENSE);
        YearMonth month = YearMonth.of(2026, 3);

        service.addTransaction(m.getId(), income.getId(), 2000.0, LocalDate.of(2026, 3, 1), "salary");
        service.addTransaction(m.getId(), expense.getId(), 800.0, LocalDate.of(2026, 3, 15), "rent");

        List<MemberBudgetReport> report = service.monthlyReport(month);

        assertEquals(1, report.size());
        MemberBudgetReport r = report.get(0);
        assertEquals(2000.0, r.getTotalIncome(), 0.001);
        assertEquals(800.0, r.getTotalExpense(), 0.001);
        assertEquals(1200.0, r.getBalance(), 0.001);
    }

    @Test
    void monthlyReport_memberWithNoTransactionsInMonth_showsZeros() {
        Member m = service.addMember("Bob", "Child", 0.0);
        YearMonth month = YearMonth.of(2026, 3);

        List<MemberBudgetReport> report = service.monthlyReport(month);

        assertEquals(1, report.size());
        assertEquals(0.0, report.get(0).getTotalIncome(), 0.001);
        assertEquals(0.0, report.get(0).getTotalExpense(), 0.001);
        assertEquals(0.0, report.get(0).getBalance(), 0.001);
    }

    @Test
    void monthlyReport_transactionsInDifferentMonth_notCounted() {
        Member m = service.addMember("Alice", "Parent", 3000.0);
        Category income = service.addCategory("Salary", CategoryType.INCOME);
        service.addTransaction(m.getId(), income.getId(), 2000.0, LocalDate.of(2026, 2, 28), "feb salary");

        List<MemberBudgetReport> report = service.monthlyReport(YearMonth.of(2026, 3));

        assertEquals(0.0, report.get(0).getTotalIncome(), 0.001);
    }

    @Test
    void monthlyReport_multipleMembers_eachGetsCorrectRow() {
        Member alice = service.addMember("Alice", "Parent", 3000.0);
        Member bob = service.addMember("Bob", "Parent", 2000.0);
        Category income = service.addCategory("Salary", CategoryType.INCOME);
        YearMonth month = YearMonth.of(2026, 4);

        service.addTransaction(alice.getId(), income.getId(), 3000.0, LocalDate.of(2026, 4, 1), "");
        service.addTransaction(bob.getId(), income.getId(), 2000.0, LocalDate.of(2026, 4, 1), "");

        List<MemberBudgetReport> report = service.monthlyReport(month);
        assertEquals(2, report.size());

        MemberBudgetReport aliceReport = report.stream()
                .filter(r -> r.getMemberName().equals("Alice")).findFirst().orElseThrow();
        MemberBudgetReport bobReport = report.stream()
                .filter(r -> r.getMemberName().equals("Bob")).findFirst().orElseThrow();

        assertEquals(3000.0, aliceReport.getTotalIncome(), 0.001);
        assertEquals(2000.0, bobReport.getTotalIncome(), 0.001);
    }

    @Test
    void monthlyReport_nullMonth_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.monthlyReport(null));
    }

    // ── Update / Delete Transaction ────────────────────────────────────────────

    @Test
    void updateTransaction_nonExistentMember_throwsValidationException() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);
        Transaction t = service.addTransaction(m.getId(), c.getId(), 100.0, LocalDate.now(), "");

        assertThrows(ValidationException.class,
                () -> service.updateTransaction(t.getId(), 999L, c.getId(), 200.0, LocalDate.now(), ""));
    }

    @Test
    void deleteTransaction_nonExistentId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteTransaction(999L));
    }

    @Test
    void deleteTransaction_existingTransaction_removedFromRepo() {
        Member m = service.addMember("Alice", "Parent", 1000.0);
        Category c = service.addCategory("Salary", CategoryType.INCOME);
        Transaction t = service.addTransaction(m.getId(), c.getId(), 100.0, LocalDate.now(), "");

        service.deleteTransaction(t.getId());

        assertTrue(service.listTransactions().isEmpty());
    }
}
