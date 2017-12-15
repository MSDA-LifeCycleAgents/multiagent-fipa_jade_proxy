/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.fipa_services.parser;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Rogier
 */
public class XmlParser {
    private static final Logger logger = Logger.getLogger(XmlParser.class.getName());
    
    public static ACLMessage parse(String xml, Envelope envelope) throws ParserConfigurationException, SAXException{
        ACLMessage message = parseBody(xml);
        message.setEnvelope(envelope);
        return message;
    }
    
    public static ACLMessage parseBody(String body) throws ParserConfigurationException, SAXException{
        try {
            Document xml = loadXMLFromString(body);
            Element root = xml.getDocumentElement();
            return mapElementToACL(root);
        } catch (IOException ex) {
            // this error should never be thrown, as there is no reading from disk involved
            logger.log(Level.WARNING, "IOException: {0}", ex.getMessage());
            return null;
        }
    }
    
    private static ACLMessage mapElementToACL(Element root){
        String performative = root.getAttribute("communicative-act");
        ACLMessage message = new ACLMessage(getPerformative(performative));
        
        // should only be one
        Element senderElement = (Element) root.getElementsByTagName("sender").item(0);
        AID sender = elementToAID((Element) senderElement.getFirstChild());
        message.setSender(sender);
        
        NodeList receiverElements = root.getElementsByTagName("receiver").item(0).getChildNodes();
        for(int i = 0; i < receiverElements.getLength(); i++){
            AID receiver = elementToAID((Element) receiverElements.item(i));
            message.addReceiver(receiver);
        }
        
        String content = getString("content", root);
        String language = getString("language", root);
        String ontology = getString("ontology", root);
        String protocol = getString("protocol", root);
        String conversationID = getString("conversation-id", root);
        
        message.setContent(content);
        message.setLanguage(language);
        message.setOntology(ontology);
        message.setProtocol(protocol);
        message.setConversationId(conversationID);
        
        return message;
    }
    
    private static AID elementToAID(Element identifier){
        String agentName = getString("name", identifier);
        NodeList addresses = identifier.getElementsByTagName("addresses");
        
        AID aid = new AID(agentName, true);
        
        for(int i = 0; i < addresses.getLength(); i++){
            String address = getString("url", (Element) addresses.item(i));
            aid.addAddresses(address);
        }
        
        return aid;
    }
    
    private static int getPerformative(String performative){
        String[] performatives = ACLMessage.getAllPerformativeNames();
        for(int i = 0; i < performatives.length; i++){
            if(performatives[i].equals(performative.toUpperCase()))
                return i;
        }
        return -1;
    }
    
    private static Document loadXMLFromString(String xml) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }
    
    private static String getString(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();
            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }
        return null;
    }
}
