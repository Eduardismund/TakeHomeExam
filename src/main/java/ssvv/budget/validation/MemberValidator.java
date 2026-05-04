package ssvv.budget.validation;

import ssvv.budget.domain.Member;

public class MemberValidator implements Validator<Member> {

    @Override
    public void validate(Member member) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        if (member == null) {
            throw new ValidationException("Member cannot be null!");
        }
        if (member.getName() == null || member.getName().isBlank()) {
            errors.append("Name cannot be empty! ");
        }
        if (member.getRole() == null || member.getRole().isBlank()) {
            errors.append("Role cannot be empty! ");
        }
        if (member.getMonthlyIncome() < 0) {
            errors.append("Monthly income cannot be negative! ");
        }
        if (errors.length() > 0) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}
