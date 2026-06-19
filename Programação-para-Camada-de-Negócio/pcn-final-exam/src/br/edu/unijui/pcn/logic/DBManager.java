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
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBManager {

    private final String hostName;
    private final int port;
    private final String dbName;
    private final String username;
    private final String password;
    private Connection activeConnection = null;
    private static final Logger logger
            = Logger.getLogger(DBManager.class.getName());

    public DBManager(String hostName, int port, String dbName, String username, String password) {
        this.hostName = hostName;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    private Connection openConnection() throws SQLException {
        String url = String.format("jdbc:derby://%s:%d/%s", hostName, port, dbName);
        logger.info("Tentando conectar ao banco: " + url);

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (Throwable t) {
            logger.warning(
                    "Driver Derby não carregado explicitamente. "
                    + "Tentando conexão via DriverManager."
            );
            System.out.println("Aviso: Driver carregado por contingência via DriverManager.");
        }

        logger.info("Conexão com banco estabelecida com sucesso");

        return DriverManager.getConnection(url, username, password);
    }

    public void setAutoCommit(boolean status) throws SQLException {
        if (activeConnection == null || activeConnection.isClosed()) {
            activeConnection = openConnection();
        }
        activeConnection.setAutoCommit(status);

        logger.info("Commit realizado com sucesso");
    }

    public void commit() throws SQLException {
        if (activeConnection != null && !activeConnection.isClosed()) {
            activeConnection.commit();
            activeConnection.close();
            logger.info("Conexão encerrada");
        }
    }

    public void insertAll(List<IsolationRecord> records) {

        logger.info("Iniciando inserção de " + records.size() + "registros");
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

            int totalInseridos = 0;

            for (IsolationRecord record : records) {
                Long stateId = getOrInsertState(conn, record);

                stmtIso.setString(1, record.city());
                stmtIso.setLong(2, stateId);
                stmtIso.setDouble(3, record.index());
                stmtIso.setString(4, record.date());

                stmtIso.executeUpdate();

                totalInseridos++;
            }

            logger.info("Inserção concluída. Total de registros inseridos: " + totalInseridos);

            if (activeConnection == null && conn != null) {
                conn.close();
            }

        } catch (SQLException e) {
            logger.log(
                    Level.WARNING,
                    "Erro ao inserir registros no banco",
                    e
            );
            throw new RuntimeException("Erro ao inserir em lote: " + e.getMessage(), e);
        } finally {
            try {
                if (stmtIso != null) {
                    stmtIso.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private Long getOrInsertState(Connection conn, IsolationRecord record) throws SQLException {

        logger.info(
                "Estado já existente: "
                + record.state()
        );
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
            logger.info(
                    "Inserindo novo estado: "
                    + record.state()
                    + " (" + record.stateAcronym() + ")"
            );
            insertStmt.setString(1, record.state());
            insertStmt.setString(2, record.stateAcronym());
            insertStmt.executeUpdate();

            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    logger.info(
                            "Estado inserido com sucesso: "
                            + record.state()
                    );
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

        logger.info(
                "Buscando maior índice de isolamento para: "
                + whereToFind
        );

        try (Connection conn = openConnection()) {

            String sql;

            if (whereToFind.equals("Brazil")) {

                sql = """
                  SELECT s.NAME,
                         s.ACRONYM,
                         si.CITY,
                         si."INDEX",
                         si.DATE_WHEN
                  FROM SOCIAL_ISOLATION si
                  JOIN STATE s ON s.ID = si.STATE_ID
                  ORDER BY si.INDEX DESC
                  FETCH FIRST 1 ROW ONLY
                  """;

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }

            } else {

                String acronym = whereToFind.substring(
                        whereToFind.indexOf("(") + 1,
                        whereToFind.indexOf(")")
                );

                sql = """
                  SELECT s.NAME,
                         s.ACRONYM,
                         si.CITY,
                         si."INDEX",
                         si.DATE_WHEN
                  FROM SOCIAL_ISOLATION si
                  JOIN STATE s ON s.ID = si.STATE_ID
                  WHERE s.ACRONYM = ?
                  ORDER BY si.INDEX DESC
                  FETCH FIRST 1 ROW ONLY
                  """;

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, acronym);

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }
            }

            logger.info(
                    "Maior índice encontrado para "
                    + whereToFind
            );

        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Erro ao buscar maior índice de isolamento",
                    e
            );
        }

        return null;
    }

    public IsolationRecord findTheLowest(String whereToFind) {

        logger.info(
                "Buscando menor índice de isolamento para: "
                + whereToFind
        );

        try (Connection conn = openConnection()) {

            String sql;

            if (whereToFind.equals("Brazil")) {

                sql = """
                  SELECT s.NAME,
                         s.ACRONYM,
                         si.CITY,
                         si."INDEX",
                         si.DATE_WHEN
                  FROM SOCIAL_ISOLATION si
                  JOIN STATE s ON s.ID = si.STATE_ID
                  ORDER BY si.INDEX ASC
                  FETCH FIRST 1 ROW ONLY
                  """;

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }

                logger.info(
                        "Menor índice encontrado para "
                        + whereToFind
                );

            } else {

                String acronym = whereToFind.substring(
                        whereToFind.indexOf("(") + 1,
                        whereToFind.indexOf(")")
                );

                sql = """
                  SELECT s.NAME,
                         s.ACRONYM,
                         si.CITY,
                         si."INDEX",
                         si.DATE_WHEN
                  FROM SOCIAL_ISOLATION si
                  JOIN STATE s ON s.ID = si.STATE_ID
                  WHERE s.ACRONYM = ?
                  ORDER BY si.INDEX ASC
                  FETCH FIRST 1 ROW ONLY
                  """;

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, acronym);

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }
            }
            logger.info(
                    "Menor índice encontrado para "
                    + whereToFind
            );
        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Erro ao buscar menor índice de isolamento",
                    e
            );
        }

        return null;
    }

    public List<IsolationRecord> getAllRecords() {

        logger.info(
                "Iniciando recuperação de registros do banco"
        );

        List<IsolationRecord> records = new java.util.ArrayList<>();

        String sql = """
        SELECT
            s.NAME,
            s.ACRONYM,
            si.CITY,
            si."INDEX",
            si.DATE_WHEN
        FROM SOCIAL_ISOLATION si
        INNER JOIN STATE s
            ON s.ID = si.STATE_ID
        ORDER BY si.CITY ASC
    """;

        try (
                Connection conn = openConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                records.add(
                        new IsolationRecord(
                                rs.getString("NAME"),
                                rs.getString("ACRONYM"),
                                rs.getString("CITY"),
                                rs.getDouble("INDEX"),
                                rs.getString("DATE_WHEN")
                        )
                );
            }

            logger.info(
                    "Total de registros recuperados: "
                    + records.size()
            );

        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Erro ao recuperar registros do banco",
                    e
            );
        }

        return records;
    }
}
