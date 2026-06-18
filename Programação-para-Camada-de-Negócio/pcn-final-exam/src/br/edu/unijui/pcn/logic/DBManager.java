package br.edu.unijui.pcn.logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBManager {

    private final String hostName;
    private final int port;
    private final String dbName;
    private final String username;
    private final String password;

    public DBManager(String hostName, int port, String dbName, String username, String password) {
        this.hostName = hostName;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    private Connection openConnection() throws SQLException {
        String url = String.format("jdbc:derby://%s:%d/%s", hostName, port, dbName);

        return DriverManager.getConnection(url, username, password);
    }

    public void insertAll(List<IsolationRecord> records) {
        // implemente este método
    }
    
    public Long getOrInsertState(IsolationRecord record) throws SQLException {
        // atualize este método para que ele verifique se um estado está cadastrado
        // caso contrário, cadastre-o
        return 0L;
    }
    
    public IsolationRecord findTheHighest(String whereToFind) {
        return null;
    }
    
    public IsolationRecord findTheLowest(String whereToFind) {
        return null;
    }
}
