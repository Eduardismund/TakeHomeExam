package guiTesting;

import guiTesting.support.GuiTestSupport;
import org.junit.jupiter.api.*;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.*;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static guiTesting.support.GuiTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end GUI tests that exercise full user journeys through {@link MainFrame},
 * spanning multiple tabs and use cases from the specification.
 */
class EndToEndUserJourneyTest {

    private BudgetService service;
    private MainFrame frame;

    @BeforeAll
    static void enableSwing() {
        GuiTestSupport.enableSwingForTests();
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
        SwingUtilities.invokeAndWait(() -> frame = new MainFrame(service));
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.dispose());
    }

    @Test
    @DisplayName("GUI-01: UC-1 — add member via Members tab")
    void addMemberThroughGui() throws Exception {
        MembersPanel panel = membersPanel(frame);
        setMemberForm(panel, "Alice", "Parent", "3500");
        clickButton(panel, "Add");

        assertEquals(1, tableModel(panel).getRowCount());
        assertEquals("Alice", tableModel(panel).getValueAt(0, 1));
        assertEquals(1, service.listMembers().size());
    }

    @Test
    @DisplayName("GUI-02: UC-2 — add category via Categories tab")
    void addCategoryThroughGui() throws Exception {
        CategoriesPanel panel = categoriesPanel(frame);
        setCategoryForm(panel, "Salary");
        selectComboItem(panel, "typeBox", CategoryType.INCOME);
        clickButton(panel, "Add");

        assertEquals(1, tableModel(panel).getRowCount());
        assertEquals("Salary", tableModel(panel).getValueAt(0, 1));
        assertEquals(CategoryType.INCOME, tableModel(panel).getValueAt(0, 2));
    }

    @Test
    @DisplayName("GUI-03: UC-3 — record transaction linking member, category, and amount")
    void addTransactionThroughGui() throws Exception {
        service.addMember("Bob", "Parent", 4000);
        service.addCategory("Groceries", CategoryType.EXPENSE);

        TransactionsPanel panel = transactionsPanel(frame);
        setTransactionForm(panel, "85.50", "2026-05-15", "Weekly shop");
        clickButton(panel, "Add Transaction");

        assertEquals(1, tableModel(panel).getRowCount());
        assertEquals("Bob", tableModel(panel).getValueAt(0, 1));
        assertEquals("Groceries (EXPENSE)", tableModel(panel).getValueAt(0, 2));
        assertEquals(85.50, tableModel(panel).getValueAt(0, 3));
    }

    @Test
    @DisplayName("GUI-04: UC-4 — generate monthly report with correct balances")
    void generateMonthlyReportThroughGui() throws Exception {
        var member = service.addMember("Carol", "Parent", 3000);
        var income = service.addCategory("Salary", CategoryType.INCOME);
        var expense = service.addCategory("Rent", CategoryType.EXPENSE);
        service.addTransaction(member.getId(), income.getId(), 3000, java.time.LocalDate.of(2026, 5, 1), "Pay");
        service.addTransaction(member.getId(), expense.getId(), 1200, java.time.LocalDate.of(2026, 5, 5), "May rent");

        ReportPanel panel = reportPanel(frame);
        setReportMonth(panel, "2026-05");
        clickButton(panel, "Generate Report");

        DefaultTableModel model = tableModel(panel);
        assertEquals(1, model.getRowCount());
        assertEquals("Carol", model.getValueAt(0, 1));
        assertEquals("3000.00", model.getValueAt(0, 2));
        assertEquals("1200.00", model.getValueAt(0, 3));
        assertEquals("1800.00", model.getValueAt(0, 4));
    }

    @Test
    @DisplayName("GUI-05: UC-5 — update selected member from the table")
    void updateMemberThroughGui() throws Exception {
        service.addMember("Dave", "Child", 0);

        MembersPanel panel = membersPanel(frame);
        selectTableRow(panel, 0);
        setMemberForm(panel, "Dave Updated", "Teen", "200");
        clickButton(panel, "Update Selected");

        assertEquals("Dave Updated", service.listMembers().get(0).getName());
        assertEquals("Teen", service.listMembers().get(0).getRole());
        assertEquals(200.0, service.listMembers().get(0).getMonthlyIncome());
    }

    @Test
    @DisplayName("GUI-06: UC-6 — delete member removes row and cascades transactions")
    void deleteMemberThroughGui() throws Exception {
        var member = service.addMember("Eve", "Parent", 5000);
        var cat = service.addCategory("Food", CategoryType.EXPENSE);
        service.addTransaction(member.getId(), cat.getId(), 50, java.time.LocalDate.now(), "Lunch");

        MembersPanel panel = membersPanel(frame);
        selectTableRow(panel, 0);
        autoAcceptNextDialog();
        clickButton(panel, "Delete Selected");

        Thread.sleep(200);
        assertTrue(service.listMembers().isEmpty());
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    @DisplayName("GUI-07: full workflow — member → category → transaction → report")
    void completeBudgetWorkflowThroughGui() throws Exception {
        // Step 1: add member
        MembersPanel members = membersPanel(frame);
        setMemberForm(members, "Frank", "Parent", "4500");
        clickButton(members, "Add");
        assertEquals(1, service.listMembers().size());

        // Step 2: add income and expense categories
        CategoriesPanel categories = categoriesPanel(frame);
        setCategoryForm(categories, "Salary");
        selectComboItem(categories, "typeBox", CategoryType.INCOME);
        clickButton(categories, "Add");

        setCategoryForm(categories, "Utilities");
        selectComboItem(categories, "typeBox", CategoryType.EXPENSE);
        clickButton(categories, "Add");
        assertEquals(2, service.listCategories().size());

        // Step 3: add transactions (tab refresh loads new member/categories into combos)
        TransactionsPanel transactions = transactionsPanel(frame);
        assertEquals(1, comboBox(transactions, "memberBox").getItemCount());
        assertEquals(2, comboBox(transactions, "categoryBox").getItemCount());

        setTransactionForm(transactions, "4500", "2026-05-01", "Monthly salary");
        clickButton(transactions, "Add Transaction");

        selectComboIndex(transactions, "categoryBox", 1);
        setTransactionForm(transactions, "150", "2026-05-10", "Electric bill");
        clickButton(transactions, "Add Transaction");

        assertEquals(2, service.listTransactions().size());

        // Step 4: verify report
        ReportPanel report = reportPanel(frame);
        setReportMonth(report, "2026-05");
        clickButton(report, "Generate Report");

        DefaultTableModel reportModel = tableModel(report);
        assertEquals(1, reportModel.getRowCount());
        assertEquals("4500.00", reportModel.getValueAt(0, 2));
        assertEquals("150.00", reportModel.getValueAt(0, 3));
        assertEquals("4350.00", reportModel.getValueAt(0, 4));
    }
}
