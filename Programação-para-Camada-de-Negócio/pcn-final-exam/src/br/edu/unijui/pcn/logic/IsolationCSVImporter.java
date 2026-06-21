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

/**
 *
 * @author Isadora Beckmann e Luiza Graminho
 */
public class IsolationCSVImporter {

    public static List<IsolationRecord> load(File[] files) {
        List<IsolationRecord> sharedRecords = Collections.synchronizedList(new ArrayList<>());

        if (files == null || files.length == 0) {
            return sharedRecords;
        }

        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numCores);

        for (File file : files) {
            executor.submit(() -> parseFile(file, sharedRecords));
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                System.err.println("Aviso: O tempo limite para leitura dos arquivos expirou.");
            }
        } catch (InterruptedException e) {
            System.err.println("A leitura dos arquivos foi interrompida.");
            Thread.currentThread().interrupt();
        }

        return sharedRecords;
    }

    private static void parseFile(File file, List<IsolationRecord> targetList) {
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
                        System.err.println("Erro de formato na linha do arquivo " + file.getName() + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro crítico ao ler o arquivo " + file.getName() + ": " + e.getMessage());
        }
    }
}