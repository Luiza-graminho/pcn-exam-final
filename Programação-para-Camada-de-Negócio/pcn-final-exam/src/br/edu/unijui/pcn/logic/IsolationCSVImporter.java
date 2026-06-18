package br.edu.unijui.pcn.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IsolationCSVImporter {
    public static List<IsolationRecord> load(File[] files) throws InterruptedException {
        List<IsolationRecord> records = Collections.synchronizedList(new ArrayList<>());
        
        // utilize threads para ler em paralelo o conteúdo (os índices de isolamento) de todos os arquivos recebidos como parâmetro
        // todas as threads devem armazenar o conteúdo lido à lista "records"
        // você poderá escolher a estratégia (uso da ApiExecutor ou criação ad hoc de threads) que julgar mais adequada
        
        return records;
    }
}
