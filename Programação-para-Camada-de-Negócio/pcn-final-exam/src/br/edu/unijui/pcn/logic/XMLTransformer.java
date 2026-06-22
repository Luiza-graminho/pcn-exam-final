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
import java.util.logging.Level;
import java.util.logging.Logger;

public class XMLTransformer {

    private static final Logger logger
            = Logger.getLogger(XMLTransformer.class.getName());

    // Responsável pelo acesso aos dados armazenados no banco
    private final DBManager dbManager;

    // Inicializa o transformador XML
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

        logger.info("Iniciando exportação dos registros para XML.");

        try {
            // Recupera todos os registros armazenados no banco de dados
            List<IsolationRecord> records = dbManager.getAllRecords();

            logger.info("Quantidade de registros recuperados: " + records.size());

            // Cria um novo documento XML vazio em memória
            Document doc = XMLHandler.newDocument();

            logger.info("Documento XML criado em memória");

            // Cria o elemento raiz que conterá todos os registros
            Element root = doc.createElement("isolation-indexes");
            doc.appendChild(root);

            int id = 1;

            // Percorre todos os registros recuperados para gerar os elementos XML
            for (IsolationRecord record : records) {

                // Cria um elemento representando um registro de isolamento social
                Element covid = doc.createElement("covid");

                covid.setAttribute("id", String.valueOf(id++));
                covid.setAttribute("city", record.city());
                covid.setAttribute("state-name", record.state());
                covid.setAttribute("state-acronym", record.stateAcronym());
                covid.setAttribute("date", record.date());
                covid.setAttribute("index", String.valueOf(record.index()));

                root.appendChild(covid);

                logger.fine("Exportando registro da cidade: " + record.city() + " - " + record.stateAcronym());
            }

            logger.info("Gravando arquivo XML: " + fileName + ".xml");

            XMLHandler.writeXmlFile(doc, fileName + ".xml");

            logger.info("Arquivo XML gerado com sucesso.");
        } catch (Exception e){
        logger.log(Level.SEVERE, "Erro durante a exportação dos dados para XML", e);
        throw new RuntimeException("Falha ao gerar arquivo XML, e");
        }
    }
}
