package ssvv.budget.repository;

import ssvv.budget.domain.Entity;

import java.util.Optional;

public interface Repository<ID, T extends Entity<ID>> {

    Optional<T> findOne(ID id);

    Iterable<T> findAll();

    Optional<T> save(T entity);

    Optional<T> delete(ID id);

    Optional<T> update(T entity);
}
