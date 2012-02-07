package com.maestrodev.maestrocontinuumplugin;

import com.maestrodev.MaestroWorker;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.continuum.xmlrpc.utils.BuildTrigger;
import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.BuildAgentConfiguration;
import org.apache.maven.continuum.xmlrpc.project.BuildAgentGroupConfiguration;
import org.apache.maven.continuum.xmlrpc.project.BuildDefinition;
import org.apache.maven.continuum.xmlrpc.project.BuildResult;
import org.apache.maven.continuum.xmlrpc.project.ContinuumProjectState;
import org.apache.maven.continuum.xmlrpc.project.Project;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroup;
import org.apache.maven.continuum.xmlrpc.project.ProjectSummary;
import org.apache.maven.continuum.xmlrpc.system.Profile;

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
    
    
    private void puts(String string){
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
        
        throw new Exception("Unable To Find Build Agent At " + url);
    }
    
    private BuildAgentGroupConfiguration createBuildAgentGroup(String name, BuildAgentConfiguration buildAgentConfiguration) throws Exception{
        BuildAgentGroupConfiguration buildAgentGroupConfiguration = new BuildAgentGroupConfiguration();
        buildAgentGroupConfiguration.setName(name + " (" + buildAgentConfiguration.getUrl() + ")");
                
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
    
    private Project triggerBuild(Project project, BuildDefinition buildDefinition) throws Exception{
        int buildNumber = project.getBuildNumber();
//        int buildId = project.getLatestBuildId();
        
        BuildTrigger buildTrigger = new BuildTrigger();
        buildTrigger.setTrigger(ContinuumProjectState.TRIGGER_FORCED);
        buildTrigger.setTriggeredBy("Maestro4");
        try {
            client.buildProject(project.getId(), buildDefinition.getId(), buildTrigger);
        } catch (Exception ex) {
            throw new Exception("Failed To Trigger Build " + ex.getMessage());
        }
        
        int timeout = Integer.parseInt(this.getField("timeout")) * 1000;
        long start = System.currentTimeMillis();

        while(!client.getProjectStatusAsString(project.getState()).equals("Building")){
            if(System.currentTimeMillis() - start >  timeout){
                throw new Exception("Failed To Detect Build Start After " + (timeout/1000) + " Seconds");
            }
            
            this.writeOutput("Waiting For Build To Start "+ 
                    client.getProjectStatusAsString(project.getState()) +
                    " Last Build Number " + buildNumber + "\n");
            Thread.sleep(1000);
            
            project = client.getProjectWithAllDetails(project.getId());
            
        }
        
        this.writeOutput("Found New Build Number " + 
                    project.getBuildNumber() + "\n");
        return project;
    }
            
    private void waitForBuild(Project project) throws Exception {
        project = client.getProjectWithAllDetails( project.getId() );
        String output = "";
        String runningTotal = "";
        while( "Updating".equals( client.getProjectStatusAsString( project.getState() ) ) ||
               "Building".equals( client.getProjectStatusAsString( project.getState() ) ) ){
            String newOutput = client.getBuildOutput(project.getId(), project.getLatestBuildId());
            output = newOutput.replace(runningTotal, "");
            runningTotal += output;
            writeOutput(output);
            Thread.sleep(5000);
            project = client.getProjectWithAllDetails( project.getId() );
        }
        
        project = client.getProjectWithAllDetails( project.getId() );
        
        String newOutput = client.getBuildOutput(project.getId(), project.getLatestBuildId());
        
        output = newOutput.replace(runningTotal, "");
        
        writeOutput(output);

        BuildResult result = client.getBuildResult(project.getId(), project.getLatestBuildId());
        if(result.getExitCode() != 0)
            throw new Exception("Result Returned Not Success");
    }
    
    public void build() {
        try{
            URL url = null;
         
            url = new URL("http://"+this.getField("host")+":"+
                    this.getField("port") + "/" + 
                    this.getField("web_path").replaceAll("^\\/", "") + "/" +
                    "xmlrpc");
            this.writeOutput("Using Continuum At " + url.toString() + "\n");
            client = new ContinuumXmlRpcClient( url, this.getField("username"), this.getField("password"));
           
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
                try{
                    writeOutput("Using Agent Facts To Locate Continuum Build Agent\n");
                    profile = findProfile(getField("composition"));
                    Map facts = (Map)(getFields().get("facts"));
                    BuildAgentConfiguration buildAgent = this.getBuildAgent((String)facts.get("continuum_build_agent"));
                    if(profile == null){
                        profile = this.createProfile(getField("composition"), this.createBuildAgentGroup(getField("composition"), buildAgent).getName());
                    }
                }catch(Exception e){
                    throw new Exception("Error Locating Continuum Build Agent Or Creating Build Environment" + e.getMessage());
                }
            }
            
            this.writeOutput("Searching For Build Definition With Goals " + goals+ "\n");
            this.writeOutput("And Arguements " + arguments+ "\n");
            BuildDefinition buildDefinition = this.getBuildDefinitionFromProject(goals, arguments, project, profile);
            this.writeOutput("Retrieved Build Definition " + buildDefinition.getId()+ "\n");
            
            this.writeOutput("Triggering Build " + goals + "\n");
            project = triggerBuild(project, buildDefinition);
            this.writeOutput("The Build Has Started\n");
            
            waitForBuild(project);
        }catch(Exception e){
            this.setError("Continuum Build Failed: " + e.getMessage());   
        }
    }

}
