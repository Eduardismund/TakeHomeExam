# Family Budget Manager

SSVV Take-Home Task 1. Java 17 + Swing, MVC.

## Entities
- **Member** (id, name, role, monthlyIncome)
- **Category** (id, name, type: INCOME/EXPENSE)
- **Transaction** (id, memberId, categoryId, amount, date, description)

## Functionalities
1. **Add Transaction** — links Member + Category + Transaction.
2. **Monthly Report** — income / expense / balance per member for a given month.

## Run
```bash
./gradlew run
```
