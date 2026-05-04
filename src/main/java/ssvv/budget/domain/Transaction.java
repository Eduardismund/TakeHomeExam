package ssvv.budget.domain;

import java.time.LocalDate;
import java.util.Objects;

public class Transaction extends Entity<Long> {

    private Long memberId;
    private Long categoryId;
    private double amount;
    private LocalDate date;
    private String description;

    public Transaction() {
        super();
    }

    public Transaction(Long id, Long memberId, Long categoryId, double amount, LocalDate date, String description) {
        super(id);
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        if (!super.equals(o)) return false;
        return Double.compare(that.amount, amount) == 0
                && Objects.equals(memberId, that.memberId)
                && Objects.equals(categoryId, that.categoryId)
                && Objects.equals(date, that.date)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), memberId, categoryId, amount, date, description);
    }

    @Override
    public String toString() {
        return "Transaction{id=" + getId() +
                ", memberId=" + memberId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", date=" + date +
                ", description='" + description + '\'' +
                '}';
    }
}
