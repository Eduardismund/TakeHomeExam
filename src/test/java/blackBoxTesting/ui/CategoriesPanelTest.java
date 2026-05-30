package blackBoxTesting.ui;

import org.junit.jupiter.api.*;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.CategoriesPanel;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriesPanelTest {

    private BudgetService service;
    private CategoriesPanel panel;

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
        Field f = CategoriesPanel.class.getDeclaredField("nameField");
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private JComboBox<?> comboBox() throws Exception {
        Field f = CategoriesPanel.class.getDeclaredField("typeBox");
        f.setAccessible(true);
        return (JComboBox<?>) f.get(panel);
    }

    private JTable table() throws Exception {
        Field f = CategoriesPanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    private DefaultTableModel model() throws Exception {
        Field f = CategoriesPanel.class.getDeclaredField("tableModel");
        f.setAccessible(true);
        return (DefaultTableModel) f.get(panel);
    }

    private void clickAdd() throws Exception {
        autoClickOkButton();
        Method m = CategoriesPanel.class.getDeclaredMethod("onAdd");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickUpdate() throws Exception {
        autoClickOkButton();
        Method m = CategoriesPanel.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickDelete() throws Exception {
        autoClickOkButton();
        Method m = CategoriesPanel.class.getDeclaredMethod("onDelete");
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
        SwingUtilities.invokeAndWait(() -> panel = new CategoriesPanel(service));
    }

    @Test
    void addValidCategory_addsSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("Salary");
                comboBox().setSelectedItem(CategoryType.INCOME);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("Salary", model().getValueAt(0, 1));
        assertEquals(CategoryType.INCOME, model().getValueAt(0, 2));
        assertEquals("", field().getText()); // Form gets cleared
    }

    @Test
    void emptyCategoryName_throwsError() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("");
                comboBox().setSelectedItem(CategoryType.EXPENSE);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
        assertTrue(service.listCategories().isEmpty());
    }

    @Test
    void whitespaceOnlyName_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("     ");
                comboBox().setSelectedItem(CategoryType.EXPENSE);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void singleCharacterName_addsSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("A");
                comboBox().setSelectedItem(CategoryType.INCOME);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("A", service.listCategories().get(0).getName());
    }

    @Test
    void categoryAdded_rowSelectionPopulatesForm() throws Exception {
        service.addCategory("Rent", CategoryType.EXPENSE);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals("Rent", field().getText());
        assertEquals(CategoryType.EXPENSE, comboBox().getSelectedItem());
    }

    @Test
    void updateNoRowSelected_executionPathCancelled() throws Exception {
        service.addCategory("Utilities", CategoryType.EXPENSE);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().clearSelection();
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals("Utilities", service.listCategories().get(0).getName());
    }

    @Test
    void selectAndUpdateCategory_updatesSuccessfully() throws Exception {
        service.addCategory("Bonus", CategoryType.INCOME);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
                field().setText("Holiday Bonus");
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        Category cat = service.listCategories().get(0);
        assertEquals("Holiday Bonus", cat.getName());
    }

    @Test
    void selectAndDeleteCategory_deletesSuccessfully() throws Exception {
        service.addCategory("Investments", CategoryType.INCOME);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
                clickDelete();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
        assertTrue(service.listCategories().isEmpty());
    }

    @Test
    void paddedWhitespaceTrimming_isDoneSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("   Healthcare   ");
                comboBox().setSelectedItem(CategoryType.EXPENSE);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("Healthcare", service.listCategories().get(0).getName());
    }

    @Test
    void nullTypeHandling_validationExceptionThrown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field().setText("NullTypeTest");
                comboBox().setSelectedItem(null);
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }
}