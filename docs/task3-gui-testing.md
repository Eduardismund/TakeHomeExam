# Task 3 — GUI Testing

**Project:** Family Budget Manager  
**Application type:** Java 17 desktop GUI (Swing)  
**Tester:** _(your name)_  
**Date:** May 2026

---

## 1. Purpose and scope

This document describes the **GUI testing** activity for Task 3 of the SSVV take-home assignment. GUI testing verifies that the application behaves correctly when accessed through its graphical user interface — the same way an end user would interact with it.

### In scope

- Main window layout and tab navigation
- CRUD operations on Members, Categories, and Transactions through the UI
- Functionality A: adding a transaction that links all three entities
- Functionality B: generating a monthly budget report
- Validation error dialogs and confirmation dialogs
- Cross-panel behaviour (e.g. new members appearing in the Transactions dropdown after tab switch)

### Out of scope

- Service-layer unit tests (Task 2 — white box / black box)
- Integration tests that bypass the UI (Task 2)
- Performance, security, and multi-user testing
- Web/browser testing (this is a Swing desktop app, not a web app)

---

## 2. Testing approach

| Aspect | Choice |
|--------|--------|
| **Technique** | Automated GUI testing |
| **Tooling** | JUnit 5 + Java Swing (`SwingUtilities`, component interaction) |
| **Test level** | End-to-end through `MainFrame`, spanning multiple tabs |
| **Test data** | In-memory repositories (isolated, repeatable) |
| **Entry point** | `src/test/java/guiTesting/` |

### Why automated GUI tests?

Manual clicking alone would be exploratory testing. Task 3 requires **test code** that can be re-run after every change to confirm the UI still works. The tests simulate user actions (select tab → fill form → click button → verify table) and assert on visible outcomes.

### Difference from Task 2 UI tests

Task 2 black-box tests in `blackBoxTesting/ui/` focus on **individual panels** and boundary/decision-table cases. Task 3 GUI tests in `guiTesting/` focus on **full user journeys** through the complete application window, mapped to the use cases in the specification.

---

## 3. Test environment

| Item | Value |
|------|-------|
| OS | macOS / Linux / Windows |
| JDK | 17 |
| Build tool | Gradle |
| Run application | `./gradlew run` |
| Run GUI tests | `./gradlew test --tests "guiTesting.*"` |
| Run all tests | `./gradlew test` |

**Note:** GUI tests require a display (or compatible headless Swing setup). They set `java.awt.headless=false` so `JOptionPane` dialogs can be handled programmatically.

---

## 4. Test cases and results

| ID | Use case | Description | Expected result | Status |
|----|----------|-------------|-----------------|--------|
| GUI-01 | UC-1 | Add member via Members tab | Row appears in table; fields cleared | Pass |
| GUI-02 | UC-2 | Add category via Categories tab | Row appears with correct name and type | Pass |
| GUI-03 | UC-3 | Add transaction | Row shows member, category, amount | Pass |
| GUI-04 | UC-4 | Generate monthly report | Correct income, expense, balance | Pass |
| GUI-05 | UC-5 | Update selected member | Updated values persisted | Pass |
| GUI-06 | UC-6 | Delete member | Member and their transactions removed | Pass |
| GUI-07 | UC-1…4 | Full workflow across all tabs | End-to-end budget flow succeeds | Pass |
| GUI-08 | — | Main window tab layout | Four tabs with correct titles | Pass |
| GUI-09 | — | Tab switch refreshes combos | New member/category in dropdowns | Pass |
| GUI-10 | UC-1 | Invalid member (blank name) | Error dialog; no row added | Pass |
| GUI-11 | UC-3 | Transaction without prerequisites | Info dialog; nothing saved | Pass |
| GUI-12 | UC-6 | Delete category with transactions | Deletion blocked | Pass |
| GUI-13 | UC-4 | Invalid report month format | Error dialog; table empty | Pass |
| GUI-14 | UC-5 | Row selection fills form | Form populated from selected row | Pass |

---

## 5. Detailed test procedures

### GUI-01 — Add member (UC-1)

1. Launch app (test creates `MainFrame`).
2. Open **Members** tab.
3. Enter Name = `Alice`, Role = `Parent`, Income = `3500`.
4. Click **Add**.
5. **Verify:** table has 1 row; name column shows `Alice`.

### GUI-07 — Full budget workflow

1. Add member `Frank` on Members tab.
2. Switch to Categories tab; add `Salary` (INCOME) and `Utilities` (EXPENSE).
3. Switch to Transactions tab; verify dropdowns contain the new data.
4. Add income transaction (4500) and expense transaction (150) for May 2026.
5. Switch to Monthly Report tab; enter `2026-05`; click **Generate Report**.
6. **Verify:** income = 4500.00, expense = 150.00, balance = 4350.00.

### GUI-12 — Delete category with transactions

1. Pre-condition: member, category, and one transaction exist.
2. Open Categories tab; select the category row.
3. Click **Delete Selected**; confirm dialog.
4. **Verify:** category still exists (business rule: cannot delete while in use).

---

## 6. Test code structure

```
src/test/java/guiTesting/
├── support/
│   └── GuiTestSupport.java      # shared helpers (tabs, buttons, dialogs)
├── EndToEndUserJourneyTest.java # GUI-01 … GUI-07 (use-case flows)
└── GuiNavigationAndValidationTest.java  # GUI-08 … GUI-14 (navigation & errors)
```

### How dialogs are handled

`JOptionPane` confirmation and error dialogs block the EDT. Tests start a background thread (`autoAcceptNextDialog`) that finds the visible `JDialog` and clicks **Yes** or **OK** before the user action that triggers the dialog.

---

## 7. Defects found

| ID | Severity | Summary | Status |
|----|----------|---------|--------|
| — | — | No defects found during automated GUI testing | — |

_(Update this table if exploratory or manual sessions reveal issues.)_

---

## 8. Conclusion

Fourteen automated GUI test cases were executed against the Family Budget Manager Swing application. All tests passed. The UI correctly supports the six use cases from the specification, enforces validation rules with error dialogs, and keeps dependent panels in sync when switching tabs.

The test suite is repeatable via `./gradlew test --tests "guiTesting.*"` and can be extended as new UI features are added.

---

## 9. How to run

```bash
# GUI tests only (Task 3)
./gradlew test --tests "guiTesting.*"

# All project tests (Task 2 + Task 3)
./gradlew test
```
