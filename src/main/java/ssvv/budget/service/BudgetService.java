package ssvv.budget.service;

import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.Repository;
import ssvv.budget.validation.ValidationException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class BudgetService {

    private final Repository<Long, Member> memberRepo;
    private final Repository<Long, Category> categoryRepo;
    private final Repository<Long, Transaction> transactionRepo;

    private final AtomicLong memberSeq;
    private final AtomicLong categorySeq;
    private final AtomicLong transactionSeq;

    public BudgetService(Repository<Long, Member> memberRepo,
                         Repository<Long, Category> categoryRepo,
                         Repository<Long, Transaction> transactionRepo) {
        this.memberRepo = memberRepo;
        this.categoryRepo = categoryRepo;
        this.transactionRepo = transactionRepo;
        this.memberSeq = new AtomicLong(maxId(memberRepo));
        this.categorySeq = new AtomicLong(maxId(categoryRepo));
        this.transactionSeq = new AtomicLong(maxId(transactionRepo));
    }

    private static <T extends ssvv.budget.domain.Entity<Long>> long maxId(Repository<Long, T> repo) {
        long max = 0L;
        for (T e : repo.findAll()) {
            if (e.getId() != null && e.getId() > max) max = e.getId();
        }
        return max;
    }

    // ---------- Member CRUD ----------
    public Member addMember(String name, String role, double monthlyIncome) {
        Member m = new Member(memberSeq.incrementAndGet(), name, role, monthlyIncome);
        Optional<Member> result = memberRepo.save(m);
        if (result.isPresent()) {
            throw new IllegalStateException("Member with id " + m.getId() + " already exists");
        }
        return m;
    }

    public List<Member> listMembers() {
        List<Member> out = new ArrayList<>();
        memberRepo.findAll().forEach(out::add);
        return out;
    }

    public Member updateMember(Long id, String name, String role, double monthlyIncome) {
        Member m = new Member(id, name, role, monthlyIncome);
        Optional<Member> result = memberRepo.update(m);
        if (result.isPresent()) {
            throw new IllegalArgumentException("Member " + id + " not found");
        }
        return m;
    }

    public Member deleteMember(Long id) {
        Optional<Member> result = memberRepo.delete(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Member " + id + " not found");
        }
        // cascade: remove transactions for that member
        List<Long> toRemove = new ArrayList<>();
        for (Transaction t : transactionRepo.findAll()) {
            if (id.equals(t.getMemberId())) {
                toRemove.add(t.getId());
            }
        }
        toRemove.forEach(transactionRepo::delete);
        return result.get();
    }

    // ---------- Category CRUD ----------
    public Category addCategory(String name, CategoryType type) {
        Category c = new Category(categorySeq.incrementAndGet(), name, type);
        Optional<Category> result = categoryRepo.save(c);
        if (result.isPresent()) {
            throw new IllegalStateException("Category with id " + c.getId() + " already exists");
        }
        return c;
    }

    public List<Category> listCategories() {
        List<Category> out = new ArrayList<>();
        categoryRepo.findAll().forEach(out::add);
        return out;
    }

    public Category updateCategory(Long id, String name, CategoryType type) {
        Category c = new Category(id, name, type);
        Optional<Category> result = categoryRepo.update(c);
        if (result.isPresent()) {
            throw new IllegalArgumentException("Category " + id + " not found");
        }
        return c;
    }

    public Category deleteCategory(Long id) {
        // safety: cannot delete category if any transactions reference it
        for (Transaction t : transactionRepo.findAll()) {
            if (id.equals(t.getCategoryId())) {
                throw new IllegalStateException(
                        "Cannot delete category " + id + ": transactions exist for it");
            }
        }
        Optional<Category> result = categoryRepo.delete(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Category " + id + " not found");
        }
        return result.get();
    }

    // ---------- Transaction CRUD ----------
    public List<Transaction> listTransactions() {
        List<Transaction> out = new ArrayList<>();
        transactionRepo.findAll().forEach(out::add);
        return out;
    }

    public Transaction updateTransaction(Long id, Long memberId, Long categoryId,
                                         double amount, LocalDate date, String description) {
        if (memberRepo.findOne(memberId).isEmpty()) {
            throw new ValidationException("Member " + memberId + " does not exist");
        }
        if (categoryRepo.findOne(categoryId).isEmpty()) {
            throw new ValidationException("Category " + categoryId + " does not exist");
        }
        Transaction t = new Transaction(id, memberId, categoryId, amount, date, description);
        Optional<Transaction> result = transactionRepo.update(t);
        if (result.isPresent()) {
            throw new IllegalArgumentException("Transaction " + id + " not found");
        }
        return t;
    }

    public Transaction deleteTransaction(Long id) {
        Optional<Transaction> result = transactionRepo.delete(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Transaction " + id + " not found");
        }
        return result.get();
    }

    // ---------- Functionality 1: links all 3 entities ----------
    /**
     * Add a budget entry: validates that the Member exists, the Category exists,
     * and creates a new Transaction connecting them. Touches all three entities.
     */
    public Transaction addTransaction(Long memberId, Long categoryId, double amount,
                                      LocalDate date, String description) {
        if (memberRepo.findOne(memberId).isEmpty()) {
            throw new ValidationException("Member " + memberId + " does not exist");
        }
        if (categoryRepo.findOne(categoryId).isEmpty()) {
            throw new ValidationException("Category " + categoryId + " does not exist");
        }
        Transaction t = new Transaction(
                transactionSeq.incrementAndGet(),
                memberId, categoryId, amount, date, description);
        Optional<Transaction> result = transactionRepo.save(t);
        if (result.isPresent()) {
            throw new IllegalStateException("Transaction with id " + t.getId() + " already exists");
        }
        return t;
    }

    // ---------- Functionality 2: report ----------
    /**
     * Generate a per-member income/expense/balance report for a given month.
     * Aggregates Transactions by category type (INCOME vs EXPENSE) for each Member.
     */
    public List<MemberBudgetReport> monthlyReport(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("month cannot be null");
        }
        List<MemberBudgetReport> reports = new ArrayList<>();
        for (Member m : memberRepo.findAll()) {
            double income = 0.0;
            double expense = 0.0;
            for (Transaction t : transactionRepo.findAll()) {
                if (!m.getId().equals(t.getMemberId())) continue;
                if (!YearMonth.from(t.getDate()).equals(month)) continue;
                Optional<Category> cat = categoryRepo.findOne(t.getCategoryId());
                if (cat.isEmpty()) continue;
                if (cat.get().getType() == CategoryType.INCOME) {
                    income += t.getAmount();
                } else {
                    expense += t.getAmount();
                }
            }
            reports.add(new MemberBudgetReport(m.getId(), m.getName(), income, expense));
        }
        return reports;
    }
}
