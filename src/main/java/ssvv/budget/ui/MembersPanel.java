package ssvv.budget.ui;

import ssvv.budget.domain.Member;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.ValidationException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MembersPanel extends JPanel {

    private final BudgetService service;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField = new JTextField(15);
    private final JTextField roleField = new JTextField(15);
    private final JTextField incomeField = new JTextField(8);

    public MembersPanel(BudgetService service) {
        this.service = service;
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Role", "Monthly Income"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Name:"), g);
        g.gridx = 1; form.add(nameField, g);
        g.gridx = 2; form.add(new JLabel("Role:"), g);
        g.gridx = 3; form.add(roleField, g);
        g.gridx = 4; form.add(new JLabel("Income:"), g);
        g.gridx = 5; form.add(incomeField, g);

        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton refreshBtn = new JButton("Refresh");

        g.gridx = 0; g.gridy = 1; form.add(addBtn, g);
        g.gridx = 1; form.add(updateBtn, g);
        g.gridx = 2; form.add(deleteBtn, g);
        g.gridx = 3; form.add(refreshBtn, g);

        add(form, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        refreshBtn.addActionListener(e -> refresh());

        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        refresh();
    }

    public void refresh() {
        tableModel.setRowCount(0);
        List<Member> members = service.listMembers();
        members.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        for (Member m : members) {
            tableModel.addRow(new Object[]{m.getId(), m.getName(), m.getRole(), m.getMonthlyIncome()});
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        roleField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        incomeField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
    }

    private void onAdd() {
        try {
            double income = Double.parseDouble(incomeField.getText().trim());
            service.addMember(nameField.getText().trim(), roleField.getText().trim(), income);
            refresh();
            clearForm();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Income must be a number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Long id = (Long) tableModel.getValueAt(row, 0);
            double income = Double.parseDouble(incomeField.getText().trim());
            service.updateMember(id, nameField.getText().trim(), roleField.getText().trim(), income);
            refresh();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Income must be a number", "Error", JOptionPane.ERROR_MESSAGE);
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
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete member " + id + "? Their transactions will also be removed.",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            service.deleteMember(id);
            refresh();
            clearForm();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        nameField.setText("");
        roleField.setText("");
        incomeField.setText("");
    }
}
