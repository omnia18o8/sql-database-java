package edu.uob;

import java.io.File;
import java.io.IOException;

public class CmdDrop extends DBCmd {
    private final String identifierType;
    private final String identifierName;

    public CmdDrop(DBServer server, QueryParser parser) throws IOException {
        super(server);
        parser.parseDrop();

        if (parser.isDropTable()) {
            this.identifierType = "TABLE";
            this.identifierName = parser.getTableName();
        } else {
            this.identifierType = "DATABASE";
            this.identifierName = parser.getDatabaseName();
        }

        ReadWrite readWrite = new ReadWrite();
        readWrite.setCurrentDatabase(server.getDatabaseName());
    }

    @Override
    public String query(DBServer server) {
        if (identifierType.equals("TABLE")) {
            return dropTable(server);
        } else {
            return dropDatabase(server);
        }
    }

    private String dropTable(DBServer server) {
        String tablePath = server.getStorageFolderPath() + File.separator +
                server.getDatabaseName() + File.separator +
                identifierName + ".tab";

        File tableFile = new File(tablePath);

        if (tableFile.exists()) {
            if (tableFile.delete()) {
                server.removeTable(identifierName);
                return "[OK]";
            } else {
                return "[ERROR] Could not delete table '" + identifierName + "'.";
            }
        }
        return "[ERROR] Table '" + identifierName + "' does not exist.";
    }

    private String dropDatabase(DBServer server) {
        String dbPath = server.getStorageFolderPath() + File.separator + identifierName;
        File dbFolder = new File(dbPath);

        File[] files = dbFolder.listFiles((dir, name) -> name.endsWith(".tab"));
        boolean allFilesDeleted = true;

        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    allFilesDeleted = false;
                }
            }
        }

        if (allFilesDeleted && dbFolder.delete()) {
            if (server.getDatabaseName().equals(identifierName)) {
                server.setDatabaseName(null);
            }
            return "[OK]";
        }
        return "[ERROR] Could not delete some files in database '" + identifierName + "'.";
    }
}
