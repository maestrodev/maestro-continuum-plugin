package com.maestrodev.maestrocontinuumplugin;

import com.maestrodev.MaestroWorker;
import java.net.URL;
import java.util.List;
import org.apache.continuum.xmlrpc.utils.BuildTrigger;
import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;
import org.apache.maven.continuum.xmlrpc.project.BuildDefinition;
import org.apache.maven.continuum.xmlrpc.project.BuildResult;
import org.apache.maven.continuum.xmlrpc.project.ContinuumProjectState;
import org.apache.maven.continuum.xmlrpc.project.Project;
import org.apache.maven.continuum.xmlrpc.project.ProjectGroup;
import org.apache.maven.continuum.xmlrpc.project.ProjectSummary;

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
    
    
    @Override
    public void writeOutput(String string){
        System.out.print(string);
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
    
    private BuildDefinition getBuildDefinitionFromProject(String goals, String arguments, Project project) throws Exception {
        List<BuildDefinition> buildDefinitions = project.getBuildDefinitions();
        
        for(BuildDefinition buildDefinition : buildDefinitions){
            if(buildDefinition.getGoals().equals(goals) &&
                    buildDefinition.getArguments().equals(arguments))
                return buildDefinition;
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
        
        buildDefinition.setSchedule(client.getSchedule(1));
        
        try {
            return client.addBuildDefinitionToProject(project.getId(), buildDefinition);
            
        } catch (Exception ex) {
            throw new Exception("Unable To Add Build Definition " + ex.getMessage());
        }
    }
    
    public Project triggerBuild(Project project, BuildDefinition buildDefinition) throws Exception{
        int buildNumber = project.getBuildNumber();
        int buildId = project.getLatestBuildId();
        
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

        while(buildId == project.getLatestBuildId()){
            if(System.currentTimeMillis() - start >  timeout){
                throw new Exception("Failed To Detect Build Start After " + (timeout/1000) + " Seconds");
            }
            
            
            
            this.writeOutput("Waiting For Build To Start "+ 
                    client.getProjectStatusAsString(project.getState()) +
                    " Last Build Number " + buildNumber + "\n");
            Thread.sleep(10000);
            
            project = client.getProjectWithAllDetails(project.getId());
            
        }
        
        this.writeOutput("Found New Build Number " + 
                    project.getBuildNumber() + "\n");
        return project;
    }
            
    private void waitForBuild(Project project) throws Exception {
        BuildResult result = client.getLatestBuildResult( project.getId() );
        String output = "";
        String runningTotal = "";
        while( "Updating".equals( client.getProjectStatusAsString( result.getState() ) ) ||
                "Building".equals( client.getProjectStatusAsString( result.getState() ) ) ){
            String newOutput = client.getBuildOutput(project.getId(), result.getId());
            output = newOutput.replace(runningTotal, "");
            runningTotal += output;
            writeOutput(output);
            Thread.sleep(5000);
            result = client.getLatestBuildResult( project.getId() );
        }
        
        String newOutput = client.getBuildOutput(project.getId(), result.getId());
        
        
        output = newOutput.replace(runningTotal, "");
        
        writeOutput(output);

        
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
            
            this.writeOutput("Searching For Build Definition With Goals " + goals+ "\n");
            this.writeOutput("And Arguements " + arguments+ "\n");
            BuildDefinition buildDefinition = this.getBuildDefinitionFromProject(goals, arguments, project);
            this.writeOutput("Retrieved Build Definition " + buildDefinition.getId()+ "\n");
            
            this.writeOutput("Triggering Build " + goals + "\n");
            project = triggerBuild(project, buildDefinition);
            this.writeOutput("The Build Has Started\n");
            
            waitForBuild(project);
        }catch(Exception e){
            writeOutput("Continuum Build Failed: " + e.getMessage());
            this.setError("Continuum Build Failed: " + e.getMessage());   
        }
    }

}
