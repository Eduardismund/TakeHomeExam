package ssvv.budget.ui;

import ssvv.budget.service.BudgetService;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame(BudgetService service) {
        super("Family Budget Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        MembersPanel membersPanel = new MembersPanel(service);
        CategoriesPanel categoriesPanel = new CategoriesPanel(service);
        TransactionsPanel transactionsPanel = new TransactionsPanel(service);
        ReportPanel reportPanel = new ReportPanel(service);

        tabs.addTab("Members", membersPanel);
        tabs.addTab("Categories", categoriesPanel);
        tabs.addTab("Transactions (add/edit)", transactionsPanel);
        tabs.addTab("Monthly Report", reportPanel);

        // refresh dependent panels when their tab is selected
        ChangeListener tabListener = e -> {
            int i = tabs.getSelectedIndex();
            if (i == 0) membersPanel.refresh();
            else if (i == 1) categoriesPanel.refresh();
            else if (i == 2) transactionsPanel.refresh();
        };
        tabs.addChangeListener(tabListener);

        setContentPane(tabs);
    }
}
