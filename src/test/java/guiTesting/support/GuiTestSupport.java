package guiTesting.support;

import ssvv.budget.ui.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for Task 3 GUI / end-to-end tests.
 * Interacts with the Swing UI the way a user would: tabs, buttons, form fields.
 */
public final class GuiTestSupport {

    private GuiTestSupport() {}

    public static void enableSwingForTests() {
        System.setProperty("java.awt.headless", "false");
        UIManager.put("OptionPaneUI", "javax.swing.plaf.basic.BasicOptionPaneUI");
    }

    public static JTabbedPane tabs(MainFrame frame) {
        Container content = frame.getContentPane();
        if (content instanceof JTabbedPane tabs) {
            return tabs;
        }
        for (Component c : content.getComponents()) {
            if (c instanceof JTabbedPane tabs) {
                return tabs;
            }
        }
        throw new IllegalStateException("MainFrame has no JTabbedPane");
    }

    public static void selectTab(MainFrame frame, int index) throws Exception {
        SwingUtilities.invokeAndWait(() -> tabs(frame).setSelectedIndex(index));
    }

    public static MembersPanel membersPanel(MainFrame frame) throws Exception {
        selectTab(frame, 0);
        MembersPanel panel = (MembersPanel) tabs(frame).getComponentAt(0);
        SwingUtilities.invokeAndWait(panel::refresh);
        return panel;
    }

    public static CategoriesPanel categoriesPanel(MainFrame frame) throws Exception {
        selectTab(frame, 1);
        CategoriesPanel panel = (CategoriesPanel) tabs(frame).getComponentAt(1);
        SwingUtilities.invokeAndWait(panel::refresh);
        return panel;
    }

    public static TransactionsPanel transactionsPanel(MainFrame frame) throws Exception {
        selectTab(frame, 2);
        TransactionsPanel panel = (TransactionsPanel) tabs(frame).getComponentAt(2);
        SwingUtilities.invokeAndWait(panel::refresh);
        return panel;
    }

    public static ReportPanel reportPanel(MainFrame frame) throws Exception {
        selectTab(frame, 3);
        return (ReportPanel) tabs(frame).getComponentAt(3);
    }

    public static void clickButton(Container root, String label) throws Exception {
        SwingUtilities.invokeAndWait(() -> findButton(root, label).doClick());
    }

    public static JButton findButton(Container root, String label) {
        for (Component c : allComponents(root)) {
            if (c instanceof JButton b && label.equals(b.getText())) {
                return b;
            }
        }
        throw new IllegalArgumentException("Button not found: " + label);
    }

    public static List<Component> allComponents(Container root) {
        List<Component> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(Container root, List<Component> out) {
        for (Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof Container container) {
                collect(container, out);
            }
        }
    }

    /** Auto-clicks Yes/OK on the next modal dialog (runs on a background thread). */
    public static void autoAcceptNextDialog() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog d && d.isShowing()) {
                            SwingUtilities.invokeLater(() -> clickDialogAccept(d));
                            return;
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void clickDialogAccept(JDialog dialog) {
        for (Component c : allComponents(dialog.getContentPane())) {
            if (c instanceof JButton b) {
                String text = b.getText();
                if ("Yes".equals(text) || "OK".equals(text)) {
                    b.doClick();
                    return;
                }
            }
        }
        var rootPane = dialog.getRootPane();
        if (rootPane != null && rootPane.getDefaultButton() != null) {
            rootPane.getDefaultButton().doClick();
        } else {
            dialog.dispose();
        }
    }

    public static JTextField textField(Object panel, String fieldName) throws Exception {
        Field f = panel.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    public static JComboBox<?> comboBox(Object panel, String fieldName) throws Exception {
        Field f = panel.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JComboBox<?>) f.get(panel);
    }

    public static JTable table(Object panel) throws Exception {
        Field f = panel.getClass().getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    public static DefaultTableModel tableModel(Object panel) throws Exception {
        Field f = panel.getClass().getDeclaredField("tableModel");
        f.setAccessible(true);
        return (DefaultTableModel) f.get(panel);
    }

    public static void selectTableRow(Object panel, int row) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                table(panel).setRowSelectionInterval(row, row);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void selectComboItem(Object panel, String fieldName, Object item) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                comboBox(panel, fieldName).setSelectedItem(item);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void selectComboIndex(Object panel, String fieldName, int index) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                comboBox(panel, fieldName).setSelectedIndex(index);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void setMemberForm(MembersPanel panel, String name, String role, String income)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                textField(panel, "nameField").setText(name);
                textField(panel, "roleField").setText(role);
                textField(panel, "incomeField").setText(income);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void setCategoryForm(CategoriesPanel panel, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                textField(panel, "nameField").setText(name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void setTransactionForm(TransactionsPanel panel, String amount, String date, String desc)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                textField(panel, "amountField").setText(amount);
                textField(panel, "dateField").setText(date);
                textField(panel, "descField").setText(desc);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void setReportMonth(ReportPanel panel, String month) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                textField(panel, "monthField").setText(month);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
