package br.edu.unijui.logger;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.*;

/**
 *
 * @author Luiza Graminho e Isadora Beckmann
 */
public class AppLogger {

    private static final Logger logger = Logger.getLogger("COVID_LOGGER");

    public static void init() {
        try {
            Properties conf = getLogConf();

            FileHandler fh = new FileHandler(
                    conf.getProperty("file"),
                    Boolean.parseBoolean(conf.getProperty("append"))
            );

            Logger root = Logger.getLogger("");
            root.addHandler(fh);

            // Formatter
            if (conf.getProperty("output-format").equalsIgnoreCase("XML")) {
                fh.setFormatter(new XMLFormatter());
            } else {
                fh.setFormatter(new SimpleFormatter());
            }

            // Console
            if (Boolean.parseBoolean(conf.getProperty("suppress-console-output"))) {
                Handler[] handlers = root.getHandlers();
                for (Handler h : handlers) {
                    if (h instanceof ConsoleHandler) {
                        root.removeHandler(h);
                    }
                }
            }

            // Level
            root.setLevel(parseLevel(conf.getProperty("level")));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao configurar logger", e);
        }
    }

    private static Level parseLevel(String level) {
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

    public static Logger getLogger() {
        return logger;
    }

    private static Properties getLogConf() {
        Properties conf = new Properties();

        conf.setProperty("file", "app.log");
        conf.setProperty("append", "true");
        conf.setProperty("suppress-console-output", "false");
        conf.setProperty("level", "INFO");
        conf.setProperty("output-format", "SIMPLE");

        return conf;
    }

}
