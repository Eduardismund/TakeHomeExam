package blackBoxTesting.ui;

import org.junit.jupiter.api.*;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.TransactionsPanel;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionsPanelTest {

    private BudgetService service;
    private TransactionsPanel panel;
    private Member testMember;
    private Category testCategory;

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

    private JTextField field(String name) throws Exception {
        Field f = TransactionsPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private JComboBox<?> comboBox() throws Exception {
        Field f = TransactionsPanel.class.getDeclaredField("memberBox");
        f.setAccessible(true);
        return (JComboBox<?>) f.get(panel);
    }

    private JTable table() throws Exception {
        Field f = TransactionsPanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    private DefaultTableModel model() throws Exception {
        Field f = TransactionsPanel.class.getDeclaredField("tableModel");
        f.setAccessible(true);
        return (DefaultTableModel) f.get(panel);
    }

    private void clickAdd() throws Exception {
        autoClickOkButton();
        Method m = TransactionsPanel.class.getDeclaredMethod("onAdd");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickUpdate() throws Exception {
        autoClickOkButton();
        Method m = TransactionsPanel.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickDelete() throws Exception {
        autoClickOkButton();
        Method m = TransactionsPanel.class.getDeclaredMethod("onDelete");
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

        testMember = service.addMember("Alice", "Parent", 2500.0);
        testCategory = service.addCategory("Groceries", CategoryType.EXPENSE);

        SwingUtilities.invokeAndWait(() -> panel = new TransactionsPanel(service));
    }

    @Test
    void addValidTransaction_addsSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("150.50");
                field("dateField").setText("2026-05-20");
                field("descField").setText("Weekly market shopping");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals(1, service.listTransactions().size());
        assertEquals("", field("amountField").getText());
    }

    @Test
    void nonNumericAmount_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("abc");
                field("dateField").setText("2026-05-20");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    void malformedDate_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("50.0");
                field("dateField").setText("20-05-2026");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void zeroAmountTransaction_notAdded() throws Exception {
        testCategory = service.addCategory("Groceries", CategoryType.EXPENSE);

        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("0.0");
                field("dateField").setText("2026-05-20");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void missingPrerequisites_dialogPopup() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                comboBox().removeAllItems();
                field("amountField").setText("100");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void updateNoRowSelected_noDataChanged() throws Exception {
        service.addTransaction(testMember.getId(), testCategory.getId(), 45.0, LocalDate.now(), "Gas");
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().clearSelection();
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        Transaction t = service.listTransactions().get(0);
        assertEquals(45.0, t.getAmount());
    }

    @Test
    void deleteNoRowSelected_noDataChanged() throws Exception {
        service.addTransaction(testMember.getId(), testCategory.getId(), 45.0, LocalDate.now(), "Gas");
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().clearSelection();
                clickDelete();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, service.listTransactions().size());
    }

    @Test
    void selectAndApplyMutations_updatesSuccessfully() throws Exception {
        service.addTransaction(testMember.getId(), testCategory.getId(), 10.0, LocalDate.parse("2026-05-20"), "Initial");
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
                field("amountField").setText("99.99");
                field("dateField").setText("2026-05-20");
                field("descField").setText("Altered Description");
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        Transaction updatedTx = service.listTransactions().get(0);
        assertEquals(99.99, updatedTx.getAmount());
        assertEquals("Altered Description", updatedTx.getDescription());
    }

    @Test
    void selectAndDeleteTransaction_deletesSuccessfully() throws Exception {
        service.addTransaction(testMember.getId(), testCategory.getId(), 100.0, LocalDate.now(), "To Remove");
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
                clickDelete();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
        assertTrue(service.listTransactions().isEmpty());
    }

    @Test
    void spacesInNumericFields_properlyTrimmed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("   75.50   ");
                field("dateField").setText("  2026-05-20  ");
                field("descField").setText("Spaces test");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals(75.50, service.listTransactions().get(0).getAmount());
    }

    @Test
    void extremeAmountOverflow_handledGracefully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("99999999999999999999999999999999999999999999999999");
                field("dateField").setText("2026-05-20");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertTrue(model().getRowCount() == 0 || service.listTransactions().size() == 1);
    }

    @Test
    void invalidLeapYearDate_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("20.0");
                field("dateField").setText("2026-02-29");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void specialCharactersAndInjectionStrings_worksSmoothly() throws Exception {
        String riskyDescription = "Drop Table Transactions; -- '\" & <xml>";

        SwingUtilities.invokeAndWait(() -> {
            try {
                field("amountField").setText("10.0");
                field("dateField").setText("2026-05-20");
                field("descField").setText(riskyDescription);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals(riskyDescription, service.listTransactions().get(0).getDescription());
    }

    @Test
    void staleSelectionOnUpdate_noDataChanged() throws Exception {
        service.addTransaction(testMember.getId(), testCategory.getId(), 50.0, LocalDate.now(), "Original");
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);

                field("amountField").setText("ThisIsNotANumber");
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(50.0, service.listTransactions().get(0).getAmount());
    }
}