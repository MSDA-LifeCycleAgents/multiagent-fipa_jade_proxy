/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.lca.communication;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
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
public class MailAgent extends Agent{
    private static final Logger logger = Logger.getLogger(MailAgent.class.getName());
    
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
                            String subject = XmlParser.getString("subject", root);
                            String to = XmlParser.getString("to", root);
                            
                            if(message != null && to != null)
                                sendMail(message, subject, to);
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
    
    private void sendMail(String message, String subject, String to){
        Configuration config = Configuration.getInstance();
        String host = config.getProperty("mail.host");
        String from = config.getProperty("mail.from");
        String password = config.getProperty("mail.password");
        String port = config.getProperty("mail.port");
        
        if(host == null || from == null){
            logger.log(Level.WARNING, "Failed to send message: Mail configuration not present");
            return;
        }
        
        if(subject == null)
            subject = config.getProperty("mail.default_subject");
        
        Properties sysProps = System.getProperties();
        sysProps.setProperty("mail.smtp.auth", "true");
	sysProps.setProperty("mail.smtp.starttls.enable", "true");
        sysProps.setProperty("mail.smtp.host", host);
        sysProps.setProperty("mail.smtp.port", port);
        
        Session session = Session.getInstance(sysProps, new Authenticator(){
            @Override
            protected PasswordAuthentication  getPasswordAuthentication(){
                return new PasswordAuthentication(from, password);
            }
        });
        
        try{
            MimeMessage mime = new MimeMessage(session);
            
            mime.setFrom(new InternetAddress(from));
            mime.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mime.setSubject(subject);
            mime.setText(message);
            
            Transport.send(mime);
        }catch(MessagingException e){
            logger.log(Level.WARNING, "Failed to send message: {0}", e.toString());
        }
    }
}
