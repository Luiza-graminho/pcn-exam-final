package br.edu.unijui.pcn.utils;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Classe utilitária responsável por operações relacionadas 
 * à manipulação de documentos XML
 *
 * @author Isadora Beckmann e Luiza Graminho
 */
public class XMLHandler {
    
    private static final Logger logger = Logger.getLogger(XMLHandler.class.getName());

    /**
     * Criação de um novo documento XML vazio.
     * 
     * @return Documento XML inicializado.
     */
    public static Document newDocument() {
        try {

            // Cria a fábrica responsável pela construção de documentos XML
            logger.info("Criando novo documento XML");
            
            DocumentBuilderFactory dbF = DocumentBuilderFactory.newInstance();
            
            logger.info("Documento XML criado com sucesso");
             
            return dbF.newDocumentBuilder().newDocument();
            
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "Erro ao criar documento XML", ex);
            return null;
        }
    }

    /**
     * Obtenção de um objeto XPath Expression
     */
    public static XPathExpression getXPathExpression(String xpath) {
        try {

            // Cria um mecanismo XPath para consultas em documentos XML
            
            logger.info("Compilando expressão XPath: "+ xpath);
            XPath localxpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = localxpath.compile(xpath);
            logger.info("Expressão XPath compilada com sucesso");
            
            return expr;

        } catch (XPathExpressionException ex) {
            logger.log(Level.WARNING, "Erro ao compilar expressão XPath");
            return null;
        }
    }

    /**
     * Converte um documento XML para sua representação textual
     * 
     * @return XML em formato String
    */
    public static String xml2String(Document doc) {
        try {
            
            logger.info("Convertendo XML para String");
            
            // Converte o documento XML para uma fonte de transformação
            
            Source source = new DOMSource(doc);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Result result = new StreamResult(stream);
            
            // Configura o tranformador responsável pela serialização do XML
            
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.transform(source, result);
            
            logger.info("Conversão do XML para String concluída");
            
            return stream.toString();

        } catch (TransformerFactoryConfigurationError | IllegalArgumentException | TransformerException ex) {
            logger.log(Level.SEVERE, "Erro ao converser XML para String");
            return null;
        }
    }

    /**
     * Converte uma String contendo XML em um objeto Document
     * 
     * @param text Conteúdo XML
     * @return Documento XML gerado
     */
    public static Document string2Xml(String text) {
        try {

            logger.info("Convertendo String para documento XML");
            
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            
            logger.info("Documento XML criado a partir da String");
            
            // Converte o texto XML em um documento DOM
            return builder.parse(new ByteArrayInputStream(text.getBytes()));

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            logger.log(Level.SEVERE, "Erro ao converter String para XML", ex);
            return null;
        }
    }

    // Leitura de um Arquivo XML
    // Montar um objeto Document a partir de um arquivo texto XML
    public static Document readXmlFile(String filename) {
        try {

            logger.info("Lendo arquivo XML: "+ filename);
            
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            
            logger.info("Arquivo XML carregado com sucesso");
            
            // Leitura do arquivo XML informado
            return builder.parse(filename);

        } catch (IOException | SAXException | ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "Erro ao ler arquivo XML");
            return null;
        }
    }

   // Transformar um objeto Document em um arquivo texto XML
    public static void writeXmlFile(Document doc, String filename) {
        try {
            
            logger.info("Iniciando gravação do arquivo XML: "+ filename );
            
            Source source = new DOMSource(doc);
            // Cria o arquivo físico onde o XML será armazenado
            File file = new File(filename);
            Result result = new StreamResult(file);

            // Configura o transformador responsável pela gravação do XML
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.transform(source, result);
        } catch (TransformerException | TransformerFactoryConfigurationError ex) {
            logger.log(Level.SEVERE, "Erro ao gravar arquivo XML", ex);
        }
    }
    
    // Buscar o valor de uma tag ou atributo usando XPath
    public static String getXMLValue(Document doc, String xpath) {
        try {
            logger.info("Buscando valor XPath: "+ xpath);
            XPathExpression exp = XMLHandler.getXPathExpression(xpath);
            if (exp == null) {
                logger.warning("Expressão XPath inválida");
                return null;
            }
            
            logger.info("Consulta XPath executada com sucesso");
            // Retorna o resultado diretamente como uma String avaliada pelo XPath
            return exp.evaluate(doc); 
            
        } catch (XPathExpressionException ex) {
            logger.log(Level.WARNING, "Erro ao executar consulta XPath", ex);
            return null;
        }
    }
}
