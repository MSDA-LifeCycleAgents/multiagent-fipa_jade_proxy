/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.fipa_services.parser;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.xml.sax.SAXException;

/**
 *
 * @author Rogier
 */
public class XmlParserTest extends TestCase{
    private static final String XML = "<?xml version=\"1.0\"?>\n" +
"<fipa-message communicative-act=\"request\">\n" +
"	<sender><agent-identifier><name>See-Sharp-Agent</name><addresses><url>tcp://192.168.178.14:1234</url></addresses></agent-identifier></sender>\n" +
"	<receiver><agent-identifier><name>MailAgent@192.168.178.14:1099/JADE</name><addresses><url>tcp://192.168.178.14:1099</url></addresses></agent-identifier></receiver>\n" +
"	<content>\n" +
"		<message>\n" +
"			<content>Insert e-mail body here</content>\n" +
"			<subject>Insert subject here</subject>	\n" +
"			<to>receiver@email.com</to>\n" +
"		</message>\n" +
"	</content>\n" +
"	<language>fipa-sl0</language>\n" +
"	<ontology>fipa-agent-management</ontology>\n" +
"	<protocol>fipa-request</protocol>\n" +
"	<conversation-id>17</conversation-id>\n" +
"</fipa-message>";
    
    /*
    Just testing to see if the parser doesn't throw any exception.
    Comparing parsed content might be added later.
    */
    public void testParser() throws ParserConfigurationException, SAXException{
        ACLMessage message = XmlParser.parseBody(XML);
        
        assertNotNull(message.getPerformative());
        
        assertNotNull(message.getSender());
        assertNotNull(message.getSender().getName());
        assertEquals(message.getSender().getAddressesArray().length, 1);
        
        int numberOfReceivers = 0;
        Iterator receiverIt = message.getAllReceiver();
        
        while(receiverIt.hasNext()){
            AID receiver = (AID) receiverIt.next();
            assertNotNull(receiver.getName());
            assertEquals(receiver.getAddressesArray().length, 1);
            numberOfReceivers++;
        }
        assertEquals(numberOfReceivers, 1);
        
        assertNotNull(message.getContent());
        assertNotNull(message.getLanguage());
        assertNotNull(message.getOntology());
        assertNotNull(message.getProtocol());
        assertNotNull(message.getConversationId());
    }
}
