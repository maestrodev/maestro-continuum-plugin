maestro-continuum-plugin
=============

Maestro plugin providing integration for Continuum. This
plugin is a Java-based deployable that gets delivered as a Zip file.

<http://continuum.apache.org>


Files:

* src/main/java/../ContinuumWorker.java
* pom.xml
* README.md (this file)
* manifest.json

## The Continuum Build Task
This task allows a composition to begin a build in continuum and wait for it's 
completion.  A Continuum Build Agent can run alongside a Maestro Agent.  Providing
a "continuum_build_agent":"address for the agent" fact will allow the Continuum
Build Agent to be configured in the Continuum Master.
 

* **project_name** Name Of The Project To Build
* **group_name** Name Of The Project Group
* **goals** Space Seperated List Of Maven Goals
* **arguments** Arguments To Pass To Maven Or The Build File
* **build_file** Maven Pom Or Shell Executable
* **host** Address Of The Continuum Build Server
* **port** Port Of The Continuum Build Server
* **username** User For Accessing Continuum
* **password** Password For Accessing Continuum
* **timeout** Seconds To Wait For Build To Begin
* **web_path** Context Path Of The Continuum Server
* **use_ssl** Use SSL When Connecting To Continuum


## The Add Shell Project Task
This task will add a shell based project to Continuum.


* **project_name** Name Of The Project To Build
* **group_name** Name Of The Project Group
* **group_id** Id Of The Group
* **group_description** Description Of The Group
* **project_description** Description For The Project
* **project_version** Version Of The Project
* **scm_url** Maven SCM Url
* **scm_username** Username To Access SCM
* **scm_password** Password To Access SCM
* **scm_use_cache** Use The SCM Credential Cache
* **scm_branch** Repository Branch
* **host** Address Of The Continuum Build Server
* **port** Port Of The Continuum Build Server
* **username** User For Accessing Continuum
* **password** Password For Accessing Continuum
* **web_path** Context Path Of The Continuum Server
* **use_ssl** Use SSL When Connecting To Continuum

## The Add Maven Project Task
This task will add a Maven based project to Continuum

* **project_name** Name Of The Project To Build
* **pom_url** Remote Url Of The Pom File
* **group_name** Name Of The Group That Contains The Project
* **group_id** Id Of The Group That Contains The Project
* **group_description** Description Of The Group That Contains The Project
* **host** Address Of The Continuum Build Server
* **port** Port Of The Continuum Build Server
* **username** User For Accessing Continuum
* **password** Password For Accessing Continuum
* **web_path** Context Path Of The Continuum Server
* **use_ssl** Use SSL When Connecting To Continuum


## License
Apache 2.0 License: <http://www.apache.org/licenses/LICENSE-2.0.html>