package com.maestrodev.maestrocontinuumplugin;

import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.BuildDefinition;
import org.apache.maven.continuum.xmlrpc.project.BuildResult;
import org.apache.maven.continuum.xmlrpc.project.ContinuumProjectState;
import org.apache.maven.continuum.xmlrpc.project.Project;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroup;
import org.apache.maven.continuum.xmlrpc.project.ProjectSummary;
import org.fusesource.stomp.client.BlockingConnection;
import org.junit.Before;
import org.junit.Test;

import org.json.simple.JSONObject;

import com.maestrodev.StompConnectionFactory;

/**
 * Unit test for simple App.
 */
public class ContinuumWorkerTest 
{
    HashMap<String,Object> stompConfig;
    StompConnectionFactory stompConnectionFactory;       
    BlockingConnection blockingConnection;
    
    ContinuumXmlRpcClientFactory continuumXmlRpcClientFactory;
    ContinuumXmlRpcClient continuumXmlRpcClient;
    ContinuumWorker continuumWorker;

    
    @Before
    public void setUp() throws Exception {
        stompConfig = new HashMap<String,Object>();
        stompConfig.put("host", "localhost");
        stompConfig.put("port", "61613");
        stompConfig.put("queue", "test");
        
        // Setup the mock stomp connection
        stompConnectionFactory = mock(StompConnectionFactory.class);       
        blockingConnection = mock(BlockingConnection.class);
        when(stompConnectionFactory.getConnection(any(String.class), any(int.class))).thenReturn(blockingConnection);
        
        // Setup the mock continuum client
        continuumXmlRpcClientFactory = mock(ContinuumXmlRpcClientFactory.class);
        continuumXmlRpcClient = mock(ContinuumXmlRpcClient.class);
        when(continuumXmlRpcClientFactory.getClient(any(URL.class), any(String.class), any(String.class))).thenReturn(continuumXmlRpcClient);

        continuumWorker = new ContinuumWorker(stompConnectionFactory, continuumXmlRpcClientFactory);
        continuumWorker.setStompConfig(stompConfig);

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMavenProject () throws Exception 
    {
       List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
       ProjectGroup group = new ProjectGroup();
       group.setName("HelloWorld");
       group.setGroupId("com.maestrodev");
       projectGroups.add(group);

       when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);
      
      
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
    @SuppressWarnings("unchecked")
    @Test
    public void testBuild() throws Exception
    {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        ProjectGroup group = new ProjectGroup();
        group.setName("HelloWorld");
        group.setGroupId("com.maestrodev");
        
        ProjectSummary projectSummary = new ProjectSummary();
        projectSummary.setName("HelloWorld");
        List<ProjectSummary> projects = new ArrayList<ProjectSummary>();
        projects.add(projectSummary);
        group.setProjects(projects);
        
        projectGroups.add(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);
        
        Project project = new Project();
        project.setId(0);
        project.setName("HelloWorld");
        
        
        Project buildingProject = new Project();
        buildingProject.setId(0);
        buildingProject.setState(ContinuumProjectState.BUILDING);
        
        Project completedProject = new Project();
        completedProject.setId(0);
        completedProject.setState(ContinuumProjectState.OK);
        
        when(continuumXmlRpcClient.getProjectWithAllDetails(0)).thenReturn(project, buildingProject, completedProject);
        
        List<BuildDefinition> buildDefinitions = new ArrayList<BuildDefinition>();
        BuildDefinition buildDef = new BuildDefinition();
        buildDef.setId(1);
        buildDef.setDescription("Build Definition Generated By Maestro 4, task ID: 1");
        buildDefinitions.add(buildDef);
        project.setBuildDefinitions(buildDefinitions);

        BuildResult buildResult = new BuildResult();
        buildResult.setExitCode(0);
        when(continuumXmlRpcClient.getBuildOutput(any(int.class), any(int.class))).thenReturn("");
        when(continuumXmlRpcClient.getLatestBuildResult(0)).thenReturn(buildResult);
         
        
        JSONObject fields = new JSONObject();
        fields.put("group_name", "HelloWorld");
        fields.put("group_id", "com.maestrodev");        
        fields.put("project_name", "HelloWorld");
        fields.put("project_group", "com.maestrodev");        
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("host", "localhost");
        fields.put("port", 9000);
        fields.put("username", "admin");        
        fields.put("password", "adm1n");        
        fields.put("web_path", "/continuum");
        fields.put("composition", "Test Composition");
        fields.put("facts", new JSONObject());        
        fields.put("__context_outputs__", new JSONObject());
        
        JSONObject params = new JSONObject();
        params.put("composition_task_id", 1L);
        fields.put("params", params);
        
            
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
        
//        assertNotNull(continuumWorker.getField("__context_outputs__"));
        assertNull(continuumWorker.getField("__error__"),continuumWorker.getField("__error__"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithPreviousContextOutputs() throws Exception
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
   
    @SuppressWarnings("unchecked")
    @Test      
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
   
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
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
