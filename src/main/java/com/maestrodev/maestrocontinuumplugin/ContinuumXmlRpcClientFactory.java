package com.maestrodev.maestrocontinuumplugin;

import java.net.URL;

import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;

public class ContinuumXmlRpcClientFactory
{

 private static ContinuumXmlRpcClientFactory theInstance;
    
    public static ContinuumXmlRpcClientFactory getInstance() {
        
        if (theInstance == null) {            
            synchronized (ContinuumXmlRpcClientFactory.class) {
                if (theInstance == null)
                    theInstance = new ContinuumXmlRpcClientFactory();                
            }
        }
        return theInstance;
    }
    
    
    private ContinuumXmlRpcClientFactory() {
        
    }
    
    public ContinuumXmlRpcClient getClient(URL url, String username, String password) {        
        return new ContinuumXmlRpcClient( url, username, password);
    }
    
    
}
