package ssvv.budget.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ssvv.budget.domain.Member;
import ssvv.budget.validation.Validator;
import ssvv.budget.validation.ValidationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class AbstractFileRepositoryTest {

    @TempDir
    Path tempDir;

    static class NoopValidator implements Validator<Member> {
        @Override
        public void validate(Member entity) throws ValidationException {
        }
    }

    @Test
    public void loadFromFileOnConstructorWhenFileExists() throws IOException {
        Path file = tempDir.resolve("members.txt");
        Files.writeString(file, "1;Alice;Dev;2000.0\n2;Bob;Chef;1500.0\n");
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), file.toString());
        List<Member> all = new ArrayList<>();
        repo.findAll().forEach(all::add);
        assertEquals(2, all.size());
        assertTrue(repo.findOne(1L).isPresent());
        assertEquals("Alice", repo.findOne(1L).get().getName());
    }

    @Test
    public void loadFromFileIgnoresBlankLines() throws IOException {
        Path file = tempDir.resolve("members.txt");
        Files.writeString(file, "1;Alice;Dev;2000.0\n\n\n2;Bob;Chef;1500.0\n");
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), file.toString());
        List<Member> all = new ArrayList<>();
        repo.findAll().forEach(all::add);
        assertEquals(2, all.size());
    }

    @Test
    public void loadFromFileSwallowsIOExceptionWhenFileDoesNotExist() throws IOException {
        Path file = tempDir.resolve("nonexistent.txt");
        assertDoesNotThrow(() -> new MemberFileRepository(new NoopValidator(), file.toString()));
    }

    @Test
    public void saveWritesEntityToFile() throws IOException {
        Path file = tempDir.resolve("members.txt");
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), file.toString());
        Member m = new Member(1L, "Alice", "Dev", 2000.0);
        repo.save(m);
        String content = Files.readString(file);
        assertTrue(content.contains("1;Alice;Dev;2000.0"));
    }

    @Test
    public void updateWritesChangesToFile() throws IOException {
        Path file = tempDir.resolve("members.txt");
        Files.writeString(file, "1;Alice;Dev;2000.0\n");
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), file.toString());
        Member m = new Member(1L, "Alice", "Manager", 3000.0);
        repo.update(m);
        String content = Files.readString(file);
        assertTrue(content.contains("Manager"));
        assertTrue(content.contains("3000.0"));
    }

    @Test
    public void deleteRemovesFromFileAndMemory() throws IOException {
        Path file = tempDir.resolve("members.txt");
        Files.writeString(file, "1;Alice;Dev;2000.0\n2;Bob;Chef;1500.0\n");
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), file.toString());
        repo.delete(1L);
        String content = Files.readString(file);
        assertFalse(content.contains("Alice"));
        assertTrue(content.contains("Bob"));
    }
}
