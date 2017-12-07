/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiagent.lca.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rogier
 */
public class Configuration {
    private static final Logger logger = Logger.getLogger(Configuration.class.getName());
    
    private static Configuration configuration;
    private final Properties properties;
    private static final String PROP_FILE_NAME = "config.properties";
    
    private Configuration(){
        properties = new Properties();
        loadProperties();
    }
    
    public static Configuration getInstance(){
        if(configuration == null){
            configuration = new Configuration();
        }
        return configuration;
    }
    
    private void loadProperties(){
        try{
            InputStream input = new FileInputStream(PROP_FILE_NAME);
            properties.load(input);
        }catch(IOException e){
            logger.log(Level.WARNING, "Failed to load configuration properties: {0}", e.getMessage());
        }
    }
    
    public String getProperty(String property){
        return properties.getProperty(property);
    }
}
