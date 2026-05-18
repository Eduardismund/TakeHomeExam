import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
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

public class ReportServiceTest {
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
    }

    @Test
    void testMonthlyReport_DecisionTableCombinations() {
        // decision table testing
        Member primaryMember = budgetService.addMember("John", "Father", 5000.0);
        Member secondaryMember = budgetService.addMember("Jane", "Mother", 4000.0);

        Category salary = budgetService.addCategory("Work Income", CategoryType.INCOME);
        Category food = budgetService.addCategory("Food Expense", CategoryType.EXPENSE);

        YearMonth targetMonth = YearMonth.of(2026, 5);

        // case 1: perfect condition matches (belongs to John, Target Month, Category Exists)
        budgetService.addTransaction(primaryMember.getId(), salary.getId(), 1000.0, LocalDate.of(2026, 5, 10), "Salary");
        budgetService.addTransaction(primaryMember.getId(), food.getId(), 200.0, LocalDate.of(2026, 5, 15), "Groceries");

        // case 2: wrong Member identity (belongs to Jane, target Month, Category Exists)
        budgetService.addTransaction(secondaryMember.getId(), food.getId(), 150.0, LocalDate.of(2026, 5, 12), "Jane Food");

        // case 3: Wrong Calendar Date frame (Belongs to John, next Month, Category Exists)
        assertThrows(ValidationException.class, () -> {
            budgetService.addTransaction(primaryMember.getId(), food.getId(), 300.0, LocalDate.of(2026, 6, 1), "Future Food");
        }, "Date cannot be in the future!");

        // execute calculations targeting 2026-05
        List<BudgetService.MemberBudgetReport> reports = budgetService.monthlyReport(targetMonth);

        // verify isolation boundaries
        assertNotNull(reports);
        assertEquals(2, reports.size(), "Should have a generated report line entry for every member registered");

        // locate John's mapped calculation structure
        BudgetService.MemberBudgetReport johnsReport = reports.stream()
                .filter(r -> r.getMemberId().equals(primaryMember.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(1000.0, johnsReport.getTotalIncome(), "Should map income transaction from Case 1 correctly");
        assertEquals(200.0, johnsReport.getTotalExpense(), "Should aggregate expense from Case 1 but filter out Case 3");
        assertEquals(800.0, johnsReport.getBalance());

        // locate Jane's mapped calculation structure
        BudgetService.MemberBudgetReport janesReport = reports.stream()
                .filter(r -> r.getMemberId().equals(secondaryMember.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(0.0, janesReport.getTotalIncome());
        assertEquals(150.0, janesReport.getTotalExpense(), "Case 2 transaction must be safely isolated to Jane's metrics");
    }

    @Test
    void testMonthlyReport_NullMonthInput_ThrowsIllegalArgumentException() {
        // error guessing
        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.monthlyReport(null);
        });
    }

    @Test
    void testMonthlyReport_EmptySystemState_ReturnsEmptyList() {
        // state transition testing
        List<BudgetService.MemberBudgetReport> reports = budgetService.monthlyReport(YearMonth.now());
        assertNotNull(reports);
        assertTrue(reports.isEmpty());
    }
}