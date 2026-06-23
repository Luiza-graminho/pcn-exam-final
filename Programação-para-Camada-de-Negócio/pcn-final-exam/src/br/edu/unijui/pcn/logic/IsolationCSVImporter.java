package br.edu.unijui.pcn.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsável pela leitura e importação dos arquivos CSV contendo os registros de isolamento social
 * A leitura é realizada de forma paralela utilizando múltiplas threads
 * @author Isadora Beckmann e Luiza Graminho
 */
public class IsolationCSVImporter {
    private static final Logger logger = Logger.getLogger(IsolationCSVImporter.class.getName());
   
    // Realiza a leitura de um conjunto de arquivos CSV e retorna os registros encontrados
    public static List<IsolationRecord> load(File[] files) {
        logger.log(Level.INFO, "Iniciando importacao de {0} arquivo(s).", files.length);
        List<IsolationRecord> sharedRecords = Collections.synchronizedList(new ArrayList<>());

        if (files == null || files.length == 0) {
            return sharedRecords;
        }

        int numCores = Runtime.getRuntime().availableProcessors();
        logger.log(Level.INFO, "Pool de threads criado com {0} threads", numCores);
        
        ExecutorService executor = Executors.newFixedThreadPool(numCores);
        for (File file : files) {
            logger.log(Level.INFO, "Arquivo enviado para processamento: {0}", file.getName());
            executor.submit(() -> parseFile(file, sharedRecords));
        }
        executor.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                logger.warning("Tempo limite excedido durante a leitura dos arquivos");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "A leitura dos arquivos foi interrompida", e);
            Thread.currentThread().interrupt();
        }

        return sharedRecords;
    }

    private static void parseFile(File file, List<IsolationRecord> targetList) {
        
        logger.info("Iniciando leitura dos arquivos");
        
        // Abre o arquivo para leitura linha por linha
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] tokens = line.split("[,;]");
                if (tokens.length >= 5) {
                    try {
                        String state = tokens[0].trim(); 
                        String city = tokens[1].trim(); 
                        String indexStr = tokens[2].trim().replace(",", ".");
                        String date = tokens[3].trim(); 
                        String stateAcronym = tokens[4].trim();

                        double index = Double.parseDouble(indexStr);

                        IsolationRecord record = new IsolationRecord(state, stateAcronym, city, index, date);
                        targetList.add(record);

                    } catch (NumberFormatException nfe) {
                        logger.log(Level.WARNING, "Erro de conversao no arquivo {0}. Linha ignorada: {1}", new Object[]{file.getName(), line});
                    }
                }
            }
            logger.log(Level.INFO, "Leitura concluida para o arquivo: {0}", file.getName());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao ler arquivo: "+ file.getName(), e);
        }
    }
}