/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.lca;

import jade.core.AID;
import jade.core.Agent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rogier
 */
public abstract class SensorAgent extends Agent {
    private static final Logger logger = Logger.getLogger(SensorAgent.class.getName());
    protected double min;
    protected double max;
    protected double reading;
    private String id;
    private String name;
    private String unit;
    private long pollTime = 2000;
    private AID decisionAgent;
    
    public void setup(String name, double min, double max, String unit){
        this.name = name;
        this.min = min;
        this.max = max;
        this.unit = unit;
    }
    
    protected double scaled(double value){   
        double minRange = -1;
        double maxRange = 1;
        
        // Need to add: out of bounds points, Avoiding symmetry. According to Medical paper VII-F/I
        return ((maxRange - minRange) * (value - min) / max - min ) + minRange;
    }
    
    protected String tag(String tag, String value){
        return "<" + tag + ">" + value + "</" + tag + ">";
    }
    
    protected String xmlMsg(){
        // NYI
        return null;
    }
    
    protected void report(String message){
        logger.log(Level.INFO, "{0}: {1}", new Object[]{name, message});
    }
    
    protected abstract double performMeasurement();
    
}
