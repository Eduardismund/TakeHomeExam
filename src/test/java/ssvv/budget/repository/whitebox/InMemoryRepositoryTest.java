package ssvv.budget.repository;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Member;
import ssvv.budget.validation.Validator;
import ssvv.budget.validation.ValidationException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class InMemoryRepositoryTest {
    static class NoopMemberValidator implements Validator<Member> {
        @Override
        public void validate(Member entity) throws ValidationException {
            // No validation logic for testing purposes
        }
    }

    @Test 
    public void findOneNullIdThrows() {
        InMemoryRepository<Long, Member> repo = new InMemoryRepository<>(new NoopMemberValidator());
        assertThrows(IllegalArgumentException.class, () -> repo.findOne(null));
    }

    @Test
    public void saveNullEntityThrows() {
        InMemoryRepository<Long, Member> repo = new InMemoryRepository<>(new NoopMemberValidator());
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
    }

    @Test 
    public void saveExistingReturnsOptionalOfEntity() {
        InMemoryRepository<Long, Member> repo = new InMemoryRepository<>(new NoopMemberValidator());
        Member m = new Member(1L, "A", "R", 0);
        assertTrue(repo.save(m).isEmpty());
        assertTrue(repo.save(m).isPresent());
    }

    @Test 
    public void deleteRemovesEntity(){
        InMemoryRepository<Long, Member> repo = new InMemoryRepository<>(new NoopMemberValidator());
        Member m = new Member(1L, "A", "R", 0);
        repo.save(m);
        Optional<Member> deleted = repo.delete(1L);
        assertTrue(deleted.isPresent());
        assertTrue(repo.findOne(1L).isEmpty());
    }

    @Test
    public void updateNonExistingReturnsOptional(){
        InMemoryRepository<Long, Member> repo = new InMemoryRepository<>(new NoopMemberValidator());
        Member m = new Member(99L, "A", "R", 0);
        assertTrue(repo.update(m).isPresent());
    }
}
