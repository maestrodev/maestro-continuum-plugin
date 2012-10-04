package com.maestrodev.maestrocontinuumplugin;

import com.maestrodev.StompConnectionFactory;
import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.AddingResult;
import org.apache.maven.continuum.xmlrpc.project.BuildDefinition;
import org.apache.maven.continuum.xmlrpc.project.BuildResult;
import org.apache.maven.continuum.xmlrpc.project.ContinuumProjectState;
import org.apache.maven.continuum.xmlrpc.project.Project;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroup;
import org.apache.maven.continuum.xmlrpc.project.ProjectSummary;
import org.fusesource.stomp.client.BlockingConnection;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for simple App.
 */
public class ContinuumWorkerTest {
    HashMap<String, Object> stompConfig;
    StompConnectionFactory stompConnectionFactory;
    BlockingConnection blockingConnection;

    ContinuumXmlRpcClientFactory continuumXmlRpcClientFactory;
    ContinuumXmlRpcClient continuumXmlRpcClient;
    ContinuumWorker continuumWorker;


    @Before
    public void setUp() throws Exception {
        stompConfig = new HashMap<String, Object>();
        stompConfig.put("host", "localhost");
        stompConfig.put("port", "61613");
        stompConfig.put("queue", "test");

        // Setup the mock stomp connection
        stompConnectionFactory = mock(StompConnectionFactory.class);
        blockingConnection = mock(BlockingConnection.class);
        when(stompConnectionFactory.getConnection(Matchers.any(String.class),
                Matchers.any(int.class))).thenReturn(blockingConnection);

        // Setup the mock continuum client
        continuumXmlRpcClientFactory = mock(ContinuumXmlRpcClientFactory.class);
        continuumXmlRpcClient = mock(ContinuumXmlRpcClient.class);
        when(continuumXmlRpcClientFactory.getClient(Matchers.any(URL.class), Matchers.any(String.class),
                Matchers.any(String.class))).thenReturn(continuumXmlRpcClient);

        continuumWorker = new ContinuumWorker(stompConnectionFactory,
                continuumXmlRpcClientFactory);
        continuumWorker.setStompConfig(stompConfig);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMavenProject() throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        ProjectGroup group = new ProjectGroup();
        group.setName("HelloWorld");
        group.setGroupId("com.maestrodev");
        projectGroups.add(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);


        JSONObject fields = createContinuumFields();
        fields.put("group_name", "HelloWorld");
        fields.put("group_id", "com.maestrodev");
        fields.put("group_description", "clean test install package");
        fields.put("pom_url", "https://svn.apache.org/repos/asf/activemq/trunk/pom.xml");

        createWorkItem(fields);


        Method method = continuumWorker.getClass().getMethod("addMavenProject");
        method.invoke(continuumWorker);

        assertNotNull(continuumWorker.getField("__context_outputs__"));
        assertNull(continuumWorker.getField("__error__"), continuumWorker.getField("__error__"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDuplicateMavenProjectWithPom() throws Exception {
        String projectPom = "https://raw.github.com/etiennep/centrepoint/master/pom.xml";
        ProjectSummary project = createProject("com.maestrodev", "HelloWorld", 1);

        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        ProjectGroup group = new ProjectGroup();
        group.setId(1);
        group.setName("HelloGroupWorld");
        group.setGroupId("com.maestrodev");
        group.addProject(project);
        projectGroups.add(group);
        project.setProjectGroup(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);

        ProjectSummary duplicateProject = new ProjectSummary();
        duplicateProject.setId(0);
        duplicateProject.setName("HelloWorld");
        duplicateProject.setProjectGroup(group);

        AddingResult duplicateResult = mockProjectAddition(projectPom, project);
        duplicateResult.addError("Trying to add duplicate projects in the same project group");
        when(continuumXmlRpcClient.getProjectGroup(1)).thenReturn(group);

        JSONObject fields = createContinuumFields();
        fields.put("pom_url", projectPom);

        createWorkItem(fields);

        Method method = continuumWorker.getClass().getMethod("addMavenProject");
        method.invoke(continuumWorker);
        JSONObject output = (JSONObject) continuumWorker.getFields().get("__context_outputs__");
        assertThat((Integer) output.get("continuum_project_id"), is(equalTo(project.getId())));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMavenProjectWithPom() throws Exception {

        String projectPom = "https://raw.github.com/etiennep/centrepoint/master/pom.xml";

        ProjectSummary project = createProject("com.maestrodev", "HelloWorld", 1);

        mockProjectAddition(projectPom, project);

        JSONObject fields = createContinuumFields();
        fields.put("pom_url", projectPom);

        createWorkItem(fields);

        Method method = continuumWorker.getClass().getMethod("addMavenProject");
        method.invoke(continuumWorker);
        JSONObject output = (JSONObject) continuumWorker.getFields().get("__context_outputs__");
        assertThat((Integer) output.get("continuum_project_id"), is(equalTo(project.getId())));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));

    }

    private void setupBuildProjectMocks(int projectId, int buildDefId)
            throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        ProjectGroup group = new ProjectGroup();
        group.setName("HelloWorld");
        group.setGroupId("com.maestrodev");

        ProjectSummary projectSummary = new ProjectSummary();
        projectSummary.setName("HelloWorld");
        projectSummary.setId(projectId);
        List<ProjectSummary> projects = new ArrayList<ProjectSummary>();
        projects.add(projectSummary);
        group.setProjects(projects);

        projectGroups.add(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);

        List<BuildDefinition> buildDefinitions = new ArrayList<BuildDefinition>();
        BuildDefinition buildDef = new BuildDefinition();
        buildDef.setId(buildDefId);
        buildDef.setDescription("Build Definition Generated By Maestro 4, task ID: 1");
        buildDefinitions.add(buildDef);

        Project project = new Project();
        project.setId(projectId);
        project.setName("HelloWorld");
        project.setBuildDefinitions(buildDefinitions);

        Project buildingProject = new Project();
        buildingProject.setId(projectId);
        buildingProject.setState(ContinuumProjectState.BUILDING);
        buildingProject.setBuildDefinitions(buildDefinitions);

        Project completedProject = new Project();
        completedProject.setId(projectId);
        completedProject.setState(ContinuumProjectState.OK);
        buildingProject.setBuildDefinitions(buildDefinitions);

        when(continuumXmlRpcClient.getProjectWithAllDetails(projectId)).thenReturn(project, buildingProject, completedProject);

        BuildResult buildResult = new BuildResult();
        buildResult.setExitCode(0);
        when(continuumXmlRpcClient.getBuildOutput(Matchers.any(int.class), Matchers.any(int.class))).thenReturn("");
        when(continuumXmlRpcClient.getLatestBuildResult(projectId)).thenReturn(buildResult);
    }

    /**
     * Rigourous Test :-)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuild() throws Exception {
        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId);


        JSONObject fields = createContinuumFields();
        fields.put("group_name", "HelloWorld");
        fields.put("group_id", "com.maestrodev");
        fields.put("project_name", "HelloWorld");
        fields.put("project_group", "com.maestrodev");
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("facts", new JSONObject());
        fields.put("composition", "Test Composition");

        JSONObject params = new JSONObject();
        params.put("composition_task_id", 1L);
        fields.put("params", params);


        createWorkItem(fields);


        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);

        assertThat((Integer) ((JSONObject) continuumWorker.getFields().get("__context_outputs__")).get("build_definition_id"), is(buildDefId));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));
    }

    /**
     * Test a build step that finds a project ID in the context data.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithProjectIdInContext() throws Exception {
        int projectId = 8;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId);

        JSONObject fields = createContinuumFields();
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("composition", "Test Composition");
        fields.put("facts", new JSONObject());

        JSONObject params = new JSONObject();
        params.put("composition_task_id", 1L);
        fields.put("params", params);

        JSONObject contextOutputs = new JSONObject();
        contextOutputs.put("continuum_project_id", new Long(projectId));
        fields.put("__context_outputs__", contextOutputs);

        createWorkItem(fields);

        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);

        assertThat((Integer) ((JSONObject) continuumWorker.getFields().get("__context_outputs__")).get("build_definition_id"), is(buildDefId));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithPreviousContextOutputs() throws Exception {

        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId);


        JSONObject fields = createContinuumFields();
        fields.put("group_name", "HelloWorld");
        fields.put("group_id", "com.maestrodev");
        fields.put("project_name", "HelloWorld");
        fields.put("project_group", "com.maestrodev");
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("composition", "Test Composition");
        fields.put("facts", new JSONObject());

        JSONObject previousContextOutputs = new JSONObject();

        previousContextOutputs.put("build_definition_id", buildDefId);

        fields.put("__previous_context_outputs__", previousContextOutputs);

        JSONObject params = new JSONObject();
        params.put("composition_task_id", 1L);
        fields.put("params", params);

        createWorkItem(fields);


        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);

        assertThat((Integer) ((JSONObject) continuumWorker.getFields().get("__context_outputs__")).get("build_definition_id"), is(buildDefId));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithPreviousContextOutputsAndChangeGoals() throws Exception {
        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId);


        JSONObject fields = createContinuumFields();
        fields.put("group_name", "HelloWorld");
        fields.put("group_id", "com.maestrodev");
        fields.put("project_name", "HelloWorld");
        fields.put("project_group", "com.maestrodev");
        fields.put("goals", "clean test package");
        fields.put("arguments", "--batch-mode -DskipTests");
        fields.put("composition", "Test Composition");
        fields.put("facts", new JSONObject());

        JSONObject previousContextOutputs = new JSONObject();

        previousContextOutputs.put("build_definition_id", buildDefId);

        fields.put("__previous_context_outputs__", previousContextOutputs);
        JSONObject params = new JSONObject();
        params.put("composition_task_id", 1L);
        fields.put("params", params);

        createWorkItem(fields);


        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);

        assertThat((Integer) ((JSONObject) continuumWorker.getFields().get("__context_outputs__")).get("build_definition_id"), is(buildDefId));
        assertThat(continuumWorker.getField("__error__"), is(nullValue()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testBuildWithAgentFacts() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ContinuumWorker continuumWorker = mock(ContinuumWorker.class);
        JSONObject fields = createContinuumFields();
        fields.put("project", "HelloWorld");
        fields.put("project_group", "com.maestrodev");
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("composition", "Test Composition");

        Map agentFacts = new JSONObject();

        agentFacts.put("app_scan", "V8.5");
        agentFacts.put("continuum_build_agent", "http://localhost:9001/continuum-buildagent/xmlrpc");

        fields.put("facts", agentFacts);

        createWorkItem(continuumWorker, fields);

        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);

        assertNull(continuumWorker.getField("__error__"), continuumWorker.getField("__error__"));
    }

    private static ProjectGroup createProjectGroup(String groupId, ProjectSummary project, int id) {
        ProjectGroup group = new ProjectGroup();
        group.setId(id);
        group.setName(groupId);
        group.setGroupId(groupId);
        group.addProject(project);
        project.setProjectGroup(group);
        return group;
    }


    private static ProjectSummary createProject(String groupId, String name, int id) {
        ProjectSummary project = new ProjectSummary();
        project.setId(id);
        project.setGroupId(groupId);
        project.setName(name);
        return project;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject createContinuumFields() {
        JSONObject fields = new JSONObject();
        fields.put("host", "localhost");
        fields.put("port", 80);
        fields.put("username", "admin");
        fields.put("password", "admin0");
        fields.put("web_path", "/continuum");
        fields.put("use_ssl", false);
        fields.put("__context_outputs__", new JSONObject());
        return fields;
    }

    private void createWorkItem(JSONObject fields) {
        createWorkItem(continuumWorker, fields);
    }

    private void createWorkItem(ContinuumWorker continuumWorker, JSONObject fields) {
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
    }

    private AddingResult mockProjectAddition(String projectPom, ProjectSummary project) throws Exception {
        AddingResult result = new AddingResult();
        result.addProject(project);
        when(continuumXmlRpcClient.addMavenTwoProject(projectPom)).thenReturn(result);
        return result;
    }

    private void mockProjectAddition(String projectPom, ProjectSummary project, int projectGroupId) throws Exception {
        AddingResult result = new AddingResult();
        result.addProject(project);
        when(continuumXmlRpcClient.addMavenTwoProject(projectPom, projectGroupId)).thenReturn(result);
    }
}
