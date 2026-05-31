package ssvv.budget.validation;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Member;
import static org.junit.jupiter.api.Assertions.*;

public class MemberValidatorTest {
    private final MemberValidator validator = new MemberValidator();

    @Test
    public void validMemberDoesNotThrow() {
        Member member = new Member(1L, "John Doe", "Developer", 5000);
        assertDoesNotThrow(() -> validator.validate(member));
    }

    @Test 
    public void nullMemberThrows(){
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be null"));
    }

    @Test 
    public void blankNameOrRoleThrows(){
        Member m1 = new Member(1L, "", "Developer", 5000);
        Member m2 = new Member(1L, "John Doe", "", 5000);
        assertThrows(ValidationException.class, () -> validator.validate(m1));
        assertThrows(ValidationException.class, () -> validator.validate(m2));
    }

    @Test 
    public void negativeMonthlyIncomeThrows(){
        Member m = new Member(1L, "John Doe", "Developer", -100);
        assertThrows(ValidationException.class, () -> validator.validate(m));
    }
}
