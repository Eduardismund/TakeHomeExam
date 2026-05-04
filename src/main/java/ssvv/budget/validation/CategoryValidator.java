package ssvv.budget.validation;

import ssvv.budget.domain.Category;

public class CategoryValidator implements Validator<Category> {

    @Override
    public void validate(Category category) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        if (category == null) {
            throw new ValidationException("Category cannot be null!");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            errors.append("Category name cannot be empty! ");
        }
        if (category.getType() == null) {
            errors.append("Category type must be INCOME or EXPENSE! ");
        }
        if (errors.length() > 0) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}
