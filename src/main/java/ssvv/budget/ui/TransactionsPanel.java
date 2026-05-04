package ssvv.budget.ui;

import ssvv.budget.domain.Category;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.ValidationException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Functionality 1: links Member + Category + Transaction.
 * Also supports listing/updating/deleting transactions (Transaction CRUD).
 */
public class TransactionsPanel extends JPanel {

    private final BudgetService service;
    private final DefaultTableModel tableModel;
    private final JTable table;

    private final JComboBox<Member> memberBox = new JComboBox<>();
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final JTextField dateField = new JTextField(10);
    private final JTextField descField = new JTextField(20);

    public TransactionsPanel(BudgetService service) {
        this.service = service;
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Member", "Category", "Amount", "Date", "Description"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Member:"), g);
        g.gridx = 1; form.add(memberBox, g);
        g.gridx = 2; form.add(new JLabel("Category:"), g);
        g.gridx = 3; form.add(categoryBox, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Amount:"), g);
        g.gridx = 1; form.add(amountField, g);
        g.gridx = 2; form.add(new JLabel("Date (YYYY-MM-DD):"), g);
        g.gridx = 3; form.add(dateField, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Description:"), g);
        g.gridx = 1; g.gridwidth = 3; form.add(descField, g);
        g.gridwidth = 1;

        JButton addBtn = new JButton("Add Transaction");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton refreshBtn = new JButton("Refresh");

        g.gridx = 0; g.gridy = 3; form.add(addBtn, g);
        g.gridx = 1; form.add(updateBtn, g);
        g.gridx = 2; form.add(deleteBtn, g);
        g.gridx = 3; form.add(refreshBtn, g);

        add(form, BorderLayout.SOUTH);

        memberBox.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Member m) l.setText(m.getId() + " - " + m.getName());
                return l;
            }
        });
        categoryBox.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Category c) l.setText(c.getId() + " - " + c.getName() + " (" + c.getType() + ")");
                return l;
            }
        });

        dateField.setText(LocalDate.now().toString());

        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        refreshBtn.addActionListener(e -> refresh());

        refresh();
    }

    public void refresh() {
        // refresh combos from service (so newly-added members/categories show up)
        List<Member> members = service.listMembers();
        members.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        memberBox.removeAllItems();
        for (Member m : members) memberBox.addItem(m);

        List<Category> cats = service.listCategories();
        cats.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        categoryBox.removeAllItems();
        for (Category c : cats) categoryBox.addItem(c);

        // refresh table
        tableModel.setRowCount(0);
        List<Transaction> txs = service.listTransactions();
        txs.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        for (Transaction t : txs) {
            String memberName = members.stream()
                    .filter(m -> m.getId().equals(t.getMemberId()))
                    .map(Member::getName).findFirst().orElse("?");
            String catName = cats.stream()
                    .filter(c -> c.getId().equals(t.getCategoryId()))
                    .map(c -> c.getName() + " (" + c.getType() + ")").findFirst().orElse("?");
            tableModel.addRow(new Object[]{
                    t.getId(), memberName, catName, t.getAmount(), t.getDate(), t.getDescription()
            });
        }
    }

    private void onAdd() {
        Member m = (Member) memberBox.getSelectedItem();
        Category c = (Category) categoryBox.getSelectedItem();
        if (m == null || c == null) {
            JOptionPane.showMessageDialog(this,
                    "You need at least one member and one category first.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            service.addTransaction(m.getId(), c.getId(), amount, date, descField.getText().trim());
            refresh();
            amountField.setText("");
            descField.setText("");
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Amount must be a number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException dpe) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ValidationException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Member m = (Member) memberBox.getSelectedItem();
        Category c = (Category) categoryBox.getSelectedItem();
        if (m == null || c == null) return;
        try {
            Long id = (Long) tableModel.getValueAt(row, 0);
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            service.updateTransaction(id, m.getId(), c.getId(), amount, date, descField.getText().trim());
            refresh();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Amount must be a number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException dpe) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Long id = (Long) tableModel.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Delete transaction " + id + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            service.deleteTransaction(id);
            refresh();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
