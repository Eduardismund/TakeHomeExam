package ssvv.budget.domain;

import java.util.Objects;

public class Member extends Entity<Long> {

    private String name;
    private String role;
    private double monthlyIncome;

    public Member() {
        super();
    }

    public Member(Long id, String name, String role, double monthlyIncome) {
        super(id);
        this.name = name;
        this.role = role;
        this.monthlyIncome = monthlyIncome;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        if (!super.equals(o)) return false;
        return Double.compare(member.monthlyIncome, monthlyIncome) == 0
                && Objects.equals(name, member.name)
                && Objects.equals(role, member.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, role, monthlyIncome);
    }

    @Override
    public String toString() {
        return "Member{id=" + getId() +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", monthlyIncome=" + monthlyIncome +
                '}';
    }
}
