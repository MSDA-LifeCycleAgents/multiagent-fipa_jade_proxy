/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.fipa_services;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author Rogier
 */
public abstract class HeartbeatBehaviour extends TickerBehaviour{
    private final AID extAgent;
    private final int timeout;
    
    public HeartbeatBehaviour(Agent a, long period, AID extAgent) {
        super(a, period);
        this.extAgent = extAgent;
        this.timeout = 4000;
    }
    
    public HeartbeatBehaviour(Agent a, long period, AID extAgent, int timeout){
        super(a, period);
        this.extAgent = extAgent;
        this.timeout = timeout;
    }

    @Override
    protected void onTick() {
        String[] addresses = extAgent.getAddressesArray();
        if(addresses.length < 1)
            return;
        
        boolean[] availables = new boolean[addresses.length];
        
        for(int i = 0; i < addresses.length; i++){
            String[] ipParts = addresses[i].replaceAll("http://|tcp://", "").split(":");
            InetSocketAddress socketAddress = new InetSocketAddress(ipParts[0], Integer.valueOf(ipParts[1]));
            
            availables[i] = isAvailable(socketAddress);    
            if(availables[i])
                break;
        }
        
        if(!containsTrue(availables)){
            onUnavailable();
        }
    }
    
    private boolean isAvailable(InetSocketAddress socketAddress){
        try{
            Socket socket = new Socket();
            socket.connect(socketAddress, timeout);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean containsTrue(boolean[] array){
        for(boolean b : array) if(b) return true;
        return false;
    }
    
    protected abstract void onUnavailable();
    
}
