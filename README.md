# 🎲 P2P Challenge & Betting System

A full-stack **Peer-to-Peer (P2P) Challenge and Betting Platform** built with **Java (Spring Boot)** and **Vanilla JS**, designed around clean Object-Oriented Programming (OOP) principles, layered architecture, and classic GoF Design Patterns.

Users can create custom prediction events, challenge other players in real-time mini-game rooms, manage virtual wallets, and track their match/betting history.

---

## 🏗️ Architecture & Design Patterns

The backend follows a strict **4-Layer Architecture** (`API / DTO` ➔ `Business / Service` ➔ `Core / Domain` ➔ `Data Access`), decoupling business rules from persistence mechanisms.

### Applied Design Patterns
* **State Pattern (`p2pbet.core.entities.state`):** Manages the lifecycle of a bet cleanly without complex conditional blocks (`PendingState` ➔ `MatchedState` ➔ `SettledState` / `CancelledState`).
* **Strategy Pattern (`p2pbet.business.strategy`):**
  * **Game Strategies (`IGameStrategy`):** Dynamically encapsulates game logic for different room types (`HighCardStrategy`, `DiceRouletteStrategy`, `ClosestNumberStrategy`, `HighestRollStrategy`, `JackpotStrategy`).
  * **Payout Strategies (`PayoutStrategy`):** Decouples reward distribution algorithms (`ZeroSumPayoutStrategy`).
* **Facade Pattern (`SettlementEngineFacade`):** Provides a simplified unified interface to coordinate complex settlement workflows between wallets, bets, and game rooms.
* **Repository Pattern (`p2pbet.dataaccess`):** Abstracts data persistence behind interfaces (`IUserRepository`, `IBetRepository`, etc.) with a custom CSV storage engine (`AbstractCsvRepository`, `CsvDataStorage`), making it effortless to migrate to a relational database like PostgreSQL in the future.
* **Optimistic Locking:** Prevents race conditions during concurrent wallet transactions and bet matching (`OptimisticLockException`).

---

## ✨ Key Features

* **User & Admin Management:** Role-based operations with separate user and administrator endpoints.
* **Virtual Wallet System:** Deposit funds, lock balances during active bets/games, and automated settlement payouts.
* **P2P Prediction Events:** Create events, place pending bets, match against opponents, and resolve outcomes via Admin controls.
* **Interactive Game Rooms:** Create or join multiplayer rooms featuring 5 distinct mini-games (High Card, Dice Roulette, Closest Number, Highest Roll, Jackpot).
* **Custom Exceptions:** Domain-specific error handling (`InsufficientFundsException`, `EventStartedException`, `InvalidBetOperationException`, `UnauthorizedActionException`).

---

## 🛠️ Tech Stack

* **Backend:** Java, Spring Boot, Maven
* **Frontend:** HTML5, CSS3, Vanilla JavaScript
* **Storage:** Custom CSV File-Based Persistence (`users.csv`, `wallets.csv`, `bets.csv`, `events.csv`, `game_rooms.csv`, `admins.csv`)
* **Documentation & Modeling:** UML (Class, Sequence, Activity, State, and Use Case Diagrams)

---

## 📂 Project Structure

```text
P2P-Challenge-System/
├── docs/                       # UML Diagrams (Class, Sequence, State, Activity) & Reports
├── frontend/                   # Web UI (HTML, CSS, JS & game assets)
├── src/main/java/p2pbet/
│   ├── api/                    # REST Controllers, DTOs & WebConfig
│   ├── business/               # Services, Facade & Strategy implementations
│   ├── core/entities/          # Domain models, State pattern & Custom Exceptions
│   └── dataaccess/             # Repository interfaces & CSV storage engine
├── *.csv                       # Initial data storage files
└── pom.xml                     # Maven dependencies & build configuration