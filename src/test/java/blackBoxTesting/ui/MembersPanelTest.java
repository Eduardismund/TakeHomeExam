package blackBoxTesting.ui;

import org.junit.jupiter.api.*;
import ssvv.budget.domain.Member;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.MembersPanel;
import ssvv.budget.validation.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class MembersPanelTest {

    private BudgetService service;
    private MembersPanel panel;

    // helper methods for panels
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
        Field f = MembersPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private JTable table() throws Exception {
        Field f = MembersPanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    private DefaultTableModel model() throws Exception {
        Field f = MembersPanel.class.getDeclaredField("tableModel");
        f.setAccessible(true);
        return (DefaultTableModel) f.get(panel);
    }

    private void clickAdd() throws Exception {
        autoClickOkButton();
        Method m = MembersPanel.class.getDeclaredMethod("onAdd");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickUpdate() throws Exception {
        autoClickOkButton();
        Method m = MembersPanel.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private void clickDelete() throws Exception {
        autoClickOkButton();
        Method m = MembersPanel.class.getDeclaredMethod("onDelete");
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
        SwingUtilities.invokeAndWait(() -> panel = new MembersPanel(service));
    }

    @Test
    void addValid_addsSuccesfully_clearsFields() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("2000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals("Alice", model().getValueAt(0, 1));

        assertEquals("", field("nameField").getText());
        assertEquals("", field("incomeField").getText());
    }

    @Test
    void nonNumericIncome_valueNotAdded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("abc");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(0, model().getRowCount());
        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    void blankName_valueNotAdded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("");
                field("roleField").setText("Parent");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void blankRole_valueNotAdded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void whitespaceOnlyName_valueNotAdded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("   ");
                field("roleField").setText("Parent");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void zeroIncome_addsSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Bob");
                field("roleField").setText("Child");
                field("incomeField").setText("0");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
        assertEquals(0.0, service.listMembers().get(0).getMonthlyIncome());
    }

    @Test
    void justBelowZeroIncome_throwsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Bob");
                field("roleField").setText("Child");
                field("incomeField").setText("-0.001");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(0, model().getRowCount());
    }

    @Test
    void justAboveZeroIncome_addsSuccessfully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Bob");
                field("roleField").setText("Child");
                field("incomeField").setText("0.001");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, model().getRowCount());
    }

    @Test
    @DisplayName("DT-M1: valid member + valid category + valid income → added")
    void allValid() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, service.listMembers().size());
    }

    @Test
    @DisplayName("DT-M2: invalid name + valid rest → not added")
    void dt_invalidName() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("");
                field("roleField").setText("Parent");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    @DisplayName("DT-M3: valid name + invalid role + valid income → not added")
    void dt_invalidRole() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    @DisplayName("DT-M4: valid name + valid role + non-numeric income → not added")
    void dt_invalidIncome() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("xyz");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertTrue(service.listMembers().isEmpty());
    }

    @Test
    void updateNoSelection_noDataChanged() throws Exception {
        service.addMember("Charlie", "Son", 500.0);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().clearSelection();
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        Member m = service.listMembers().get(0);
        assertEquals("Charlie", m.getName());
        assertEquals(500.0, m.getMonthlyIncome());
    }

    @Test
    void deleteNoSelection_noDataChanged() throws Exception {
        service.addMember("Charlie", "Son", 500.0);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().clearSelection();
                clickDelete();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(1, service.listMembers().size());
    }

    @Test
    void rowClick_formFilled() throws Exception {
        service.addMember("Alice", "Parent", 1500.0);
        SwingUtilities.invokeAndWait(() -> panel.refresh());

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("Alice",  field("nameField").getText());
        assertEquals("Parent", field("roleField").getText());
        assertEquals("1500.0", field("incomeField").getText());
    }

    @Test
    void addThenUpdate_newValuesReflected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("1000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
                field("nameField").setText("Alice Updated");
                field("roleField").setText("Guardian");
                field("incomeField").setText("2000");
                clickUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        Member m = service.listMembers().get(0);
        assertEquals("Alice Updated", m.getName());
        assertEquals("Guardian", m.getRole());
        assertEquals(2000.0, m.getMonthlyIncome());
    }

    @Test
    void refresh_sortedById() throws Exception {
        service.addMember("Zachary", "Son",         10.0);
        service.addMember("Aaron",   "Grandfather", 5000.0);
        service.addMember("Bob",     "Uncle",       1200.0);

        SwingUtilities.invokeAndWait(() -> panel.refresh());

        assertEquals(3,  model().getRowCount());
        assertEquals(1L, model().getValueAt(0, 0));
        assertEquals(2L, model().getValueAt(1, 0));
        assertEquals(3L, model().getValueAt(2, 0));
    }

    @Test
    void addMultipleMembers_allAppearInTable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("2000");
                clickAdd();

                field("nameField").setText("Bob");
                field("roleField").setText("Child");
                field("incomeField").setText("0");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        assertEquals(2, model().getRowCount());
        assertEquals(2, service.listMembers().size());
    }

    @Test
    void addAndSelect_formPopulated() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                field("nameField").setText("Alice");
                field("roleField").setText("Parent");
                field("incomeField").setText("3000");
                clickAdd();
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        SwingUtilities.invokeAndWait(() -> {
            try {
                table().setRowSelectionInterval(0, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("Alice",  field("nameField").getText());
        assertEquals("Parent", field("roleField").getText());
    }
}