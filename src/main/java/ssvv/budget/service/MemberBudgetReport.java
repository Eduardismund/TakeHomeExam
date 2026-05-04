package ssvv.budget.service;

public class MemberBudgetReport {

    private final Long memberId;
    private final String memberName;
    private final double totalIncome;
    private final double totalExpense;

    public MemberBudgetReport(Long memberId, String memberName, double totalIncome, double totalExpense) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getBalance() {
        return totalIncome - totalExpense;
    }

    @Override
    public String toString() {
        return String.format("%s | income=%.2f | expense=%.2f | balance=%.2f",
                memberName, totalIncome, totalExpense, getBalance());
    }
}
