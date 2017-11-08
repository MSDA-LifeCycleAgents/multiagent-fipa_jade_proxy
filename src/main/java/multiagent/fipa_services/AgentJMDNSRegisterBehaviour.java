package multiagent.fipa_services;

import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
import java.util.Arrays;
import java.util.Map;

/**
 * The AMSSubscriber is a behaviour that subscribes to the AMS to receive notifications about platform-wide events.
 * The installHandlers method must be redefined to define the handlers for events the agent executing this behaviour is interested in.
 *
 * The AgentJMDNSRegisterBehaviour takes care that
 * every time an agent gets "born" it is registered via JMDNS, and when it
 * "dies" it is unregistered.
 *
 * @author Satia Herfert
 */
public class AgentJMDNSRegisterBehaviour extends AMSSubscriber {
    
    JMDNSManager jmdnsManager;

    public AgentJMDNSRegisterBehaviour(JMDNSManager jmdnsManager) {
        this.jmdnsManager = jmdnsManager;
    }
    
    private String getIpFromAddress(String address){
        String ip = address.replaceFirst("tcp://", "");
        ip = ip.replaceFirst("http://", "");
        return ip.split(":")[0];
    }

    protected void installHandlers(Map handlers) {

        AMSSubscriber.EventHandler addAgentEH = new AMSSubscriber.EventHandler() {
            public void handle(Event event) {
                String hostAddress = jmdnsManager.getInetAddress().getHostAddress();      
                BornAgent ba = (BornAgent) event;

                // Only register local agents, as external agents are already found via mDNS
                for(String address : ba.getAgent().getAddressesArray()){
                    if(getIpFromAddress(address).equals(getIpFromAddress(hostAddress))){
                        jmdnsManager.registerJadeAgent(ba.getAgent().getName());
                        break;
                    }
                }     
            }
        };

        AMSSubscriber.EventHandler removeAgentEH = new AMSSubscriber.EventHandler() {
            public void handle(Event event) {
                DeadAgent da = (DeadAgent) event;
                jmdnsManager.unregisterJadeAgent(da.getAgent().getName());
            }
        };

        handlers.put(IntrospectionVocabulary.BORNAGENT, addAgentEH);
        handlers.put(IntrospectionVocabulary.DEADAGENT, removeAgentEH);
//          handlers.put(IntrospectionVocabulary.SUSPENDEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.RESUMEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.FROZENAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.THAWEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.MOVEDAGENT, jpa);
    }
}
