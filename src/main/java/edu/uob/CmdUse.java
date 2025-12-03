package edu.uob;

public class CmdUse extends DBCmd {
    public CmdUse(DBServer server) {
        super(server);
    }

    @Override
    public String query(DBServer server) {
        server.setDatabaseName(databaseName);
        return "[OK]";
    }
}
