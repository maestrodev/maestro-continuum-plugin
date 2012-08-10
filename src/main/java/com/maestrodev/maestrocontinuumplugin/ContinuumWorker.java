package com.maestrodev.maestrocontinuumplugin;

import com.maestrodev.MaestroWorker;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.continuum.xmlrpc.utils.BuildTrigger;
import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.*;
import org.apache.maven.continuum.xmlrpc.system.Profile;
import org.json.simple.JSONObject;

/**
 * Hello world!
 *
 */
public class ContinuumWorker extends MaestroWorker
{
    
    private static ContinuumXmlRpcClient client;
    
    public ContinuumWorker(){
        super();
    }
    
    
    public void puts(String string){
        System.out.println(string);
    }
    
    private BuildAgentConfiguration getBuildAgent(String url) throws Exception {
        List<BuildAgentConfiguration> buildAgents = client.getAllBuildAgents();
        
        for(BuildAgentConfiguration buildAgent: buildAgents){
            if(buildAgent.getUrl().equals(url)){
                writeOutput("Making Sure Agent Is Enabled\n");
                buildAgent.setEnabled(true);
                client.updateBuildAgent(buildAgent);
                if(!buildAgent.isEnabled()){
                    throw new Exception("Build Agent " + buildAgent.getUrl() + 
                            " Is Currently Not Enabled");
                }
                return buildAgent;
            }
        }
        
        BuildAgentConfiguration buildAgentConfiguration = new BuildAgentConfiguration();
        
        buildAgentConfiguration.setDescription("Maestro Configured Build Agent (" + url + ")");
        buildAgentConfiguration.setEnabled(true);
        buildAgentConfiguration.setUrl(url);
        
        
        buildAgentConfiguration = client.addBuildAgent(buildAgentConfiguration);
        
        
        if(!buildAgentConfiguration.isEnabled()){
            throw new Exception("Unable To Find Build Agent At " + url);
        }
        
        return buildAgentConfiguration;
    }
    
    private BuildAgentGroupConfiguration createBuildAgentGroup(String name, BuildAgentConfiguration buildAgentConfiguration) throws Exception{
        BuildAgentGroupConfiguration buildAgentGroupConfiguration = new BuildAgentGroupConfiguration();
        buildAgentGroupConfiguration.setName(name);
                
        buildAgentGroupConfiguration.addBuildAgent(buildAgentConfiguration);
        
        return client.addBuildAgentGroup(buildAgentGroupConfiguration);
    }
    
    private Profile createProfile(String name, String buildAgentGroupName) throws Exception{
        Profile profile = new Profile();
        
        profile.setBuildAgentGroup(buildAgentGroupName);
        profile.setActive(true);
        profile.setName(name);
        
        profile = client.addProfile(profile);
        
        return profile;
    }
    
    
    private Profile findProfile(String name) throws Exception{
        try{
            return client.getProfileWithName(name);
        }catch(Exception e){
            writeOutput("Unable To Locate Profile With Name " + name);
        }
        
        return null;
    }

    private ContinuumXmlRpcClient getClient() throws MalformedURLException {
      URL url = getUrl();
      this.writeOutput("Using Continuum At " + url.toString() + "\n");
      return new ContinuumXmlRpcClient( url, this.getField("username"), this.getField("password"));
    }
    
    
    private ProjectGroup getProjectGroup(String projectGroupName) throws Exception{
        List<ProjectGroup> projectGroups = client.getAllProjectGroupsWithAllDetails();
        
        for(ProjectGroup projectGroup : projectGroups){
            if(projectGroup.getName().equals(projectGroupName) ||
                    projectGroup.getGroupId().equals(projectGroupName))
                return projectGroup;
        }
        
        throw new Exception("Unable To Find Project Group " + projectGroupName);
    }
    
    
    private Project getProjectFromProjectGroup(String projectName, ProjectGroup projectGroup) throws Exception{
        List<ProjectSummary> projects = projectGroup.getProjects();
        
        for(ProjectSummary project : projects){
            if(project.getName().equals(projectName))
                return client.getProjectWithAllDetails(project.getId());
        }
        
        throw new Exception("Unable To Find Project " + projectName);
    }
    
    
    private BuildDefinition getBuildDefinitionFromId(int buildDefinitionId, String goals, String arguments, Project project, Profile profile) throws Exception {
        BuildDefinition buildDefinition = client.getBuildDefinition(buildDefinitionId);
        
        buildDefinition.setGoals(goals);
        buildDefinition.setArguments(arguments);
        
        if(profile != null)
            buildDefinition.setProfile(profile);
        
        
        client.updateBuildDefinitionForProject(project.getId(), buildDefinition);
        
        return buildDefinition;
    }
    
    
    private BuildDefinition getBuildDefinitionFromProject(String goals, String arguments, Project project, Profile profile) throws Exception {
        List<BuildDefinition> buildDefinitions = project.getBuildDefinitions();
        
        for(BuildDefinition buildDefinition : buildDefinitions){
            if(buildDefinition.getGoals().equals(goals) &&
                    buildDefinition.getArguments().equals(arguments)){
                
                if(profile == null)
                    return buildDefinition;
                
                if(buildDefinition.getProfile() != null && 
                        buildDefinition.getProfile().getName().equals(profile.getName()))
                    return buildDefinition;
            }
        }
        
        this.writeOutput("Unable To Detect Build Definition Creation Will Begin\n");
        BuildDefinition buildDefinition = new BuildDefinition();
        buildDefinition.setArguments(arguments);
        buildDefinition.setGoals(goals);
        buildDefinition.setDescription("Build Definition Generated By Maestro 4");
        buildDefinition.setDefaultForProject(false);
        buildDefinition.setAlwaysBuild(false);
        buildDefinition.setBuildFresh(true);
        buildDefinition.setBuildFile("pom.xml");
        buildDefinition.setType("maven2");
        
        
        if(profile != null){
            buildDefinition.setProfile(profile);
        }
        
        
        buildDefinition.setSchedule(client.getSchedule(1));
        
        try {
            return client.addBuildDefinitionToProject(project.getId(), buildDefinition);
            
        } catch (Exception ex) {
            throw new Exception("Unable To Add Build Definition " + ex.getMessage());
        }
    }

    private URL getUrl() throws MalformedURLException {
      URL url;
      String scheme = "http" + (Boolean.parseBoolean(this.getField("use_ssl")) ? "s" : "");
      url = new URL(scheme + "://"+this.getField("host")+":"+
              this.getField("port") + "/" + 
              this.getField("web_path").replaceAll("^\\/", "") + "/" +
              "xmlrpc");
      return url;
    }

  private Profile setupBuildAgent(Profile profile) throws Exception {
    try{
        writeOutput("Using Agent Facts To Locate Continuum Build Agent\n");
        profile = findProfile(getField("composition"));
        Map facts = (Map)(getFields().get("facts"));
        BuildAgentConfiguration buildAgent = this.getBuildAgent((String)facts.get("continuum_build_agent"));
        
        if(profile == null){
            writeOutput("Build Environment Not Found, Created New ("+getField("composition")+")\n");
            profile = this.createProfile(getField("composition"), this.createBuildAgentGroup(getField("composition"), buildAgent).getName());
        } else {
//                        verify build agent is in group
            writeOutput("Build Environment Found, Verifying Agent\n");
            BuildAgentGroupConfiguration buildAgentGroupConfiguration = client.getBuildAgentGroup(profile.getBuildAgentGroup());
            boolean found = false;
            
            for(BuildAgentConfiguration ba : buildAgentGroupConfiguration.getBuildAgents()){
                if(ba.getUrl().equals(buildAgent.getUrl())){
                    found = true;
                    break;
                }
            }
            
            if(!found){
                buildAgentGroupConfiguration.addBuildAgent(buildAgent);
                client.updateBuildAgentGroup(buildAgentGroupConfiguration);
            }
        }
    }catch(Exception e){
        throw new Exception("Error Locating Continuum Build Agent Or Creating Build Environment" + e.getMessage());
    }
    return profile;
  }
    
    private Project triggerBuild(Project project, BuildDefinition buildDefinition) throws Exception{
        int buildNumber = project.getBuildNumber();
//        int buildId = project.getLatestBuildId();
        
        BuildTrigger buildTrigger = new BuildTrigger();
        buildTrigger.setTrigger(ContinuumProjectState.TRIGGER_FORCED);
        buildTrigger.setTriggeredBy(this.getField("username"));
        try {
            client.buildProject(project.getId(), buildDefinition.getId(), buildTrigger);
        } catch (Exception ex) {
            throw new Exception("Failed To Trigger Build " + ex.getMessage());
        }
        
        int timeout = Integer.parseInt(this.getField("timeout")) * 1000;
        long start = System.currentTimeMillis();

        this.writeOutput("Waiting For Build To Start "+ 
                    client.getProjectStatusAsString(project.getState()) +
                    " Previous Build Number " + buildNumber + "\n");
        
        
        while(!client.getProjectStatusAsString(project.getState()).equals("Building")){
            if(System.currentTimeMillis() - start >  timeout){
                throw new Exception("Failed To Detect Build Start After " + (timeout/1000) + " Seconds");
            }
            
            Thread.sleep(1000);
//            writeOutput(client.getProjectStatusAsString(project.getState()) + "\n");
            project = client.getProjectWithAllDetails(project.getId());
            
        }
        
//        this.writeOutput("Found New Build Number " + 
//                    project.getBuildNumber() + "\n");
        return project;
    }
            
    private void waitForBuild(Project project) throws Exception {
        project = client.getProjectWithAllDetails( project.getId() );
        String output = "";
        String runningTotal = "";
        while( "Updating".equals( client.getProjectStatusAsString( project.getState() ) ) ||
               "Building".equals( client.getProjectStatusAsString( project.getState() ) ) ){
//            String newOutput = client.getBuildOutput(project.getId(), project.getLatestBuildId());
//            output = newOutput.replace(runningTotal, "");
//            runningTotal += output;
//            writeOutput(output);
//            puts(output);
            Thread.sleep(5000);
            project = client.getProjectWithAllDetails( project.getId() );
        }
        
        project = client.getProjectWithAllDetails( project.getId() );
        
        String newOutput = client.getBuildOutput(project.getId(), project.getLatestBuildId());
        
        output = newOutput.replace(runningTotal, "");
        
        writeOutput(output);
//puts(output);
        BuildResult result = client.getBuildResult(project.getId(), project.getLatestBuildId());
        if(result.getExitCode() != 0)
            throw new Exception("Result Returned Not Success");
    }
    
    public void build() {
        try{
            client = getClient();
           
            String projectGroupName = this.getField("project_group");
            this.writeOutput("Searching For Project Group " + projectGroupName + "\n");
            ProjectGroup projectGroup = this.getProjectGroup(projectGroupName);
            this.writeOutput("Found Project Group " + projectGroup.getName()+ "\n");
            
            String projectName = this.getField("project");
            this.writeOutput("Searching For Project " + projectName+ "\n");
            Project project = this.getProjectFromProjectGroup(projectName, projectGroup);
            this.writeOutput("Found Project " + project.getName()+ "\n");
            
            String goals = this.getField("goals");
            String arguments = this.getField("arguments");
            

            Profile profile = null;
            if(getField("use_agent_facts") != null && getField("use_agent_facts").equals("true")){
              profile = setupBuildAgent(profile);
            }
            
            this.writeOutput("Searching For Build Definition With Goals " + goals+ "\n");
            this.writeOutput("And Arguements " + arguments+ "\n");
            
            BuildDefinition buildDefinition = null;
            if(this.getFields().get("__previous_context_outputs__") != null &&
                    ((JSONObject)this.getFields().get("__previous_context_outputs__")).get("build_definition_id") != null){
                puts("using previous build id");
                try{
                    buildDefinition = this.getBuildDefinitionFromId(Integer.parseInt(((JSONObject)this.getFields().get("__previous_context_outputs__")).get("build_definition_id").toString()),goals, arguments, project, profile);
                }catch(Exception w){
                    buildDefinition = this.getBuildDefinitionFromProject(goals, arguments, project, profile);
                }
                
            }
            
            if(buildDefinition == null){
                buildDefinition = this.getBuildDefinitionFromProject(goals, arguments, project, profile);
            }
            
            
            this.writeOutput("Retrieved Build Definition " + buildDefinition.getId()+ "\n");
            
            this.writeOutput("Triggering Build " + goals + "\n");
            project = triggerBuild(project, buildDefinition);
            this.writeOutput("The Build Has Started\n");
            
            waitForBuild(project);
            
            JSONObject outputData = (JSONObject)this.getFields().get("__context_outputs__");
            if(outputData == null)
                outputData = new JSONObject();
            outputData.put("build_definition_id", buildDefinition.getId());
            outputData.put("build_id", project.getLatestBuildId());            
            this.getFields().put("__context_outputs__", outputData);
        }catch(Exception e){
            this.setError("Continuum Build Failed: " + e.getMessage());   
        }
    }

    public void addMavenProject() {
      try {
        client = this.getClient();
        ProjectGroup projectGroup = null;
        try{
          projectGroup = getProjectGroup(getField("group_name"));
          
        } catch (Exception e) {
          projectGroup = createProjectGroup();
        }
        
        ProjectSummary project = null;
        try{
           project = getProjectFromProjectGroup(getField("project_name"), projectGroup);
        }catch(Exception e) {
          project = createMavenProject(projectGroup.getId());
        }
        
        writeOutput("Successfully Created Maven Project " + project.getName());
      } catch (Exception e) {
        this.setError("Continuum Build Failed: " + e.getMessage());   
      }
    }

    public void addShellProject() {
      try {
        client = this.getClient();
        ProjectGroup projectGroup = null;
        try{
          projectGroup = getProjectGroup(getField("group_name"));
          
        } catch (Exception e) {
          projectGroup = createProjectGroup();
        }
        
        ProjectSummary project = null;
        try{
           project = getProjectFromProjectGroup(getField("project_name"), projectGroup);
        }catch(Exception e) {
          project = createShellProject(projectGroup.getId());
        }
        
        writeOutput("Successfully Created Shell Project " + project.getName());
      } catch (Exception e) {
        this.setError("Continuum Build Failed: " + e.getMessage());   
      }
    }
    
    private ProjectGroup createProjectGroup() throws Exception {
      ProjectGroupSummary projectGroup = new ProjectGroupSummary();
      projectGroup.setDescription(getField("description"));
      projectGroup.setGroupId(getField("group_id"));
      projectGroup.setName(getField("group_name"));
      client.addProjectGroup(projectGroup);
      
      return getProjectGroup(getField("group_name"));
    }

    private ProjectSummary createShellProject(int projectGroupId) throws Exception {
      ProjectSummary project = new ProjectSummary();
      project.setName(getField("project_name"));
      project.setDescription(getField("project_description"));
      project.setVersion(getField("project_version"));
      project.setScmUrl(getField("scm_url"));
      project.setScmUsername(getField("scm_username"));
      project.setScmPassword(getField("scm_password"));
      project.setScmUseCache(Boolean.parseBoolean(getField("scm_use_cache")));
      project.setScmTag(getField("scm_branch"));
      
      
      
      project = client.addShellProject(project, projectGroupId);
      
      if(project == null) {
        throw new Exception("Unable To Create Project In " + getField("group_name"));
      }
      return project;
    }
    
    private ProjectSummary createMavenProject(int projectGroupId) throws Exception {      
      AddingResult result = client.addMavenTwoProject(getField("pom_url"), projectGroupId);
      if(result.hasErrors()){
        throw new Exception(result.getErrorsAsString());
      }
      ProjectSummary project = result.getProjects().get(0);
      if( project == null) {
        throw new Exception("Unable To Create Project In " + getField("group_name"));
      }
      return project;
    }
}
