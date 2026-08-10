# K-Line Tyre House Management System

A comprehensive offline-first desktop application for tyre shop management, built with JavaFX 21 and MySQL. The system handles inventory, invoicing, credit sales, worker management, salary calculations, and business analytics — all with local storage and optional cloud sync.

---

## Features

### Core Modules
- **Dashboard** — Real-time KPIs, revenue charts, quick service actions, stock alerts
- **Inventory** — Product management with stock tracking, categories, brands, and low-stock alerts
- **Invoices & Billing** — Full invoice creation, line items, PDF export, payment tracking
- **Services** — Record tyre services, repairs, and maintenance with pricing
- **Credit Sales** — Customer credit management, payment tracking, and settlement
- **Tyre Exports** — Export tracking for B2B tyre sales with profit calculation
- **Expenses** — Daily expense logging and categorization
- **Salary Management** — Worker attendance-based salary calculation, advances, and credit deductions
- **Worker Management** — Worker profiles, attendance tracking, and payment history
- **Analytics** — Revenue trends, profit analysis, and business insights
- **Reports** — Comprehensive business reports and data export

### Quick Actions
- One-click service logging from sidebar or dashboard
- Customizable quick service presets with icons
- Instant session stats and daily totals

### Offline-First Architecture
- All data stored locally in MySQL
- Background sync queue for pending operations
- One-click upload to Spring Boot backend
- Connectivity monitoring with visual status indicator

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Frontend | JavaFX 21, Java 21 |
| Build Tool | Maven 3.8+ |
| Local Database | MySQL 8.0+ |
| Backend Sync | Spring Boot (optional) |
| Icons | Ikonli FontAwesome 5 |
| PDF Export | OpenPDF |

---

## Prerequisites

- **Java 21** (Eclipse Temurin or Microsoft OpenJDK)
- **Maven 3.8+** (or use included `mvnw`)
- **MySQL 8.0+** running locally
- **macOS / Windows / Linux** with JavaFX support

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/Aurion-dev-lab/K-Line-TyreHouse-Frontend.git
cd K-Line-TyreHouse-Frontend
```

### 2. Set Up Local Database

Create a MySQL database for local storage:

```sql
CREATE DATABASE kline_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Import the schema (if provided):

```bash
mysql -u root -p kline_local < setup-database.sql
```

### 3. Configure Environment Variables

The app uses environment variables for configuration. Create a `.env` file or set them in your system:

```bash
# Database Configuration
KLINE_DB_URL=jdbc:mysql://localhost:3306/kline_local?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
KLINE_DB_USER=root
KLINE_DB_PASSWORD=

# Sync Configuration (optional)
KLINE_SYNC_URL=http://localhost:8080/api/sync/batch
KLINE_SYNC_API_KEY=change-me
```

### 4. Build the Application

```bash
./mvnw clean package
```

### 5. Run the Application

**From IntelliJ IDEA:**
- Open the project
- Run `App.java` (JavaFX run configuration)

**From terminal:**
```bash
./mvnw javafx:run
```

---

## Configuration Reference

### Database Settings
| Variable | Default | Description |
|----------|---------|-------------|
| `KLINE_DB_URL` | `jdbc:mysql://localhost:3306/kline_local?...` | JDBC connection string |
| `KLINE_DB_USER` | `root` | MySQL username |
| `KLINE_DB_PASSWORD` | *(empty)* | MySQL password |

### Sync Settings
| Variable | Default | Description |
|----------|---------|-------------|
| `KLINE_SYNC_URL` | `http://localhost:8080/api/sync/batch` | Backend sync endpoint |
| `KLINE_SYNC_API_KEY` | `change-me` | API key for authentication |

---

## Project Structure

```
K-Line-TyreHouse-Frontend/
├── src/main/java/com/gui/kline/
│   ├── App.java                          # Application entry point
│   ├── controller/                       # JavaFX controllers
│   │   ├── LayoutController.java         # Main layout and navigation
│   │   ├── DashboardController.java      # Dashboard and KPIs
│   │   ├── InventoryController.java      # Inventory management
│   │   ├── InvoicesController.java       # Invoice operations
│   │   ├── ServicesController.java       # Service management
│   │   ├── CreditSalesController.java    # Credit sales
│   │   ├── TyreExportsController.java    # Export management
│   │   ├── ExpenseController.java        # Expense tracking
│   │   ├── SalaryController.java         # Salary management
│   │   ├── WorkerManagementController.java # Worker management
│   │   ├── AnalyticsController.java      # Analytics
│   │   ├── ReportsController.java        # Reports
│   │   └── form/                         # Dialog controllers
│   ├── data/                             # Data repositories
│   ├── models/                           # Data models
│   ├── service/                          # Business logic services
│   ├── utils/                            # Utility classes
│   └── view/                             # View factory
├── src/main/resources/com/gui/kline/
│   ├── view/                             # FXML layouts
│   │   ├── main-layout.fxml              # Main application layout
│   │   ├── dashboard.fxml                # Dashboard view
│   │   ├── inventory.fxml                # Inventory view
│   │   ├── invoices.fxml                 # Invoices view
│   │   ├── services.fxml                 # Services view
│   │   ├── credit-sales.fxml             # Credit sales view
│   │   ├── tyre-exports.fxml             # Exports view
│   │   ├── expenses.fxml                 # Expenses view
│   │   ├── salary.fxml                   # Salary view
│   │   ├── workers.fxml                  # Workers view
│   │   ├── analytics.fxml                # Analytics view
│   │   ├── reports.fxml                  # Reports view
│   │   └── form/                         # Dialog FXMLs
│   └── css/                              # Stylesheets
├── pom.xml                               # Maven configuration
├── mvnw                                  # Maven wrapper
└── README.md                             # This file
```

---

## Sync Architecture

The application uses an **offline-first** approach:

1. **Local Storage** — All data is written to the local MySQL database immediately
2. **Sync Queue** — Every create/update/delete operation is also enqueued in `sync_queue`
3. **Background Sync** — When the user clicks **Upload**, pending records are sent to the backend in a single batch
4. **Conflict Resolution** — The backend handles conflicts and returns updated records

### Sync Flow
```
User Action → Local DB Write → Sync Queue Enqueue
                                    ↓
                            [Upload Button Clicked]
                                    ↓
                            Batch Send to Backend
                                    ↓
                            Backend Processes & Acknowledges
```

---

## Building for Production

### Create Executable JAR

```bash
./mvnw clean package -DskipTests
```

The executable JAR will be at:
```
target/K-Line-1.0-SNAPSHOT.jar
```

### Run Executable

```bash
java -jar target/K-Line-1.0-SNAPSHOT.jar
```

### CLI Tool

Verify local database and sync queue status:

```bash
java -cp target/K-Line-1.0-SNAPSHOT.jar com.gui.kline.SyncCli
```

---

## Backend Server (Optional)

A minimal Spring Boot sync server is available in the `server/` directory:

```bash
cd server
./mvnw spring-boot:run
```

The server provides:
- `/api/sync/batch` — Batch sync endpoint
- `/health` — Health check for connectivity monitoring
- MySQL database for persistent storage

---

## Troubleshooting

### JavaFX Not Found
Ensure you're using Java 21 with JavaFX 21. The `pom.xml` includes all necessary dependencies.

### MySQL Connection Failed
- Verify MySQL is running: `mysql -u root -p`
- Check `KLINE_DB_URL`, `KLINE_DB_USER`, `KLINE_DB_PASSWORD` environment variables
- Ensure the database `kline_local` exists

### Icons Not Loading
The app uses Ikonli FontAwesome 5 icons. If icons don't appear, ensure the FontAwesome font pack is on the classpath (included in `pom.xml`).

### Sync Upload Fails
- Verify the backend server is running at `KLINE_SYNC_URL`
- Check the API key matches `KLINE_SYNC_API_KEY`
- Ensure network connectivity (green status bulb in header)

---

## Development

### IDE Setup (IntelliJ IDEA)

1. Open the project directory
2. IntelliJ will auto-detect the Maven project
3. Ensure Java 21 SDK is configured
4. Run `mvn clean compile` to download dependencies
5. Create a JavaFX run configuration for `com.gui.kline.App`

### Adding New Features

- **Controllers** → `src/main/java/com/gui/kline/controller/`
- **Views** → `src/main/resources/com/gui/kline/view/`
- **Models** → `src/main/java/com/gui/kline/models/`
- **Data Repositories** → `src/main/java/com/gui/kline/data/`
- **Styles** → `src/main/resources/com/gui/kline/css/`

---

## License

Proprietary — All rights reserved.

---

## Support

For issues or feature requests, contact the development team.