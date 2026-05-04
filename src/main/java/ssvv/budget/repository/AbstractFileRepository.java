package ssvv.budget.repository;

import ssvv.budget.domain.Entity;
import ssvv.budget.validation.Validator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public abstract class AbstractFileRepository<ID, T extends Entity<ID>> extends InMemoryRepository<ID, T> {

    protected final String filePath;

    public AbstractFileRepository(Validator<T> validator, String filePath) {
        super(validator);
        this.filePath = filePath;
        loadFromFile();
    }

    protected abstract T parseEntity(String line);

    protected abstract String formatEntity(T entity);

    protected void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                T entity = parseEntity(line);
                storage.put(entity.getId(), entity);
            }
        } catch (IOException e) {
            // file may not exist on first run - that's fine
        }
    }

    protected void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (T entity : storage.values()) {
                writer.write(formatEntity(entity));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file " + filePath, e);
        }
    }

    @Override
    public Optional<T> save(T entity) {
        Optional<T> result = super.save(entity);
        if (result.isEmpty()) {
            writeToFile();
        }
        return result;
    }

    @Override
    public Optional<T> delete(ID id) {
        Optional<T> result = super.delete(id);
        if (result.isPresent()) {
            writeToFile();
        }
        return result;
    }

    @Override
    public Optional<T> update(T entity) {
        Optional<T> result = super.update(entity);
        if (result.isEmpty()) {
            writeToFile();
        }
        return result;
    }
}
