package ssvv.budget.repository;

import ssvv.budget.domain.Entity;
import ssvv.budget.validation.Validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepository<ID, T extends Entity<ID>> implements Repository<ID, T> {

    protected final Map<ID, T> storage = new HashMap<>();
    protected final Validator<T> validator;

    public InMemoryRepository(Validator<T> validator) {
        this.validator = validator;
    }

    @Override
    public Optional<T> findOne(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Iterable<T> findAll() {
        return storage.values();
    }

    @Override
    public Optional<T> save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validator.validate(entity);
        if (storage.containsKey(entity.getId())) {
            return Optional.of(entity);
        }
        storage.put(entity.getId(), entity);
        return Optional.empty();
    }

    @Override
    public Optional<T> delete(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return Optional.ofNullable(storage.remove(id));
    }

    @Override
    public Optional<T> update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validator.validate(entity);
        if (!storage.containsKey(entity.getId())) {
            return Optional.of(entity);
        }
        storage.put(entity.getId(), entity);
        return Optional.empty();
    }
}
