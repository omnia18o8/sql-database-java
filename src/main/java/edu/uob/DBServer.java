package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DBServer {
    private static final char END_OF_TRANSMISSION = 4;
    private final String storageFolderPath;
    private String databaseName;
    private final Map<String, List<String>> databaseTables = new HashMap<>();
    private final Map<String, Integer> tableHighestIds = new HashMap<>();

    public static void main(String[] args) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch (IOException e) {
            System.out.println("Can't create database storage folder: " + storageFolderPath);
        }
    }
    public String getStorageFolderPath() {
        return storageFolderPath;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String dbName) {
        this.databaseName = dbName;
        loadTablesForDatabase(dbName);
    }

    public boolean doesDBExist(String dbName) {
        File dbFolder = new File(getStorageFolderPath() + File.separator + dbName);
        return dbFolder.exists() && dbFolder.isDirectory();
    }

    public boolean doesTableExist(String tableName) {
        String tableFilePath = storageFolderPath + File.separator + databaseName + File.separator + tableName + ".tab";
        return new File(tableFilePath).exists();
    }


    private void loadTablesForDatabase(String dbName) {
        databaseTables.put(dbName, new ArrayList<>());
        File dbFolder = new File(storageFolderPath + File.separator + dbName);

        if (dbFolder.exists() && dbFolder.isDirectory()) {
            File[] tableFiles = dbFolder.listFiles();
            if (tableFiles != null) {
                for (File file : tableFiles) {
                    String tableName = file.getName();
                    databaseTables.get(dbName).add(tableName);
                    loadHighestIdForTable(tableName);
                }
            }
        }
    }

    private void loadHighestIdForTable(String tableName) {
        File tableFile = new File(storageFolderPath + File.separator + databaseName + File.separator + tableName);
        int highestId = 0;

        if (tableFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(tableFile.toPath());
                for (int i = 1; i < lines.size(); i++) {
                    String[] row = lines.get(i).split("\t");
                    try {
                        int id = Integer.parseInt(row[0]);
                        if (id > highestId) {
                            highestId = id;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Unable to read table file: " + tableName);
            }
        }

        tableHighestIds.put(tableName.replace(".tab", ""), highestId);
    }


    public int getNextIdForTable(String tableName) {
        return tableHighestIds.getOrDefault(tableName, 0) + 1;
    }

    public void updateHighestIdForTable(String tableName, int newId) {
        tableHighestIds.put(tableName, newId);
    }

    public void addTable(String tableName) {
        if (!databaseTables.containsKey(databaseName)) {
            databaseTables.put(databaseName, new ArrayList<>());
        }

        if (!databaseTables.get(databaseName).contains(tableName)) {
            databaseTables.get(databaseName).add(tableName);
        }
        File tableFile = new File(storageFolderPath + File.separator + databaseName + File.separator + tableName + ".tab");
        if (!tableFile.exists()) {
            try {
                tableFile.createNewFile();
            } catch (IOException e) {
                System.err.println("[ERROR] Unable to create table file: " + tableName + ".tab");
            }
        }
    }


    public void removeTable(String tableName) {
        if (databaseTables.containsKey(databaseName)) {
            databaseTables.get(databaseName).remove(tableName);
        }
        tableHighestIds.remove(tableName);

        File tableFile = new File(storageFolderPath + File.separator + databaseName + File.separator + tableName + ".tab");
        if (tableFile.exists()) {
            tableFile.delete();
        }
    }


    public String handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "[ERROR] Empty command received.";
        }
        String previousDatabase = getDatabaseName();

        try {
            QueryParser parser = new QueryParser(command);
            DBCmd parsedCommand = null;
            String upperCommand = command.trim().toUpperCase();

            if (upperCommand.startsWith("USE")) {
                String dbName = parser.parseUse();
                if (doesDBExist(dbName)) {
                    setDatabaseName(dbName);
                    parsedCommand = new CmdUse(this);
                } else {
                    return "[ERROR] Database '" + dbName + "' does not exist.";
                }
            } else if (upperCommand.startsWith("CREATE DATABASE")) {
                String dbName = parser.parseCreateDB();
                parsedCommand = new CmdCreateDatabase(this, dbName);
            } else if (upperCommand.startsWith("CREATE TABLE")) {
                parsedCommand = new CmdCreateTable(this, parser);
            } else if (upperCommand.startsWith("INSERT")) {
                parsedCommand = new CmdInsert(this, parser);
            } else if (upperCommand.startsWith("ALTER")) {
                parsedCommand = new CmdAlter(this, parser);
            } else if (upperCommand.startsWith("DROP")) {
                parsedCommand = new CmdDrop(this, parser);
            } else if (upperCommand.startsWith("JOIN")) {
                parsedCommand = new CmdJoin(this, parser);
            } else if (upperCommand.startsWith("SELECT")) {
                parsedCommand = new CmdSelect(this, parser);
            } else if (upperCommand.startsWith("UPDATE")) {
                parsedCommand = new CmdUpdate(this, parser);
            } else if (upperCommand.startsWith("DELETE")) {
                parsedCommand = new CmdDelete(this, parser);
            }

            if (parsedCommand != null) {
                return parsedCommand.query(this);
            } else {
                return "[ERROR] Unknown or invalid command.";
            }

        } catch (IOException e) {
            setDatabaseName(previousDatabase);
            return "[ERROR] " + e.getMessage();
        }
    }


    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(serverSocket);
                } catch (IOException e) {
                    System.err.println("Non-fatal IO error: " + e.getMessage());
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket socket = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            System.out.println("Connection established: " + socket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                if (incomingCommand == null) break;

                System.out.println("Received command: " + incomingCommand);
                String result = handleCommand(incomingCommand);

                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
