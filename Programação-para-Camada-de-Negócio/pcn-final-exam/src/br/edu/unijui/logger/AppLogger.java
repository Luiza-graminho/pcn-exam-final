package br.edu.unijui.logger;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.*;

/**
 * Classe responsável pela configuração centralizada do sistema de logs
 * 
 * Permite configurar:
 * - arquivo de saída dos logs;
 * - nível de detalhamento das mensagens;
 * - formato de saída (texto ou XML);
 * - exibição ou supressão dos logs no console;
 *
 * @author Luiza Graminho e Isadora Beckmann
 */
public class AppLogger {

    // Logger principal utilizado pela aplicação
    private static final Logger logger = Logger.getLogger("COVID_LOGGER");

    // Inicializa e configura o sistema de logs da aplicação
    public static void init() {
        try {
            logger.info("Iniciando configuração do sistema de logs");
            
            // Carrega as configurações definidas para o sistema de logs
            Properties conf = getLogConf();

            logger.info("Configuração carregada. Arquivo: "+ conf.getProperty("file"));
            
            // Cria o manipulador responsável pela gravação dos logs em arquivo
            FileHandler fh = new FileHandler(
                    conf.getProperty("file"),
                    Boolean.parseBoolean(conf.getProperty("append"))
            );

            logger.info("Arquivo de log configurado com sucesso");
            
            // Obtém o logger raiz da aplicação
            Logger root = Logger.getLogger("");
            // Associa o manipulador de arquivo ao logger raiz
            root.addHandler(fh);

            // Define o formato de saída dos registros de log
            if (conf.getProperty("output-format").equalsIgnoreCase("XML")) {
                logger.info("Formato de saída configurado para XML");
                fh.setFormatter(new XMLFormatter());
            } else {
                logger.info("Formato de saída configurado para texto simples");
                fh.setFormatter(new SimpleFormatter());
            }

            // Remove a saída para o console caso configurado
            if (Boolean.parseBoolean(conf.getProperty("suppress-console-output"))) {
                Handler[] handlers = root.getHandlers();
                // Percorre os manipuladores registrados removendo o console
                for (Handler h : handlers) {
                    if (h instanceof ConsoleHandler) {
                        root.removeHandler(h);
                    }
                }
                logger.info("Saída de logs no console desabilitada");
            }

            // Define o nível mínimo de mensagens que serão registradas
            root.setLevel(parseLevel(conf.getProperty("level")));
            logger.info("Nível de log configurado para: "+ conf.getProperty("level"));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao configurar logger", e);
        }
        logger.info("Sistema de logs inicializado com sucesso");
    }

    // Converte uma representação textual para um obj level
    private static Level parseLevel(String level) {
        // Converte o texto informado para um nível reconhecido pelo Logger
        return switch (level.toUpperCase()) {
            case "SEVERE" -> Level.SEVERE;
            case "WARNING" -> Level.WARNING;
            case "INFO" -> Level.INFO;
            case "CONFIG" -> Level.CONFIG;
            case "FINE" -> Level.FINE;
            case "FINER" -> Level.FINER;
            case "FINEST" -> Level.FINEST;
            case "OFF" -> Level.OFF;
            default -> Level.INFO;
        };
    }

    // Retorna o logger principal da aplicação
    public static Logger getLogger() {
        // Disponibiliza o logger configurado para outras classes
        return logger;
    }

    // Cria as configurações padrão do sistema de logs
    private static Properties getLogConf() {
        // Estrutura responsável por armazenar os parametros de configuração
        Properties conf = new Properties();

        // Nome do arquivo de saída dos logs
        conf.setProperty("file", "app.log");
        // Define se os logs devem ser adicionados no arquivo existente
        conf.setProperty("append", "true");
        // Define se os logs devem aparecer no console
        conf.setProperty("suppress-console-output", "false");
        // Nível mínimo das mensagens registradas
        conf.setProperty("level", "INFO");
        // Formato de saída dos logs
        conf.setProperty("output-format", "SIMPLE");

        return conf;
    }

}
