#!/usr/bin/env groovy

def call(Map map = [:]) {
	def pom = "${map.pom ?: 'pom.xml'}"
	def deployorchestrator = "${map.deployorchestrator ?: 'beanstalk'}"
	def artefactext = "${map.artefactext ?: '.war'}"
	def releasearea = "${map.releasearea ?: 'smb'}"
	def clean = "${map.clean ?: 'true'}"
	def goal = "${map.goal ?: 'package'}"
	def binaryrepo = "${map.binaryrepo ?: 'releasearea'}"
	def props = "${map.props ?: '-DskipTests'}"
  	def mavencentral = "${map.mavencentral ?: 'true'}"
  	def mavensettings = "${map.mavensettings ?: 'true'}"
  	def rootfile = "${map.rootfile ?: 'false'}"
	
  if (mavensettings == 'true') {
    if (mavencentral == 'true') {
      withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '94e8f1b0-9ffd-431c-ab7d-88a104d37857', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
          if (isUnix()) {
              if (clean == 'true') {
              	sh "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
              } else {
                  sh "mvn -f ${pom} -P jenkins ${goal} ${props}"
           	  }
          } else {
              if (clean == 'true') {
                  bat "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
              } else {
                  bat "mvn -f ${pom} -P jenkins ${goal} ${props}"
           	  }
          }
      }
    } else {
      withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '442af402-463c-4167-98f1-558355e679cc', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
          if (isUnix()) {
              sh "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
          } else {
              if (clean == 'true') {
                  bat "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
              } else {
                  bat "mvn -f ${pom} -P jenkins ${goal} ${props}"
           	  }
          }
      }
    }
  } else {
	withMaven(maven: "$TOOL_MAVEN",jdk: "$TOOL_JDK", mavenLocalRepo: '.repository') {
    		if (isUnix()) {
			sh "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
		} else {
			if (clean == 'true') {
				bat "mvn -f ${pom} -P jenkins clean ${goal} ${props}"
			} else {
				bat "mvn -f ${pom} -P jenkins ${goal} ${props}"
			}
		}
	}
  }

	//copy artefacts to drop staging folder if they need to be copied to the releasearea
	//artefacts are already pushed to Nexus for example we skip this step
	if (binaryrepo == 'releasearea'){
		dir("${workspace}") {
			fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**/target/*${artefactext}", targetLocation: "_drop/artefacts")])
		}
    } else {
		dir("${workspace}") {
			fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**/target/*.jar", targetLocation: "_drop/artefacts")])
		}
    }
  	if (rootfile != 'false'){
  		fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**/deployframework/Templates/ROOT.war", targetLocation: "_drop/artefacts")])	
    }
	fileOperations([folderCopyOperation(destinationFolderPath: "${workspace}/_drop/artefacts/${deployorchestrator}_files", sourceFolderPath: "_${deployorchestrator}_files")])
	//finally push 'drop' staging area to releasearea
	pushToDropLocation(releasearea)
}
