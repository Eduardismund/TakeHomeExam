package ssvv.budget;

import ssvv.budget.repository.CategoryFileRepository;
import ssvv.budget.repository.MemberFileRepository;
import ssvv.budget.repository.Repository;
import ssvv.budget.repository.TransactionFileRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.ui.MainFrame;
import ssvv.budget.validation.CategoryValidator;
import ssvv.budget.validation.MemberValidator;
import ssvv.budget.validation.TransactionValidator;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        Repository<Long, ssvv.budget.domain.Member> memberRepo =
                new MemberFileRepository(new MemberValidator(), "data/members.txt");
        Repository<Long, ssvv.budget.domain.Category> categoryRepo =
                new CategoryFileRepository(new CategoryValidator(), "data/categories.txt");
        Repository<Long, ssvv.budget.domain.Transaction> transactionRepo =
                new TransactionFileRepository(new TransactionValidator(), "data/transactions.txt");

        BudgetService service = new BudgetService(memberRepo, categoryRepo, transactionRepo);

        SwingUtilities.invokeLater(() -> new MainFrame(service).setVisible(true));
    }
}
