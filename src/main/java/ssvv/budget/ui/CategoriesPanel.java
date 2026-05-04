package ssvv.budget.ui;

import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.ValidationException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CategoriesPanel extends JPanel {

    private final BudgetService service;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField = new JTextField(15);
    private final JComboBox<CategoryType> typeBox = new JComboBox<>(CategoryType.values());

    public CategoriesPanel(BudgetService service) {
        this.service = service;
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Type"}, 0) {
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
        g.gridx = 2; form.add(new JLabel("Type:"), g);
        g.gridx = 3; form.add(typeBox, g);

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
        List<Category> cats = service.listCategories();
        cats.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        for (Category c : cats) {
            tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getType()});
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        typeBox.setSelectedItem(tableModel.getValueAt(row, 2));
    }

    private void onAdd() {
        try {
            service.addCategory(nameField.getText().trim(), (CategoryType) typeBox.getSelectedItem());
            refresh();
            nameField.setText("");
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
            service.updateCategory(id, nameField.getText().trim(), (CategoryType) typeBox.getSelectedItem());
            refresh();
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
                "Delete category " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            service.deleteCategory(id);
            refresh();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
