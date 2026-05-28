package blackBoxTesting.ui;

import org.junit.jupiter.api.*;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.ReportPanel;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReportPanelTest {

    private BudgetService service;
    private ReportPanel panel;
    private Member testMember;

    private void autoClickOkButton() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    for (java.awt.Window w : java.awt.Window.getWindows()) {
                        if (w instanceof JDialog d && w.isShowing()) {
                            SwingUtilities.invokeLater(() -> {
                                var rootPane = d.getRootPane();
                                if (rootPane != null && rootPane.getDefaultButton() != null) {
                                    rootPane.getDefaultButton().doClick();
                                } else {
                                    d.dispose();
                                }
                            });
                            return;
                        }
                    }
                    Thread.sleep(100);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private JTextField field() throws Exception {
        Field f = ReportPanel.class.getDeclaredField("monthField");
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private DefaultTableModel model() throws Exception {
        Field f = ReportPanel.class.getDeclaredField("tableModel");
        f.setAccessible(true);
        return (DefaultTableModel) f.get(panel);
    }

    private void clickGenerateReport() throws Exception {
        autoClickOkButton();
        Method m = ReportPanel.class.getDeclaredMethod("onGenerate");
        m.setAccessible(true);
        m.invoke(panel);
    }

    @BeforeAll
    static void configureHeadlessFriendlyModals() {
        System.setProperty("java.awt.headless", "false");
        UIManager.put("OptionPaneUI", "javax.swing.plaf.basic.BasicOptionPaneUI");
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );

        testMember = service.addMember("Bob", "Child", 0.0);
        Category incomeCat = service.addCategory("Allowance", CategoryType.INCOME);
        Category expenseCat = service.addCategory("Toys", CategoryType.EXPENSE);

        service.addTransaction(testMember.getId(), incomeCat.getId(), 100.0, LocalDate.parse("2026-05-01"), "Monthly allowance");
        service.addTransaction(testMember.getId(), expenseCat.getId(), 40.0, LocalDate.parse("2026-05-15"), "Action figure");

        service.addTransaction(testMember.getId(), incomeCat.getId(), 50.0, LocalDate.parse("2026-04-01"), "Old cash");

        SwingUtilities.invokeAndWait(() -> panel = new ReportPanel(service));
    }


    @Test
    void validMonth_reportGenerated() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("2026-05");
                clickGenerateReport();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals(testMember.getId(), model().getValueAt(0, 0));
        assertEquals("100.00", model().getValueAt(0, 2));
        assertEquals("40.00", model().getValueAt(0, 3));
        assertEquals("60.00", model().getValueAt(0, 4));
    }

    @Test
    void emptyRecordsMonth_emptyModelView() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("2025-01");
                clickGenerateReport();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("0.00", model().getValueAt(0, 2));
        assertEquals("0.00", model().getValueAt(0, 3));
    }

    @Test
    void malformedDateFormat_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("05-2026");
                clickGenerateReport();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void whitespaceMonthTrimming_doneSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("   2026-05   ");
                clickGenerateReport();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("60.00", model().getValueAt(0, 4));
    }
}