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
 * GUI tests focused on navigation, validation feedback, and cross-panel behaviour.
 */
class GuiNavigationAndValidationTest {

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
    @DisplayName("GUI-08: main window shows all four tabs")
    void mainWindowHasExpectedTabs() throws Exception {
        JTabbedPane tabs = tabs(frame);
        assertEquals(4, tabs.getTabCount());
        assertEquals("Members", tabs.getTitleAt(0));
        assertEquals("Categories", tabs.getTitleAt(1));
        assertEquals("Transactions (add/edit)", tabs.getTitleAt(2));
        assertEquals("Monthly Report", tabs.getTitleAt(3));
    }

    @Test
    @DisplayName("GUI-09: switching to Transactions tab refreshes member/category dropdowns")
    void tabSwitchRefreshesTransactionCombos() throws Exception {
        service.addMember("Grace", "Parent", 2000);
        service.addCategory("Bonus", CategoryType.INCOME);

        TransactionsPanel panel = transactionsPanel(frame);
        assertEquals(1, comboBox(panel, "memberBox").getItemCount());
        assertEquals(1, comboBox(panel, "categoryBox").getItemCount());
    }

    @Test
    @DisplayName("GUI-10: invalid member input shows error and keeps table empty")
    void invalidMemberInputRejectedByGui() throws Exception {
        MembersPanel panel = membersPanel(frame);
        setMemberForm(panel, "", "Parent", "1000");
        autoAcceptNextDialog();
        clickButton(panel, "Add");

        Thread.sleep(150);
        assertEquals(0, tableModel(panel).getRowCount());
        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    @DisplayName("GUI-11: transaction without member/category shows info dialog")
    void transactionWithoutPrerequisitesBlocked() throws Exception {
        TransactionsPanel panel = transactionsPanel(frame);
        setTransactionForm(panel, "100", "2026-05-01", "Test");
        autoAcceptNextDialog();
        clickButton(panel, "Add Transaction");

        Thread.sleep(150);
        assertEquals(0, service.listTransactions().size());
    }

    @Test
    @DisplayName("GUI-12: category with linked transactions cannot be deleted")
    void deleteCategoryWithTransactionsBlocked() throws Exception {
        var member = service.addMember("Helen", "Parent", 3000);
        var cat = service.addCategory("Insurance", CategoryType.EXPENSE);
        service.addTransaction(member.getId(), cat.getId(), 200, java.time.LocalDate.now(), "Policy");

        CategoriesPanel panel = categoriesPanel(frame);
        selectTableRow(panel, 0);
        autoAcceptNextDialog();
        clickButton(panel, "Delete Selected");

        Thread.sleep(200);
        assertEquals(1, service.listCategories().size());
    }

    @Test
    @DisplayName("GUI-13: invalid report month format shows error dialog")
    void invalidReportMonthRejected() throws Exception {
        service.addMember("Ivan", "Parent", 1000);

        ReportPanel panel = reportPanel(frame);
        setReportMonth(panel, "not-a-month");
        autoAcceptNextDialog();
        clickButton(panel, "Generate Report");

        Thread.sleep(150);
        DefaultTableModel model = tableModel(panel);
        assertEquals(0, model.getRowCount());
    }

    @Test
    @DisplayName("GUI-14: selecting a table row fills the member edit form")
    void rowSelectionPopulatesMemberForm() throws Exception {
        service.addMember("Jane", "Parent", 2800);

        MembersPanel panel = membersPanel(frame);
        selectTableRow(panel, 0);

        assertEquals("Jane", textField(panel, "nameField").getText());
        assertEquals("Parent", textField(panel, "roleField").getText());
        assertEquals("2800.0", textField(panel, "incomeField").getText());
    }
}
