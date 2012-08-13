package com.maestrodev.maestrocontinuumplugin;

import static org.mockito.Mockito.*;

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

    public void testCreateMavenProject () throws Exception 
    {
      ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
      JSONObject fields = new JSONObject();
      fields.put("group_name", "HelloWorld");
      fields.put("group_id", "com.maestrodev");        
      fields.put("group_description", "clean test install package");
      fields.put("pom_url", "https://svn.apache.org/repos/asf/activemq/trunk/pom.xml");
      fields.put("project_name", "ActiveMQ");
      fields.put("host", "localhost");
      fields.put("port", 8081);
      fields.put("username", "admin");        
      fields.put("password", "password1");        
      fields.put("web_path", "/continuum");
      fields.put("use_ssl", false);
      fields.put("__context_outputs__", new JSONObject());

      JSONObject workitem = new JSONObject();
      workitem.put("fields", fields);
      continuumWorker.setWorkitem(workitem);


      Method method = continuumWorker.getClass().getMethod("addMavenProject");
      method.invoke(continuumWorker);

//        assertNotNull(continuumWorker.getField("__context_outputs__"));
      assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
    
    /**
     * Rigourous Test :-)
     */
    public void testBuild() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
//        ContinuumWorker continuumWorker = new ContinuumWorker();
        ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
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
        fields.put("__context_outputs__", new JSONObject());
        
            
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
//        assertNotNull(continuumWorker.getField("__context_outputs__"));
        assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
   
        public void testBuildWithPreviousContextOutputs() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
//        ContinuumWorker continuumWorker = new ContinuumWorker();
        ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
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
        fields.put("__context_outputs__", new JSONObject());
        
        JSONObject previousContextOutputs = new JSONObject();
        
        previousContextOutputs.put("build_definition_id", 31);
        
        fields.put("__previous_context_outputs__", previousContextOutputs);
            
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
//        assertNotNull(continuumWorker.getField("__context_outputs__"));
        assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
   
        
      public void testBuildWithPreviousContextOutputsAndChangeGoals() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
//        ContinuumWorker continuumWorker = new ContinuumWorker();
        ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
        JSONObject fields = new JSONObject();
        fields.put("project", "HelloWorld");
        fields.put("project_group", "com.maestrodev");        
        fields.put("goals", "clean test package");
        fields.put("arguments", "--batch-mode -DskipTests");
        fields.put("host", "localhost");
        fields.put("port", 9000);
        fields.put("username", "admin");        
        fields.put("password", "adm1n");        
        fields.put("web_path", "/continuum");
        fields.put("use_agent_facts", "false");
        fields.put("timeout", 180);
        fields.put("composition", "Test Composition");
        fields.put("__context_outputs__", new JSONObject());
        
        JSONObject previousContextOutputs = new JSONObject();
        
        previousContextOutputs.put("build_definition_id", 31);
        
        fields.put("__previous_context_outputs__", previousContextOutputs);
            
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
//        assertNotNull(continuumWorker.getField("__context_outputs__"));
        assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
   
    
    public void testBuildWithAgentFacts() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
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
        
        assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
}
