package edu.uob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CmdCreateDatabase extends DBCmd {
    public CmdCreateDatabase(DBServer server, String databaseName) {
        super(server);
        this.databaseName = databaseName;
    }

    @Override
    public String query(DBServer server) throws IOException {
        String dbPath = server.getStorageFolderPath() + File.separator + databaseName;
        try {
            var getDbPath = Paths.get(dbPath);
            if (!Files.exists(getDbPath)) {
                Files.createDirectories(getDbPath);
            } else {
                return ("ERROR: database already exists: " + databaseName);
            }

            return "[OK]";
        } catch (IOException e) {
            return "[Error]: Could not create database.";
        }
    }
}
