# Budget Planner Implementation Plan

Implement a comprehensive Budget Planner feature that allows users to plan budgets for their trips, including multi-currency support, group budget management, and expense category breakdown with visualization.

## User Review Required

> [!IMPORTANT]
> **Data Persistence**: I will be adding new collections to Firestore under the user's document to store budget plans. This ensures data is saved across sessions.
> **Currency API**: We will use the existing `CurrencyApiService` for exchange rates. Note that this requires an internet connection for live rates, otherwise, fallback rates will be used.
> **Visualizations**: The pie chart will be implemented as a custom-drawn Composable to avoid adding large external charting libraries.

## Proposed Changes

### Data Layer

#### [NEW] [BudgetData.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/data/BudgetData.kt)
Define data models for `BudgetPlan`, `ExpenseItem`, `IndividualBudget`, and `BudgetType`.

#### [MODIFY] [UserRepository.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/data/UserRepository.kt)
Add functions to save, fetch, and delete budget plans from Firestore.

### Logic Layer

#### [NEW] [BudgetViewModel.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/page/tinyApps/BudgetViewModel.kt)
Handle the business logic for:
- Fetching trips from the Calendar.
- Calculating currency conversions.
- Managing the state of a budget being edited (group vs individual, categories, etc.).
- Persisting budget plans.

### UI Layer

#### [MODIFY] [BudgetTrackerPage.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/page/tinyApps/BudgetTrackerPage.kt)
Update the "Planner" button to navigate to the new planner workflow.

#### [NEW] [BudgetPlannerListPage.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/page/tinyApps/BudgetPlannerListPage.kt)
- The "Notes app" style list of trips.
- Displays trip name, dates, and "time left" countdown.
- Contains the "Add" button to start a new plan.

#### [NEW] [BudgetTripSelector.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/page/tinyApps/BudgetTripSelector.kt)
- A black-background overlay showing future trips from the Calendar.
- Includes an option to manually add a trip name/date if not found in Calendar.

#### [NEW] [BudgetEditorPage.kt](file:///C:/Users/USER/Projects/All%20Direction/app/src/main/java/com/fourDirection/allDirection/page/tinyApps/BudgetEditorPage.kt)
The complex multi-section editor:
- **Currency Section**: Base/Exchange currency selectors + "Local Trip" toggle.
- **Budget Section**: Individual/Group toggle. Handles uniform vs different budgets for groups.
- **Expense Section**: Custom Pie Chart visualization + category-based expense entry.
- **Control Section**: Total vs Daily vs Currency toggle buttons for the chart.

## Verification Plan

### Automated Tests
- Unit tests for budget calculation logic (Individual vs Group, Same vs Different budget).
- Unit tests for currency conversion math.

### Manual Verification
- Deploy to device/emulator.
- Navigate to Budget -> Planner.
- Add a trip from the Calendar.
- Configure a group budget with "Different Budget" for 3 people.
- Add various expenses (Food, Entertainment) and verify the Pie Chart updates.
- Toggle between "Total", "Daily", and "Exchanged" views.
- Save and verify the trip appears in the "Notes" list.
- Verify the "Time Left" calculation is accurate.
