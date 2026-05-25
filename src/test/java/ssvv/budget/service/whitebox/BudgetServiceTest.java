package ssvv.budget.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.Repository;
import ssvv.budget.validation.ValidationException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BudgetServiceTest {
    Repository<Long, Member> memberRepo;
    Repository<Long, Category> categoryRepo;
    Repository<Long, Transaction> transactionRepo;
    BudgetService service;

    @BeforeEach
    public void setUp() {
        memberRepo = mock(Repository.class);
        categoryRepo = mock(Repository.class);
        transactionRepo = mock(Repository.class);
        when(memberRepo.findAll()).thenReturn(List.of());
        when(categoryRepo.findAll()).thenReturn(List.of());
        when(transactionRepo.findAll()).thenReturn(List.of());
        service = new BudgetService(memberRepo, categoryRepo, transactionRepo);
    }

    @Test 
    public void addMemberCreatesAndReturnsMember() {
        Member m = new Member(1L, "Name", "Role", 1000.0);
        when(memberRepo.save(any())).thenReturn(Optional.empty());
        Member result = service.addMember("Name", "Role", 1000.0);
        assertNotNull(result.getId());
        assertEquals("Name", result.getName());
        verify(memberRepo).save(any());
    }

    @Test
    public void addMemberThrowsIfMemberExists() {
        when(memberRepo.save(any())).thenReturn(Optional.of(new Member(1L, "Existing", "Role", 1000.0)));
        assertThrows(IllegalStateException.class, () -> service.addMember("Name", "Role", 1000.0));
    }

    @Test 
    public void updateMemberThrowsWhenMemberNotFound() {
        when(memberRepo.update(any())).thenReturn(Optional.of(new Member(42L, "Name", "Role", 1000.0)));
        assertThrows(IllegalArgumentException.class, () -> service.updateMember(42L, "Name", "Role", 1000.0));
    }

    @Test
    public void deleteMemberRemovesMemberAndCascadeDeletesTransactions() {
        Member m = new Member(1L, "A", "R", 0);
        Transaction t1 = new Transaction(1L, 1L, 2L, 10, LocalDate.now(), "d");
        when(memberRepo.delete(1L)).thenReturn(Optional.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(t1));
        when(transactionRepo.delete(1L)).thenReturn(Optional.of(t1));
        Member deleted = service.deleteMember(1L);
        assertEquals(m, deleted);
        verify(transactionRepo).delete(1L);
    }

    @Test
    public void deleteMemberThrowsWhenNotFound() {
        when(memberRepo.delete(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.deleteMember(99L));
    }

    @Test
    public void addCategoryCreatesCategory() {
        when(categoryRepo.save(any())).thenReturn(Optional.empty());
        Category c = service.addCategory("C", CategoryType.EXPENSE);
        assertNotNull(c.getId());
        assertEquals("C", c.getName());
        verify(categoryRepo).save(any());
    }

    @Test
    public void deleteCategoryFailsWhenTransactionsExist() {
        Transaction t = new Transaction(1L, 1L, 2L, 10, LocalDate.now(), "d");
        when(transactionRepo.findAll()).thenReturn(List.of(t));
        assertThrows(IllegalStateException.class, () -> service.deleteCategory(2L));
        verify(categoryRepo, never()).delete(2L);
    }

    @Test
    public void addTransactionSucceedsWhenMemberAndCategoryExist() {
        Member m = new Member(1L, "A", "R", 0);
        Category c = new Category(1L, "X", CategoryType.INCOME);
        when(memberRepo.findOne(1L)).thenReturn(Optional.of(m));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(c));
        when(transactionRepo.save(any())).thenReturn(Optional.empty());

        Transaction t = service.addTransaction(1L, 1L, 50.0, LocalDate.now(), "desc");
        assertNotNull(t.getId());
        assertEquals(50.0, t.getAmount());
        verify(transactionRepo).save(any());
    }

    @Test
    public void addTransactionThrowsWhenMemberMissing() {
        when(memberRepo.findOne(99L)).thenReturn(Optional.empty());
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(new Category(1L, "X", CategoryType.EXPENSE)));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.addTransaction(99L, 1L, 1.0, LocalDate.now(), "d"));
        assertTrue(ex.getMessage().contains("Member"));
    }

    @Test
    public void addTransactionThrowsWhenCategoryMissing() {
        when(memberRepo.findOne(1L)).thenReturn(Optional.of(new Member(1L, "A", "R", 0)));
        when(categoryRepo.findOne(99L)).thenReturn(Optional.empty());
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.addTransaction(1L, 99L, 1.0, LocalDate.now(), "d"));
        assertTrue(ex.getMessage().contains("Category"));
    }

    @Test
    public void addTransactionSaveCollisionThrows() {
        Member m = new Member(1L, "A", "R", 0);
        Category c = new Category(1L, "X", CategoryType.INCOME);
        when(memberRepo.findOne(1L)).thenReturn(Optional.of(m));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(c));
        Transaction existing = new Transaction(1L, 1L, 1L, 50.0, LocalDate.now(), "d");
        when(transactionRepo.save(any())).thenReturn(Optional.of(existing));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.addTransaction(1L, 1L, 50.0, LocalDate.now(), "d"));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(transactionRepo).save(any());
    }

    @Test
    public void updateTransactionValidatesMemberAndCategoryExist() {
        when(memberRepo.findOne(1L)).thenReturn(Optional.of(new Member(1L, "A", "R", 0)));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(new Category(1L, "X", CategoryType.EXPENSE)));
        when(transactionRepo.update(any())).thenReturn(Optional.empty());
        Transaction t = service.updateTransaction(1L, 1L, 1L, 10.0, LocalDate.now(), "d");
        assertEquals(1L, t.getId());
    }

    @Test
    public void monthlyReportAggregatesByCategoryType() {
        Member m = new Member(1L, "Alice", "R", 0);
        Category inc = new Category(1L, "Inc", CategoryType.INCOME);
        Category exp = new Category(2L, "Exp", CategoryType.EXPENSE);

        Transaction incomeTx = new Transaction(1L, 1L, 1L, 1000.0, LocalDate.of(2024,5,3), "inc");
        Transaction expenseTx = new Transaction(2L, 1L, 2L, 200.0, LocalDate.of(2024,5,4), "exp");

        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(incomeTx, expenseTx));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(inc));
        when(categoryRepo.findOne(2L)).thenReturn(Optional.of(exp));

        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        MemberBudgetReport r = reports.get(0);
        assertEquals(1000.0, r.getTotalIncome());
        assertEquals(200.0, r.getTotalExpense());
    }

    @Test
    public void monthlyReportSkipsTransactionsWithMissingCategory() {
        Member m = new Member(1L, "Alice", "R", 0);
        Transaction t = new Transaction(1L, 1L, 99L, 50.0, LocalDate.of(2024,5,3), "x");
        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(t));
        when(categoryRepo.findOne(99L)).thenReturn(Optional.empty());
        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        assertEquals(0.0, reports.get(0).getTotalIncome());
        assertEquals(0.0, reports.get(0).getTotalExpense());
    }

    @Test
    public void monthlyReportNullMonthThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.monthlyReport(null));
    }

    @Test
    public void monthlyReportMemberNoTransactions() {
        Member m = new Member(1L, "Bob", "R", 0);
        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of());
        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        assertEquals(0.0, reports.get(0).getTotalIncome());
        assertEquals(0.0, reports.get(0).getTotalExpense());
    }

    @Test
    public void monthlyReportMultiIteration() {
        Member m = new Member(1L, "Bob", "R", 0);
        Transaction t1 = new Transaction(1L, 99L, 1L, 10.0, LocalDate.of(2024,5,3), "x");
        Transaction t2 = new Transaction(2L, 1L, 1L, 20.0, LocalDate.of(2024,5,4), "y");
        Category cat = new Category(1L, "Inc", CategoryType.INCOME);
        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(t1, t2));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(cat));
        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        assertEquals(20.0, reports.get(0).getTotalIncome());
        assertEquals(0.0, reports.get(0).getTotalExpense());
    }

    @Test
    public void monthlyReportSkipsByMemberMismatch() {
        Member m = new Member(1L, "Bob", "R", 0);
        Transaction t = new Transaction(1L, 2L, 1L, 30.0, LocalDate.of(2024,5,3), "x");
        Category cat = new Category(1L, "Inc", CategoryType.INCOME);
        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(t));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(cat));
        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        assertEquals(0.0, reports.get(0).getTotalIncome());
        assertEquals(0.0, reports.get(0).getTotalExpense());
    }

    @Test
    public void monthlyReportSkipsByMonthMismatch() {
        Member m = new Member(1L, "Bob", "R", 0);
        Transaction t = new Transaction(1L, 1L, 1L, 30.0, LocalDate.of(2024,6,1), "x");
        Category cat = new Category(1L, "Inc", CategoryType.INCOME);

        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(transactionRepo.findAll()).thenReturn(List.of(t));
        when(categoryRepo.findOne(1L)).thenReturn(Optional.of(cat)); // safe to mock

        List<MemberBudgetReport> reports = service.monthlyReport(YearMonth.of(2024,5));
        assertEquals(1, reports.size());
        assertEquals(0.0, reports.get(0).getTotalIncome());
        assertEquals(0.0, reports.get(0).getTotalExpense());
    }
}   
