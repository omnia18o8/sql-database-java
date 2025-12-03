# cw-db

Lightweight database server that speaks a small SQL dialect, persists data as tab-separated files, and exposes both a blocking TCP server and a simple interactive client.

## Features
- File-backed storage under `databases/`, one folder per database and `.tab` file per table with auto-incrementing `id` column.
- Command coverage: `CREATE/DROP DATABASE`, `USE`, `CREATE TABLE`, `ALTER TABLE ADD/DROP`, `INSERT`, `SELECT` (with `WHERE`, comparison/boolean operators, and `LIKE`), `UPDATE`, `DELETE`, and `JOIN`.
- Protocol: server listens on TCP port `8888`, delimits responses with ASCII `EOT` (char `4`), and returns `[OK]`/`[ERROR]` tags.
- Persistence: databases remain on disk across server restarts; highest `id` values are tracked so inserts keep incrementing after deletions.

## Project layout
- `src/main/java/edu/uob/DBServer.java` – entrypoint for the TCP server and command dispatch.
- `src/main/java/edu/uob/DBClient.java` – small REPL client that connects to `localhost:8888`.
- `src/main/java/edu/uob/*` – parser/command handlers for the SQL-like grammar and file I/O helpers.
- `src/test/java/edu/uob/ExampleDBTests.java` – JUnit 5 regression tests exercising the command set.

## Prerequisites
- Java 17+
- Maven (wrapper scripts `./mvnw` / `mvnw.cmd` are included).

## Build and test
```bash
./mvnw test
```

## Run the server
Start the TCP server (blocking) on port 8888:
```bash
./mvnw exec:java@server
```

## Use the interactive client
In a second terminal, connect with the bundled client:
```bash
./mvnw exec:java@client
```
Type commands terminated by semicolons. Responses end when you see the EOT marker printed by the client loop.

## Example session
```
SQL:> CREATE DATABASE demo;
SQL:> USE demo;
SQL:> CREATE TABLE marks (name, mark, pass);
SQL:> INSERT INTO marks VALUES ('Simon', 65, TRUE);
SQL:> SELECT * FROM marks;
```

## Storage notes
- Data lives in `databases/` relative to the project root. Remove that folder if you need a clean slate (stop the server first).
- Table files are tab-separated with a header row; the first column is always the auto-generated `id`.


