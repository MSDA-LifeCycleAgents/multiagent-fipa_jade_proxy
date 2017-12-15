package multiagent.fipa_services.mtp;

import multiagent.fipa_services.JMDNSManager;
import jade.core.AID;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jade.lang.acl.TokenMgrError;
import jade.mtp.InChannel;
import jade.mtp.MTPException;
import jade.mtp.http.XMLCodec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import multiagent.fipa_services.parser.XmlParser;
import org.xml.sax.SAXException;

/**
 * A TcpServer that listens for incoming envelopes and forwards them
 * accordingly.
 *
 * @author Satia Herfert
 */
public class TcpServer {

    /**
     * The default port.
     */
    public static final int DEFAULT_PORT = 6789;

    /**
     * The XML parser implementation class.
     */
    public static final String XML_PARSER_CLASS = "org.apache.crimson.parser.XMLReaderImpl";
    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(TcpServer.class.getName());

    /**
     * The ServerSocket to accept connections.
     */
    private ServerSocket serverSocket;

    private final InChannel.Dispatcher dispatcher;
    private final Profile prfl;
    private final TcpAddress address;

    /**
     * Creates a TcpServer with the default port on the local IPv4 network.
     *
     * @param dispatcher the dispatcher
     * @param prfl the dispatcher
     * @throws SocketException when there is no local IPv4 network
     * @throws IOException if activation fails
     */
    public TcpServer(InChannel.Dispatcher dispatcher, Profile prfl) throws SocketException, IOException {
        this.dispatcher = dispatcher;
        this.prfl = prfl;
        this.address = new TcpAddress(JMDNSManager.getLocalIPv4Address(), DEFAULT_PORT);

        activate();
    }

    /**
     * Creates a TcpServer.
     *
     * @param dispatcher the dispatcher
     * @param prfl the dispatcher
     * @param address the address to use
     * @throws IOException if activation fails
     */
    public TcpServer(InChannel.Dispatcher dispatcher, Profile prfl, TcpAddress address) throws IOException {
        this.dispatcher = dispatcher;
        this.prfl = prfl;
        this.address = address;

        activate();
    }

    public TcpAddress getAddress() {
        return address;
    }

    /**
     * Activates this server, opens the socket. Called from the constructors.
     */
    private void activate() throws IOException {
        logger.log(Level.INFO, "TcpServer activating");
        try {
            serverSocket = new ServerSocket(address.getPortNo());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error initializing ServerSocket:", ex);
            // In the case of an exception here, we cannot function properly
            throw ex;
        }
        // Start thread accepting connections
        new Thread() {
            @Override
            public void run() {
                acceptConnections();
            }
        }.start();
    }

    /**
     * Deactivates this server, closes the socket.
     */
    public void deactivate() {
        logger.log(Level.INFO, "TcpServer terminating");
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not close server socket.", ex);
            }
        }
    }

    /**
     * Accepts connections, de-serializes the envelopes and dispatches them.
     */
    private void acceptConnections() {
        try {
            while (true) {
                final Socket socket = serverSocket.accept();
                logger.log(Level.INFO, "Accepted a new connection");
                // Start thread accepting connections
                new Thread() {
                    @Override
                    public void run() {
                        forward(socket);
                    }
                }.start();
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to deactivate): ", e);
        }
    }

    /**
     * De-serializes the envelopes and dispatches them.
     */
    private void forward(Socket socket) {
        Envelope env;
        String input;

        // XXX this is not robust!
        final String splitter = "</envelope>";

        // XXX This can destroy bit-efficient encoding. One should read
        // into a byte[] instead of char[]
        try {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                StringBuilder buffer = new StringBuilder();

                char[] segment = new char[4096];
                int charsRead;
                while ((charsRead = reader.read(segment, 0, segment.length)) != -1) {
                    buffer.append(segment, 0, charsRead);

                    int index;
                    while ((index = buffer.indexOf(splitter)) != -1) {
                        index += splitter.length();
                        StringReader envReader = new StringReader(buffer.substring(0, index));
                        XMLCodec xmlCodec = new XMLCodec(XML_PARSER_CLASS);
                        env = xmlCodec.parse(envReader);
                        logger.log(Level.INFO, "Decoded env: {0}", env);

                        int msgLen = env.getPayloadLength().intValue();
                        
                        // ! next line can get stuck if msgLen is not right.
                        while (buffer.length() < index + msgLen
                                && (charsRead = reader.read(segment, 0, segment.length)) != -1) {
                            buffer.append(segment, 0, charsRead);
                        }

                        if (buffer.length() < index + msgLen) {
                            logger.log(Level.WARNING, "Stream only contains {0} chars. Expected {1}",
                                    new Object[]{buffer.length(), index + msgLen});

                        } else {
                            String msgString = buffer.substring(index, index + msgLen);
                            try {
                                StringReader msgReader = new StringReader(msgString);
                                ACLMessage msg = ACLParser.create().parse(msgReader);
                                msgString = msg.toString();
                            } catch (ParseException ex) {
                                logger.log(Level.WARNING, "Could not parse ACL message: ", ex);
                            } catch (TokenMgrError e){
                                try {
                                    // this means it has encountered an unexpected token, like '<'. So we'll try to parse the message to XML.
                                    ACLMessage msg = XmlParser.parse(msgString, env);
                                    msgString = msg.toString();
                                } catch (ParserConfigurationException | SAXException ex) {
                                    logger.log(Level.WARNING, "Could not parse ACL message to XML: ", ex);
                                    return;
                                }
                            }

                            logger.log(Level.INFO, "Dispatching message string: {0}", msgString);
                            dispatcher.dispatchMessage(env, msgString.getBytes());
                        }
                        buffer.delete(0, index + msgLen);
                    }
                }
            } finally {
                socket.close();
                logger.log(Level.INFO, "Closed connection");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Socket io threw: ", e);
        } catch (MTPException e) {
            logger.log(Level.WARNING, "Envelope parser threw: ", e);
        }
    }
}
