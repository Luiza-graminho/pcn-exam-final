package br.edu.unijui.pcn.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Isadora Beckmann e Luiza Graminho
 */
public class IsolationCSVImporter {
    public static List<IsolationRecord> load(File[] files) throws InterruptedException {
        List<IsolationRecord> records = Collections.synchronizedList(new ArrayList<>());
        
        if (files == null || files.length == 0) {
            return records;
        }

        List<Thread> threads = new ArrayList<>();

        for (File file : files) {
            Thread t = new Thread(() -> {
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
                                records.add(record);
                            } catch (NumberFormatException nfe) {
                                System.err.println("Erro de conversão na linha: " + line + " -> " + nfe.getMessage());
                            }
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Erro ao ler o arquivo " + file.getName() + ": " + e.getMessage());
                }
            });
            
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
        
        return records;
    }
}