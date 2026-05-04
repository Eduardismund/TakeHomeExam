package ssvv.budget.ui;

import ssvv.budget.service.BudgetService;
import ssvv.budget.service.MemberBudgetReport;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Functionality 2: report on a specific information.
 * Generates per-member income/expense/balance for a chosen month.
 */
public class ReportPanel extends JPanel {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BudgetService service;
    private final JTextField monthField = new JTextField(7);
    private final DefaultTableModel tableModel;

    public ReportPanel(BudgetService service) {
        this.service = service;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Month (YYYY-MM):"));
        monthField.setText(YearMonth.now().format(MONTH_FMT));
        top.add(monthField);
        JButton generateBtn = new JButton("Generate Report");
        top.add(generateBtn);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"Member ID", "Member", "Total Income", "Total Expense", "Balance"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        generateBtn.addActionListener(e -> onGenerate());
    }

    private void onGenerate() {
        try {
            YearMonth ym = YearMonth.parse(monthField.getText().trim(), MONTH_FMT);
            List<MemberBudgetReport> reports = service.monthlyReport(ym);
            tableModel.setRowCount(0);
            for (MemberBudgetReport r : reports) {
                tableModel.addRow(new Object[]{
                        r.getMemberId(), r.getMemberName(),
                        String.format("%.2f", r.getTotalIncome()),
                        String.format("%.2f", r.getTotalExpense()),
                        String.format("%.2f", r.getBalance())
                });
            }
            if (reports.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No members found.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (DateTimeParseException dpe) {
            JOptionPane.showMessageDialog(this, "Month must be in YYYY-MM format", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
