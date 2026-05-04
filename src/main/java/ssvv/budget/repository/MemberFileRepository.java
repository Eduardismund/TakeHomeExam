package ssvv.budget.repository;

import ssvv.budget.domain.Member;
import ssvv.budget.validation.Validator;

public class MemberFileRepository extends AbstractFileRepository<Long, Member> {

    private static final String SEP = ";";

    public MemberFileRepository(Validator<Member> validator, String filePath) {
        super(validator, filePath);
    }

    @Override
    protected Member parseEntity(String line) {
        String[] parts = line.split(SEP, -1);
        Long id = Long.parseLong(parts[0]);
        String name = parts[1];
        String role = parts[2];
        double income = Double.parseDouble(parts[3]);
        return new Member(id, name, role, income);
    }

    @Override
    protected String formatEntity(Member m) {
        return m.getId() + SEP + m.getName() + SEP + m.getRole() + SEP + m.getMonthlyIncome();
    }
}
