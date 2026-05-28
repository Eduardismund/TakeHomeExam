package blackBoxTesting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssvv.budget.domain.Category;
import ssvv.budget.domain.CategoryType;
import ssvv.budget.domain.Member;
import ssvv.budget.domain.Transaction;
import ssvv.budget.repository.InMemoryRepository;
import ssvv.budget.service.BudgetService;
import ssvv.budget.validation.CategoryValidator;
import ssvv.budget.validation.MemberValidator;
import ssvv.budget.validation.TransactionValidator;
import ssvv.budget.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {
    private BudgetService budgetService;

    @BeforeEach
    void setUp(){
        budgetService = new BudgetService(
                new InMemoryRepository<>(new MemberValidator()),
                new InMemoryRepository<>(new CategoryValidator()),
                new InMemoryRepository<>(new TransactionValidator())
        );
    }

    // addMember tests
    @Test
    void addMember_validData_addsSuccessfully(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        assertNotNull(m);
        assertEquals("Alice", m.getName());
        assertEquals("Parent", m.getRole());
        assertEquals(2000.0, m.getMonthlyIncome());
    }

    @Test
    void addMember_emptyName_throwsException(){
        assertThrows(ValidationException.class, () -> budgetService.addMember("", "Parent", 2000.0), "Name cannot be empty! ");
    }

    @Test
    void addMember_emptyRole_throwsException(){
        assertThrows(ValidationException.class, () -> budgetService.addMember("Alice", "", 2000.0), "Role cannot be empty! ");
    }

    @Test
    void addMember_nullName_throwsException(){
        assertThrows(ValidationException.class, () -> budgetService.addMember(null, "Parent", 2000.0), "Name cannot be empty! ");
    }

    @Test
    void addMember_nullRole_throwsException(){
        assertThrows(ValidationException.class, () -> budgetService.addMember("Alice", null, 2000.0), "Role cannot be empty! ");
    }

    @Test
    void addMember_zeroIncome_addsSuccessfully(){

        Member m = budgetService.addMember("Alice", "Parent", 0.0);
        assertNotNull(m);
        assertEquals(0.0, m.getMonthlyIncome());
    }

    @Test
    void addMember_negativeIncome_throwsException(){
        assertThrows(ValidationException.class, () -> budgetService.addMember("Alice", "Parent", -0.01), "Monthly income cannot be negative! ");
    }

    @Test
    void addMember_stateTransition_membersIncrease() {
        List<Member> initialList = budgetService.listMembers();
        int initialSize = initialList.size();

        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        List<Member> updatedList = budgetService.listMembers();
        assertEquals(initialSize + 1, updatedList.size(), "Repository size should increase by 1");

        boolean found = updatedList.stream().anyMatch(member -> member.getId().equals(m.getId()));
        assertTrue(found, "The new member state must persist in the repository");
    }

    @Test
    void addMember_specialCharactersName_addsSuccessfully() {
        String weirdName = "J0hn-D0e_#1! 🎉";
        Member m = budgetService.addMember(weirdName, "Parent", 2000.0);

        assertNotNull(m);
        assertEquals(weirdName, m.getName());
    }

    @Test
    void addMember_duplicateMembers_addsSuccessfully() {
        budgetService.addMember("Duplicate", "Role", 500.0);
        assertNotNull(budgetService.addMember("Duplicate", "Role", 500.0));
    }

    // deleteMember tests
    @Test
    void deleteMember_deletesExistingMember_deletesSuccessfully(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Member deletedMember = budgetService.deleteMember(m.getId());
        assertNotNull(deletedMember);
        assertEquals(m.getId(), deletedMember.getId());
        assertFalse(budgetService.listMembers().stream().anyMatch(member -> member.getId().equals(m.getId())));
    }

    @Test
    void deleteMember_usesInvalidId_throwsException(){
        Long nonExistentId = 999L;

        assertThrows(IllegalArgumentException.class, () -> budgetService.deleteMember(nonExistentId), "Member 999 not found");
    }

    @Test
    void deleteMember_hasTransactions_deletesAll() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Category c = budgetService.addCategory("Utilities", CategoryType.EXPENSE);

        budgetService.addTransaction(m.getId(), c.getId(), 50.0, LocalDate.now(), "Electric bill");
        budgetService.addTransaction(m.getId(), c.getId(), 30.0, LocalDate.now(), "Water bill");

        assertEquals(2, budgetService.listTransactions().size(), "Pre-condition: 2 transactions should exist");

        budgetService.deleteMember(m.getId());

        List<Transaction> remainingTransactions = budgetService.listTransactions();
        assertTrue(remainingTransactions.isEmpty(), "Post-condition: All associated transactions must be cascaded and deleted");
    }

    @Test
    void deleteMember_deletesTwice_throwsException(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        assertNotNull(budgetService.deleteMember(m.getId()));

        assertThrows(IllegalArgumentException.class, () -> budgetService.deleteMember(m.getId()), "Member" + m.getId() + " not found");
    }

    // listMembers tests
    @Test
    void listMembers_addsAndDeletes_sizeUpdates(){
        List<Member> emptyList = budgetService.listMembers();
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size(), "Initial list should be empty");

        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        List<Member> populatedList = budgetService.listMembers();
        assertEquals(1, populatedList.size(), "List should contain exactly 1 member");
        assertEquals(m.getId(), populatedList.get(0).getId());

        budgetService.deleteMember(m.getId());

        assertEquals(0, budgetService.listMembers().size(), "List should return to 0 after deletion");
    }

    @Test
    void listMembers_clearsReturnedList_remainsTheSame(){
        budgetService.addMember("Alice", "Parent", 2000.0);
        List<Member> list = budgetService.listMembers();

        try {
            list.clear();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertEquals(1, budgetService.listMembers().size(), "Modifying returned list shouldn't break internal state");
    }

    // updateMember tests
    @Test
    void updateMember_validData_updatesSuccessfully(){
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);
        Member updated = budgetService.updateMember(m.getId(), "Charles", "Grandfather", 3500.0);

        assertNotNull(updated);
        assertEquals("Charles", updated.getName());
        assertEquals("Grandfather", updated.getRole());
        assertEquals(3500.0, updated.getMonthlyIncome());

        Member fetched = budgetService.listMembers().get(0);
        assertEquals("Charles", fetched.getName());
    }

    @Test
    void updateMember_usesInvalidId_throwsException() {
        Long nonExistentId = 999L;

        assertThrows(IllegalArgumentException.class, () -> budgetService.updateMember(nonExistentId, "NoOne", "None", 1000.0), "Member" + nonExistentId + " not found");
    }

    @Test
    void updateMember_negativeIncome_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        assertThrows(ValidationException.class, () -> budgetService.updateMember(m.getId(), "Alice", "Parent", -0.01), "Monthly income cannot be negative! ");
    }

    @Test
    void updateMember_blankName_throwsException() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        assertThrows(ValidationException.class, () -> budgetService.updateMember(m.getId(), "   ", "Parent", 2000.0), "Name cannot be empty! ");
    }

    @Test
    void updateMember_updateExistingMember_noAdditionalMembersAdded() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        Member updated = budgetService.updateMember(m.getId(), "Alice", "Parent", 2000.0);

        assertNotNull(updated);
        assertEquals(1, budgetService.listMembers().size(), "Should still remain a single record in system");
    }

    @Test
    void updateMember_nullName_throwsExceptions() {
        Member m = budgetService.addMember("Alice", "Parent", 2000.0);

        assertThrows(ValidationException.class, () -> budgetService.updateMember(m.getId(), null, "Parent", 2000.0),"Name cannot be empty! ");
    }

}
