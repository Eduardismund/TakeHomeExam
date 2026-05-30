package blackBoxTesting.ui;

import org.junit.jupiter.api.*;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.MainFrame;
import ssvv.budget.ui.CategoriesPanel;
import ssvv.budget.ui.TransactionsPanel;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class MainFrameTest {

    private BudgetService service;
    private MainFrame mainFrame;

    private JTabbedPane getTabbedPane() throws Exception {
        Container contentPane = mainFrame.getContentPane();

        if (contentPane instanceof JTabbedPane) {
            return (JTabbedPane) contentPane;
        }

        for (Component comp : contentPane.getComponents()) {
            if (comp instanceof JTabbedPane) {
                return (JTabbedPane) comp;
            }
        }

        Field[] fields = MainFrame.class.getDeclaredFields();
        for (Field f : fields) {
            if (f.getType().equals(JTabbedPane.class)) {
                f.setAccessible(true);
                return (JTabbedPane) f.get(mainFrame);
            }
        }
        throw new NoSuchFieldException("Could not locate the JTabbedPane instance within the application window structure.");
    }

    @BeforeAll
    static void configureHeadlessFriendlyModals() {
        System.setProperty("java.awt.headless", "false");
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
        SwingUtilities.invokeAndWait(() -> mainFrame = new MainFrame(service));
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> mainFrame.dispose());
    }

    @Test
    void tabLayout_initializesSmoothly() throws Exception {
        JTabbedPane tabs = getTabbedPane();

        assertEquals(4, tabs.getTabCount());
        assertEquals("Members", tabs.getTitleAt(0));
        assertEquals("Categories", tabs.getTitleAt(1));
        assertEquals("Transactions (add/edit)", tabs.getTitleAt(2));
        assertEquals("Monthly Report", tabs.getTitleAt(3));
    }

    @Test
    void tabNavigation_triggersRefresh() throws Exception {
        JTabbedPane tabs = getTabbedPane();

        service.addMember("Charlie", "Grandparent", 3000.0);
        service.addCategory("Dividends", CategoryType.INCOME);

        SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));

        CategoriesPanel catPanel = (CategoriesPanel) tabs.getComponentAt(1);
        Field modelField = CategoriesPanel.class.getDeclaredField("tableModel");
        modelField.setAccessible(true);
        DefaultTableModel catModel = (DefaultTableModel) modelField.get(catPanel);

        assertEquals(1, catModel.getRowCount());

        SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(2));
        TransactionsPanel txPanel = (TransactionsPanel) tabs.getComponentAt(2);

        Field comboField = TransactionsPanel.class.getDeclaredField("memberBox");
        comboField.setAccessible(true);
        JComboBox<?> memberBox = (JComboBox<?>) comboField.get(txPanel);

        assertEquals(1, memberBox.getItemCount());
    }
}