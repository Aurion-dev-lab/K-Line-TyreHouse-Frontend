# K-Line Tyre House Frontend (Offline-First)

This JavaFX app runs offline using a **local MySQL database**. At the end of the day, users can click **Upload** to sync local data to the Spring Boot backend.

## Local Storage (MySQL)
Set your local MySQL connection using:
- `KLINE_DB_URL` (default: `jdbc:mysql://localhost:3306/kline_local?...`)
- `KLINE_DB_USER` (default: `root`)
- `KLINE_DB_PASSWORD` (default: empty)

## Sync Configuration
Set the upload URL and API key using:
- `KLINE_SYNC_URL` (default: `http://localhost:8080/api/sync/batch`)
- `KLINE_SYNC_API_KEY` (default: `change-me`)

## Quick Check (CLI)
Run the small CLI to verify the local DB and pending queue count:

```bash
mvn -q -DskipTests package
java -cp target/K-Line-1.0-SNAPSHOT.jar com.gui.kline.SyncCli
```

## Upload Flow
1. App records data locally in MySQL.
2. User clicks **Upload** in the header bar.
3. Pending records are sent in a single batch.

## Sync Server (Spring Boot)
A minimal Spring Boot sync API is included under `server/` and also uses MySQL.

```bash
cd server
mvn -q -DskipTests spring-boot:run
```

Set server DB credentials using the same `KLINE_DB_URL`, `KLINE_DB_USER`, `KLINE_DB_PASSWORD` env vars.

## Build Outputs (Target Folder)
- Frontend build output: `target/`
- Backend build output: `server/target/`

Source code is under `src/` (frontend) and `server/src/` (backend).
