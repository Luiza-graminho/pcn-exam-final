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
    private Connection activeConnection = null;

    public DBManager(String hostName, int port, String dbName, String username, String password) {
        this.hostName = hostName;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    private Connection openConnection() throws SQLException {
        String url = String.format("jdbc:derby://%s:%d/%s", hostName, port, dbName);
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (Throwable t) {
            System.out.println("Aviso: Driver carregado por contingência via DriverManager.");
        }

        return DriverManager.getConnection(url, username, password);
    }
    
    public void setAutoCommit(boolean status) throws SQLException {
        if (activeConnection == null || activeConnection.isClosed()) {
            activeConnection = openConnection();
        }
        activeConnection.setAutoCommit(status);
    }

    public void commit() throws SQLException {
        if (activeConnection != null && !activeConnection.isClosed()) {
            activeConnection.commit();
            activeConnection.close();
        }
    }

    public void insertAll(List<IsolationRecord> records) {
        Connection conn = null;
        PreparedStatement stmtIso = null;
        
        try {
            if (activeConnection != null && !activeConnection.isClosed()) {
                conn = activeConnection;
            } else {
                conn = openConnection();
                conn.setAutoCommit(true);
            }

            String sqlIsolation = "INSERT INTO SOCIAL_ISOLATION (CITY, STATE_ID, INDEX, DATE_WHEN) VALUES (?, ?, ?, ?)";
            stmtIso = conn.prepareStatement(sqlIsolation);

            for (IsolationRecord record : records) {
                Long stateId = getOrInsertState(conn, record);

                stmtIso.setString(1, record.city());
                stmtIso.setLong(2, stateId);
                stmtIso.setDouble(3, record.index());
                stmtIso.setString(4, record.date());
                
                stmtIso.executeUpdate();
            }

            if (activeConnection == null && conn != null) {
                conn.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao inserir em lote: " + e.getMessage(), e);
        } finally {
            try { if (stmtIso != null) stmtIso.close(); } catch (SQLException e) {}
        }
    }
    
    private Long getOrInsertState(Connection conn, IsolationRecord record) throws SQLException {
        String selectSql = "SELECT ID FROM STATE WHERE NAME = ?";
        String insertSql = "INSERT INTO STATE (NAME, ACRONYM) VALUES (?, ?)";
        
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, record.state());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("ID");
                }
            }
        }
        
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, record.state());
            insertStmt.setString(2, record.stateAcronym()); 
            insertStmt.executeUpdate();
            
            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        return 0L;
    }
    
    public Long getOrInsertState(IsolationRecord record) throws SQLException {
        try (Connection conn = openConnection()) {
            return getOrInsertState(conn, record);
        }
    }
    
    public IsolationRecord findTheHighest(String whereToFind) {
        return null;
    }
    
    public IsolationRecord findTheLowest(String whereToFind) {
        return null;
    }
}