package ssvv.budget.validation;

public interface Validator<T> {
    void validate(T entity) throws ValidationException;
}
