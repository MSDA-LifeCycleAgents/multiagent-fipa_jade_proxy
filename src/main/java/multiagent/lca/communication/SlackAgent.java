/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.lca.communication;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import multiagent.lca.configuration.Configuration;
import multiagent.lca.util.XmlParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Rogier
 */
public class SlackAgent extends Agent{
    private static final Logger logger = Logger.getLogger(SlackAgent.class.getName());
    
    @Override
    public void setup(){
        addBehaviour(
            new CyclicBehaviour(){
                @Override
                public void action(){ 
                    ACLMessage aclMessage = receive();
                    if(aclMessage != null && aclMessage.getPerformative() == ACLMessage.REQUEST){
                        String content = aclMessage.getContent();
                        
                        try {
                            Document xml = XmlParser.loadXMLFromString(content);
                            Element root = xml.getDocumentElement();
                            
                            String message = XmlParser.getString("content", root);
                            String channel = XmlParser.getString("channel", root);
                            
                            if(message != null)
                                sendMessage(message, channel);
                            else
                                logger.log(Level.WARNING, "Failed to send message: invalid request");
                            
                        } catch (ParserConfigurationException | SAXException | IOException ex) {
                            logger.log(Level.WARNING, "Failed to parse message: {0}", ex.getMessage());
                        }
                    }
                }
            }
        );
    }
    
    private void sendMessage(String message, String channel){
        Configuration config = Configuration.getInstance();
        String authToken = config.getProperty("slack.auth_token");
        
        if(channel == null)
            channel = config.getProperty("slack.default_channel");
        
        try {
            SlackSession session = SlackSessionFactory.createWebSocketSlackSession(authToken);
            session.connect();
            SlackChannel chan = session.findChannelByName(channel);
            session.sendMessage(chan, message);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send message: {0}", e.toString());
        }    
    }
}
