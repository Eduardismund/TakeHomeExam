package ssvv.budget.repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ssvv.budget.domain.Member;
import ssvv.budget.validation.Validator;
import ssvv.budget.validation.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class MemberFileRepositoryTest {
    @TempDir
    Path tempDir;

    static class NoopValidator implements Validator<Member> {
        @Override
        public void validate(Member entity) throws ValidationException {
        }
    }

    @Test 
    public void parseEntityParsesValidFormat() {
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), tempDir.resolve("dummy.txt").toString());
        String line = "1;Alice;Dev;2000.0";
        Member m = repo.parseEntity(line);
        assertEquals(1L, m.getId());
        assertEquals("Alice", m.getName());
        assertEquals("Dev", m.getRole());
        assertEquals(2000.0, m.getMonthlyIncome());
    }

    @Test 
    public void parseEntityWithBlankFieldsWorks() {
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), tempDir.resolve("dummy.txt").toString());
        String line = "2;Bob;;1500.0";
        Member m = repo.parseEntity(line);
        assertEquals("", m.getRole());
    }

    @Test
    public void parseEntityThrowsOnMalformedNumber() {
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        assertThrows(Exception.class, () -> repo.parseEntity("notanumber;Name;Role;1000"));
    }

    @Test
    public void formatEntityFormatsCorrectly() {
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        Member m = new Member(1L, "Alice", "Dev", 2000.0);
        String formatted = repo.formatEntity(m);
        assertEquals("1;Alice;Dev;2000.0", formatted);
    }

    @Test
    public void roundTripParseAndFormatPreservesData() {
        MemberFileRepository repo = new MemberFileRepository(new NoopValidator(), 
            tempDir.resolve("dummy.txt").toString());
        String original = "99;TestName;TestRole;1234.56";
        Member m = repo.parseEntity(original);
        String formatted = repo.formatEntity(m);
        assertEquals(original, formatted);
    }
}
