/*
 * Copyright 2012, MaestroDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maestrodev.maestrocontinuumplugin;

import com.maestrodev.StompConnectionFactory;
import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.AddingResult;
import org.apache.maven.continuum.xmlrpc.project.BuildDefinition;
import org.apache.maven.continuum.xmlrpc.project.BuildResult;
import org.apache.maven.continuum.xmlrpc.project.ContinuumProjectState;
import org.apache.maven.continuum.xmlrpc.project.Project;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroup;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroupSummary;
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
import static org.hamcrest.CoreMatchers.is;
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
        group.setId(1);
        group.setName("HelloWorld");
        group.setGroupId("com.maestrodev");
        projectGroups.add(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);

        String projectPom = "https://svn.apache.org/repos/asf/activemq/trunk/pom.xml";
        mockProjectAddition(projectPom, createProject(group.getGroupId(), "projectName", 1), 1);

        JSONObject fields = createContinuumFields();
        fields.put("group_name", group.getName());
        fields.put("group_id", group.getGroupId());
        fields.put("group_description", "clean test install package");
        fields.put("pom_url", projectPom);

        createWorkItem(fields);

        continuumWorker.addMavenProject();

        assertNotNull(getContinuumProjectId());
        assertNull(continuumWorker.getError());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDuplicateMavenProjectDifferentGroups() throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        String projectPom = "https://raw.github.com/etiennep/centrepoint/master/pom.xml";
        String name = "HelloWorld";

        String groupId = "testCreateDuplicateMavenProjectDifferentGroups";
        ProjectSummary project = createProject(groupId, name, 1);
        ProjectGroup group = createProjectGroup(groupId, project, 1);
        projectGroups.add(group);

        String groupId2 = "testCreateDuplicateMavenProjectDifferentGroups2";
        ProjectSummary project2 = createProject(groupId2, name, 2);
        ProjectGroup group2 = createProjectGroup(groupId2, project2, 2);
        projectGroups.add(group2);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);
        when(continuumXmlRpcClient.getProjectGroup(1)).thenReturn(group);
        when(continuumXmlRpcClient.getProjectGroup(2)).thenReturn(group2);

        mockProjectAddition(projectPom, project, 1);
        mockProjectAddition(projectPom, project2, 2);

        JSONObject fields = createContinuumFields();
        fields.put("pom_url", projectPom);
        fields.put("group_name", group.getName());
        fields.put("group_id", group.getGroupId());
        fields.put("group_description", "Description");
        createWorkItem(fields);

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));

        fields = createContinuumFields();
        fields.put("pom_url", projectPom);
        fields.put("group_name", group2.getName());
        fields.put("group_id", group2.getGroupId());
        fields.put("group_description", "Description");
        createWorkItem(fields);

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project2.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDuplicateExistingMavenProjectDifferentGroups() throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        String projectPom = "https://raw.github.com/etiennep/centrepoint/master/pom.xml";
        String name = "HelloWorld";

        String groupId = "testCreateDuplicateExistingMavenProjectDifferentGroups";
        ProjectSummary project = createProject(groupId, name, 1);
        ProjectGroup group = createProjectGroup(groupId, project, 1);
        projectGroups.add(group);

        String groupId2 = "testCreateDuplicateExistingMavenProjectDifferentGroups2";
        ProjectSummary project2 = createProject(groupId2, name, 2);
        ProjectGroup group2 = createProjectGroup(groupId2, project2, 2);
        projectGroups.add(group2);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);
        when(continuumXmlRpcClient.getProjectGroup(1)).thenReturn(group);
        when(continuumXmlRpcClient.getProjectGroup(2)).thenReturn(group2);

        mockProjectDuplicate(projectPom, project);

        JSONObject fields = createContinuumFields();
        fields.put("pom_url", projectPom);
        fields.put("group_name", group.getName());
        fields.put("group_id", group.getGroupId());
        fields.put("group_description", "Description");
        createWorkItem(fields);

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));

        mockProjectDuplicate(projectPom, project2);

        fields = createContinuumFields();
        fields.put("pom_url", projectPom);
        fields.put("group_name", group2.getName());
        fields.put("group_id", group2.getGroupId());
        fields.put("group_description", "Description");
        createWorkItem(fields);

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project2.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));
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
        group.setGroupId(project.getGroupId());
        group.addProject(project);
        projectGroups.add(group);
        project.setProjectGroup(group);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);

        mockProjectDuplicate(projectPom, project, group, ContinuumWorker.NO_PROJECT_GROUP);
        when(continuumXmlRpcClient.getProjectGroup(1)).thenReturn(group);

        JSONObject fields = createContinuumFields();
        fields.put("pom_url", projectPom);

        createWorkItem(fields);

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));
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

        continuumWorker.addMavenProject();
        assertThat(getContinuumProjectId(), is(equalTo(project.getId())));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    private void setupBuildProjectMocks(int projectId, int buildDefId, String projectName, ProjectGroup group)
            throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();

        ProjectSummary projectSummary = new ProjectSummary();
        projectSummary.setName(projectName);
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
        project.setName(projectName);
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

    private ProjectGroup createProjectGroup() {
        ProjectGroup group = new ProjectGroup();
        group.setName("HelloWorld");
        group.setGroupId("com.maestrodev");
        return group;
    }

    /**
     * Rigourous Test :-)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuild() throws Exception {
        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId, "HelloWorld", createProjectGroup());

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
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildDuplicateMavenProjectDifferentGroups() throws Exception {
        List<ProjectGroup> projectGroups = new ArrayList<ProjectGroup>();
        String projectPom = "https://raw.github.com/etiennep/centrepoint/master/pom.xml";
        String name = "HelloWorld";

        String groupId = "testBuildDuplicateMavenProjectDifferentGroups";
        ProjectSummary project = createProject(groupId, name, 1);
        ProjectGroup group = createProjectGroup(groupId, project, 1);
        projectGroups.add(group);

        String groupId2 = "testBuildDuplicateMavenProjectDifferentGroups2";
        ProjectSummary project2 = createProject(groupId2, name, 2);
        ProjectGroup group2 = createProjectGroup(groupId2, project2, 2);
        projectGroups.add(group2);

        when(continuumXmlRpcClient.getAllProjectGroupsWithAllDetails()).thenReturn(projectGroups);
        when(continuumXmlRpcClient.getProjectGroup(1)).thenReturn(group);
        when(continuumXmlRpcClient.getProjectGroup(2)).thenReturn(group2);

        mockProjectAddition(projectPom, project, 1);
        mockProjectAddition(projectPom, project2, 2);

        int buildDefId = 1;
        setupBuildProjectMocks(project.getId(), buildDefId, project.getName(), group);

        JSONObject fields = createContinuumFields();
        fields.put("group_name", group.getName());
        fields.put("group_id", group.getGroupId());
        fields.put("project_name", project.getName());
        fields.put("project_group", group.getGroupId());
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("facts", new JSONObject());
        fields.put("composition", "Test Composition");

        JSONObject params = new JSONObject();
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId));
        assertThat(continuumWorker.getError(), is(nullValue()));

        int buildDefId2 = 2;
        setupBuildProjectMocks(project2.getId(), buildDefId2, project2.getName(), group2);

        fields = createContinuumFields();
        fields.put("group_name", group2.getName());
        fields.put("group_id", group2.getGroupId());
        fields.put("project_name", project2.getName());
        fields.put("project_group", group2.getGroupId());
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("facts", new JSONObject());
        fields.put("composition", "Test Composition");

        params = new JSONObject();
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId2));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    /**
     * Test a build step that finds a project ID in the context data.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithProjectIdInContext() throws Exception {
        int projectId = 8;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId, "HelloWorld", createProjectGroup());

        JSONObject fields = createContinuumFields();
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("composition", "Test Composition");
        fields.put("facts", new JSONObject());

        JSONObject params = new JSONObject();
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        JSONObject contextOutputs = new JSONObject();
        contextOutputs.put(ContinuumWorker.CONTINUUM_PROJECT_ID, (long) projectId);
        fields.put(ContinuumWorker.CONTEXT_OUTPUTS, contextOutputs);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithPreviousContextOutputs() throws Exception {

        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId, "HelloWorld", createProjectGroup());

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
        previousContextOutputs.put(ContinuumWorker.BUILD_DEFINITION_ID, (long) buildDefId);

        fields.put(ContinuumWorker.PREVIOUS_CONTEXT_OUTPUTS, previousContextOutputs);

        JSONObject params = new JSONObject();
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId));
        assertThat(continuumWorker.getError(), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithPreviousContextOutputsAndChangeGoals() throws Exception {
        int projectId = 1;
        int buildDefId = 1;

        setupBuildProjectMocks(projectId, buildDefId, "HelloWorld", createProjectGroup());

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
        previousContextOutputs.put(ContinuumWorker.BUILD_DEFINITION_ID, (long) buildDefId);
        fields.put(ContinuumWorker.PREVIOUS_CONTEXT_OUTPUTS, previousContextOutputs);

        JSONObject params = new JSONObject();
        params.put(ContinuumWorker.PARAMS_COMPOSITION_TASK_ID, 1L);
        fields.put("params", params);

        createWorkItem(fields);

        continuumWorker.build();

        assertThat(getBuildDefinitionId(), is(buildDefId));
        assertThat(continuumWorker.getError(), is(nullValue()));
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
        agentFacts.put(ContinuumWorker.FACT_CONTINUUM_BUILD_AGENT, "http://localhost:9001/continuum-buildagent/xmlrpc");

        fields.put("facts", agentFacts);

        createWorkItem(continuumWorker, fields);

        continuumWorker.build();

        assertNull(continuumWorker.getError());
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
        fields.put(ContinuumWorker.CONTEXT_OUTPUTS, new JSONObject());
        return fields;
    }

    private void createWorkItem(JSONObject fields) {
        createWorkItem(continuumWorker, fields);
    }

    @SuppressWarnings("unchecked")
    private void createWorkItem(ContinuumWorker continuumWorker, JSONObject fields) {
        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
    }

    private AddingResult mockProjectAddition(String projectPom, ProjectSummary project) throws Exception {
        return mockProjectAddition(projectPom, project, ContinuumWorker.NO_PROJECT_GROUP);
    }

    private AddingResult mockProjectAddition(String projectPom, ProjectSummary project, int projectGroupId) throws Exception {
        AddingResult result = new AddingResult();
        result.addProject(project);
        when(continuumXmlRpcClient.addMavenTwoProject(projectPom, projectGroupId)).thenReturn(result);
        return result;
    }

    private void mockProjectDuplicate(String projectPom, ProjectSummary project) throws Exception {
        mockProjectDuplicate(projectPom, project, project.getProjectGroup(), project.getProjectGroup().getId());
    }

    private void mockProjectDuplicate(String projectPom, ProjectSummary project, ProjectGroupSummary projectGroup, int projectGroupId) throws Exception {
        ProjectSummary projectSummary = new ProjectSummary();
        projectSummary.setId(0);
        projectSummary.setGroupId(project.getGroupId());
        projectSummary.setName(project.getName());
        projectSummary.setProjectGroup(projectGroup);
        AddingResult result = mockProjectAddition(projectPom, projectSummary, projectGroupId);
        result.addError(ContinuumWorker.DUPLICATE_PROJECT_ERR);
    }

    private int getBuildDefinitionId() {
        JSONObject output = continuumWorker.getContext();
        return Integer.valueOf(output.get(ContinuumWorker.BUILD_DEFINITION_ID).toString());
    }

    private int getContinuumProjectId() {
        JSONObject output = continuumWorker.getContext();
        return Integer.valueOf(output.get(ContinuumWorker.CONTINUUM_PROJECT_ID).toString());
    }
}
