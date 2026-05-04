package ssvv.budget.domain;

import java.util.Objects;

public class Category extends Entity<Long> {

    private String name;
    private CategoryType type;

    public Category() {
        super();
    }

    public Category(Long id, String name, CategoryType type) {
        super(id);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(name, category.name) && type == category.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, type);
    }

    @Override
    public String toString() {
        return "Category{id=" + getId() +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
