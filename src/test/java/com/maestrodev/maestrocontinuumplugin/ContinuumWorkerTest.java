package com.maestrodev.maestrocontinuumplugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.simple.JSONObject;

/**
 * Unit test for simple App.
 */
public class ContinuumWorkerTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ContinuumWorkerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ContinuumWorkerTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testBuild() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ContinuumWorker continuumWorker = new ContinuumWorker();
        JSONObject fields = new JSONObject();
        fields.put("project", "HelloWorld");
        fields.put("project_group", "com.maestrodev");        
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("host", "localhost");
        fields.put("port", 9000);
        fields.put("username", "admin");        
        fields.put("password", "adm1n");        
        fields.put("web_path", "/continuum");
        fields.put("use_agent_facts", "false");
        fields.put("timeout", 180);
        fields.put("composition", "Test Composition");

        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
        assertNull(workitem.get("__error__"));
    }
   
    public void testBuildWithAgentFacts() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ContinuumWorker continuumWorker = new ContinuumWorker();
        JSONObject fields = new JSONObject();
        fields.put("project", "HelloWorld");
        fields.put("project_group", "com.maestrodev");        
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("host", "localhost");
        fields.put("port", 9000);
        fields.put("username", "admin");        
        fields.put("password", "adm1n");        
        fields.put("web_path", "continuum");
        fields.put("use_agent_facts", "true");
        fields.put("timeout", 180);
        fields.put("composition", "Test Composition");
        
        Map agentFacts = new JSONObject();
        
        agentFacts.put("app_scan", "V8.5");
        agentFacts.put("continuum_build_agent", "http://localhost:9001/continuum-buildagent/xmlrpc");
        
        fields.put("facts", agentFacts);

        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
        assertNull(workitem.get("__error__"));
    }
}
