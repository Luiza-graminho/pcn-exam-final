package br.edu.unijui.pcn.logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBManager {

    private final String hostName;
    private final int port;
    private final String dbName;
    private final String username;
    private final String password;
    private Connection activeConnection = null;
    private static final Logger logger = Logger.getLogger(DBManager.class.getName());

    // Construtor responsável pela conexão com o banco de dados
    public DBManager(String hostName, int port, String dbName, String username, String password) {
        //Armazena os parametros necessários para conexão com o banco
        this.hostName = hostName;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    private Connection openConnection() throws SQLException {
        // Monta a URL de conexão utilizando os parâmetros informados
        String url = String.format("jdbc:derby://%s:%d/%s", hostName, port, dbName);
        
        // Tenta carregar explicitamente o driver JDBC do Derby
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            logger.info("Driver Derby carregado com sucesso");
        } catch (Throwable t) {
            logger.warning(
                    "Driver Derby não carregado explicitamente. "
                    + "Tentando conexão via DriverManager."
            );
        }

        logger.info("Abrindo conexão com o banco de dados");

        // Cria e retorna uma nova conexão com o banco de dados
        return DriverManager.getConnection(url, username, password);
    }

    //Garante que exista uma conexão ativa antes de alterar modo de transação
    public void setAutoCommit(boolean status) throws SQLException {
        if (activeConnection == null || activeConnection.isClosed()) {
            activeConnection = openConnection();
        }
        activeConnection.setAutoCommit(status);

        logger.log(Level.INFO, "AutoCommit alterado para: {0}", status);
    }

    // Confirma as alterações pendentes da transação
    public void commit() throws SQLException {
        if (activeConnection != null && !activeConnection.isClosed()) {
            logger.info("Realizando commit da transação");
            activeConnection.commit();
            logger.info("Encerrando conexão após commit");
            activeConnection.close();
            logger.info("Conexão encerrada");
        }
    }

    // Insere lista de registros de isolamento social no banco
    public void insertAll(List<IsolationRecord> records) {

        logger.log(Level.INFO, "Iniciando insercaoo de {0}registros", records.size());
        Connection conn = null;
        PreparedStatement stmtIso = null;

        try {
            //Utiliza conexão ativa caso exista, caso contrário abre nova conexão
            if (activeConnection != null && !activeConnection.isClosed()) {
                conn = activeConnection;
            } else {
                conn = openConnection();
                conn.setAutoCommit(true);
            }

            // Comando SQL utilizado para inserir registros na tabela SOCIAL_ISOLATION
            String sqlIsolation = "INSERT INTO SOCIAL_ISOLATION (CITY, STATE_ID, INDEX, DATE_WHEN) VALUES (?, ?, ?, ?)";
            stmtIso = conn.prepareStatement(sqlIsolation);

            int totalInseridos = 0;

            // Percorre todos os registros recebidos para persistencia
            for (IsolationRecord record : records) {
                Long stateId = getOrInsertState(conn, record);

                stmtIso.setString(1, record.city());
                stmtIso.setLong(2, stateId);
                stmtIso.setDouble(3, record.index());
                stmtIso.setString(4, record.date());

                stmtIso.executeUpdate();

                logger.log(Level.FINE, "Inserindo registro: {0} - {1}", new Object[]{record.city(), record.stateAcronym()});
                
                totalInseridos++;
            }

            logger.log(Level.INFO, "Insercao concluida. Total de registros inseridos: {0}", totalInseridos);

            if (activeConnection == null && conn != null) {
                conn.close();
            }

        } catch (SQLException e) {
            logger.log(Level.WARNING,"Erro ao inserir registros no banco",e);
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

        logger.log(Level.INFO, "Verificando existencia do estado: {0}", record.state());
        
        // Consulta o ID do estado pelo nome
        String selectSql = "SELECT ID FROM STATE WHERE NAME = ?";
        String insertSql = "INSERT INTO STATE (NAME, ACRONYM) VALUES (?, ?)";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, record.state());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("Estado encontrado no banco");
                    return rs.getLong("ID");
                }
            }
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            logger.log(Level.INFO, "Inserindo novo estado: {0} ({1})", new Object[]{record.state(), record.stateAcronym()});
            // Caso o estado não exista, realiza sua inserção
            insertStmt.setString(1, record.state());
            insertStmt.setString(2, record.stateAcronym());
            insertStmt.executeUpdate();

            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    logger.log(Level.INFO, "Estado inserido com sucesso: {0}", record.state());
                    // Retorna a chave gerada para o novo estado
                    return generatedKeys.getLong(1);

                }
            }
        }
        
        logger.log(Level.WARNING, "Nao foi possivel obter o ID do estado: {0}", record.state());
        return 0L;
    }

    // Busca o registro com maior índice de isolamento social
    public IsolationRecord findTheHighest(String whereToFind) {

        logger.log(Level.INFO, "Buscando maior indice de isolamento para: {0}", whereToFind);

        try (Connection conn = openConnection()) {

            String sql;

            // Consulta o maior índice considerando todos os estados do país
            if ("Brazil".equals(whereToFind)) {

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
            // Extrai a sigla do estado selecionado
            // Consulta o maior índice somente do estado informado
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
                    logger.log(Level.INFO, "Maior Indice encontrado para {0}", whereToFind);
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }
            }

            

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao buscar maior índice de isolamento", e);
        }

        
        return null;
    }

    // Busca o registro com menor índice de isolamento social
    public IsolationRecord findTheLowest(String whereToFind) {

        logger.log(Level.INFO, "Buscando menor Indice de isolamento para: {0}", whereToFind);

        try (Connection conn = openConnection()) {

            String sql;

            if ("Brazil".equals(whereToFind)) {

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
                    logger.log(Level.INFO, "Menor Indice encontrado para {0}", whereToFind);
                    return new IsolationRecord(
                            rs.getString("NAME"),
                            rs.getString("ACRONYM"),
                            rs.getString("CITY"),
                            rs.getDouble("INDEX"),
                            rs.getString("DATE_WHEN")
                    );
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao buscar menor índice de isolamento", e);
        }

        return null;
    }

    // Recupera todos os registros cadastrados no banco de dados
    public List<IsolationRecord> getAllRecords() {

        logger.info("Iniciando recuperação de registros do banco");

        List<IsolationRecord> records = new java.util.ArrayList<>();

        // Consulta responsável por recuperar os registros e as informações do estado relacionado
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

            // Percorre todos os registros retornados pela consulta
            while (rs.next()) {

                // Converte cada linha do resultSet para um objeto IsolationRecord
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
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao recuperar registros do banco", e);
        }
        logger.log(Level.INFO, "Total de registros recuperados: {0}", records.size());
        return records;
    }
}
