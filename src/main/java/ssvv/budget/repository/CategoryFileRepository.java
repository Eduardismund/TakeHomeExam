package ssvv.budget.repository;

import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.validation.Validator;

public class CategoryFileRepository extends AbstractFileRepository<Long, Category> {

    private static final String SEP = ";";

    public CategoryFileRepository(Validator<Category> validator, String filePath) {
        super(validator, filePath);
    }

    @Override
    protected Category parseEntity(String line) {
        String[] parts = line.split(SEP, -1);
        Long id = Long.parseLong(parts[0]);
        String name = parts[1];
        CategoryType type = CategoryType.valueOf(parts[2]);
        return new Category(id, name, type);
    }

    @Override
    protected String formatEntity(Category c) {
        return c.getId() + SEP + c.getName() + SEP + c.getType().name();
    }
}
