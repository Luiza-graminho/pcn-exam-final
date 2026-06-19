package br.edu.unijui.pcn.logic;

/**
 * Esta classe implementa a lógica de negócio para ler e gerar um arquivo XML
 * com os dados de isolamento social.
 *
 * @author Isadora Beckmann e Luiza Graminho
 */
import br.edu.unijui.pcn.utils.XMLHandler;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLTransformer {

    private final DBManager dbManager;

    public XMLTransformer(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Exporta todos os dados de isolamento social carregados no banco de dados
     * para um arquivo com nome especificado no parâmetro do método
     *
     * @param fileName indica o nome do arquivo ao qual devem ser exportados os
     * dados.
     */
    public void export(String fileName) {

        List<IsolationRecord> records = dbManager.getAllRecords();

        Document doc = XMLHandler.newDocument();

        Element root = doc.createElement("isolation-indexes");
        doc.appendChild(root);

        int id = 1;

        for (IsolationRecord record : records) {

            Element covid = doc.createElement("covid");

            covid.setAttribute("id", String.valueOf(id++));
            covid.setAttribute("city", record.city());
            covid.setAttribute("state-name", record.state());
            covid.setAttribute("state-acronym", record.stateAcronym());
            covid.setAttribute("date", record.date());
            covid.setAttribute("index", String.valueOf(record.index()));

            root.appendChild(covid);
        }

        XMLHandler.writeXmlFile(doc, fileName + ".xml");
    }
}
