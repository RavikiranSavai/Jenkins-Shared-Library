#!/usr/bin/env groovy

/**
 * Execute sonarqube analysis, handles maven, ant and msbuild scenarios
 */
def call(Map map = [:]) {
    // set defaults if nothing supplied in map
    def exclusions = "${map.exclusions ?: ''}"
    def pom = "${map.pom ?: 'pom.xml'}"
    def buildengine = "${map.buildengine ?: 'maven'}"

    env.SONAR_SCANNER_OPTS = "-Djavax.net.ssl.trustStore=D:/cert/cacerts"

    echo "exclusion list : ${exclusions}"
    echo "pom name : ${pom}"
    env.POM = pom
    echo "buildengine : ${buildengine}"

    //set default for all tool versions
    def mvnHome, mvnVer
    def javaHome, jdkVer
    def antHome
    def msbuildHome, msbuildScannerExe, msbuildScannerHome
    def jenkinsScannerHome

    if (env.SONARPROJECTKEY) {
    } else {
        env.SONARPROJECTKEY= "${APPLICATION_NAME}-${COMPONENT_NAME}"
    }
  
    if (env.SONARPROJECTNAME) {
    } else {
        env.SONARPROJECTNAME= BRANCH_NAME.replace('/',':')
        env.SONARPROJECTNAME = "${APPLICATION_NAME}-${COMPONENT_NAME}:${SONARPROJECTNAME}"
    }

    if (env.TOOL_MAVEN) {
        echo "maven version '$TOOL_MAVEN' explicitly specified"
        mvnHome = tool "$TOOL_MAVEN"
        mvnVer = "$TOOL_MAVEN"
    } else {
        //set default for o/s
        if (isUnix()) {
            mvnHome = tool 'Maven_3.5.3-Linux'
            mvnVer = 'Maven_3.5.3-Linux'
        } else {
            mvnHome = tool 'Maven_3.5.0'
            mvnVer = 'Maven_3.5.0'
        }
    }
    if (mvnVer.startsWith("Maven_3.5.0")) {
        env.SONARGOAL = 'org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar'
    } else if (mvnVer.startsWith("Maven_3.3.9")) {
        env.SONARGOAL = 'org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar' 
    } else {
        env.SONARGOAL = 'sonar:sonar'
    }
    if (env.TOOL_JDK) {
      	if (env.TOOL_JDK == "JDK 1.13-Linux") {
        	javaHome = tool 'Corretto 8-Linux'
        	jdkVer = 'Corretto 8-Linux'
        	echo "jdk version 'Corretto 8' explicitly specified, java home is '$javaHome'"
        } else {
        	javaHome = tool "$TOOL_JDK"
        	jdkVer = "$TOOL_JDK"
        	echo "jdk version '$TOOL_JDK' explicitly specified, java home is '$javaHome'"
        }
    } else {
        //set default for o/s
        if (isUnix()) {
            javaHome = tool 'JDK 1.8-Linux'
            jdkVer = 'JDK 1.8-Linux'
        } else {
            javaHome = tool 'JDK 1.8'
            jdkVer = 'JDK 1.8'  
        } 
    } 
    if (env.TOOL_ANT) {
        echo "ant version '$TOOL_ANT' explicitly specified"
        antHome = tool "$TOOL_ANT"
      	//env.ANT_OPTS=" - Djavax.net.ssl.trustStore=D:\cert\cacerts "
    } else {
        //set default for o/s
        if (isUnix()) {
            antHome = tool 'Ant 1.9.9-Linux' 
        } else {
            antHome = tool 'Ant 1.9.9'
          	//env.ANT_OPTS=" - Djavax.net.ssl.trustStore=D:\cert\cacerts "
        }
    }
    if (env.TOOL_MSBUILD) {
        echo "msbuild version '$TOOL_MSBUILD' explicitly specified"
        msbuildHome = tool "$TOOL_MSBUILD"
    } else {
        if (isUnix()) {
        } else {    
            msbuildHome = tool 'MSBuild-14' 
        }
    } 
    if (env.TOOL_SONARMSBUILD) {
        echo "sonar msbuild version '$TOOL_SONARMSBUILD' explicitly specified"
        msbuildScannerHome = tool "${env.TOOL_SONARMSBUILD}"
        msbuildScannerExe = 'SonarQube.Scanner.MSBuild.exe'
    } else {
        if (isUnix()) {
        } else {
            msbuildScannerHome = tool 'SonarMSBuild-45'
            msbuildScannerExe = 'SonarScanner.MSBuild.exe'
        }
    }
    if (env.TOOL_SONARJENKINS) {
        echo "jenkins sonar scanner version '$TOOL_SONARJENKINS' explicitly specified"
        jenkinsScannerHome = tool "$TOOL_SONARJENKINS"
    } else {
        //set default for o/s
        if (isUnix()) {
            jenkinsScannerHome = tool 'SonarRunner-Linux'
        } else {
            jenkinsScannerHome = tool 'SonarRunner'
        }
    }

    def baselineversion
    if ("${PIPELINE_VERSION}".endsWith(".1")) {
        //if first build, no leak period is set so we should set it to the value from Trunk/master project (or parent branch if no Trunk exists)
        if(env.SONARLEAKPARENTBRANCH){
            //value can be set in jenkinsfile      
        } else {
            if ("${SOURCE_PROVIDER}" == "svn") {
                env.SONARLEAKPARENTBRANCH = 'Trunk'
            } else if ("${SOURCE_PROVIDER}" == "git") {	
                env.SONARLEAKPARENTBRANCH = 'master'
            }
        }
        //look up baseline version from parent branch
        withSonarQubeEnv('Sonar') {
            def leakurl
            if (isUnix()) {
                leakurl = env.SONAR_HOST_URL + "/api/settings/values?component=${SONARPROJECTKEY}:${SONARLEAKPARENTBRANCH}&keys=sonar.leak.period"
                echo leakurl
                curlcmd = "curl -k '${leakurl}' -o sonar.json"
                sh curlcmd
            } else {
                //for bat cmd, ^ character is required to escape &
                leakurl = env.SONAR_HOST_URL + "/api/settings/values?component=${SONARPROJECTKEY}:${SONARLEAKPARENTBRANCH}^&keys=sonar.leak.period"
                echo leakurl
                curlcmd = "curl -k ${leakurl} -o sonar.json"
                bat curlcmd
                def parentleak = readJSON file : 'sonar.json'
                echo parentleak.toString()
                if (parentleak.errors){
                    echo 'errors returned from parent check so we can assume parent stats dont exist' 
                } else {
                    baselineversion = parentleak.settings[0].value
                }
            }
        }
        if (baselineversion) {
            setSonarLeakBaselineVersion(baselineversion)
        }
    }
    
    def command
    if (buildengine == 'maven') {
        //command = "mvn -f ${POM} ${SONARGOAL} -Djavax.net.ssl.trustStore=D:/cert/cacerts -Dsonar.projectKey=${SONARPROJECTKEY} -Dsonar.projectName=${SONARPROJECTKEY} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.branch=${BRANCH_NAME}"
        if (isUnix()) {
            //command = "mvn -f ${POM} ${SONARGOAL} -Djavax.net.ssl.trustStore=/opt/cert/cacerts -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.java.binaries=."
			command = "mvn -f ${POM} ${SONARGOAL} -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.java.binaries=."
			if (exclusions) {
				command = "${command} -Djavax.net.ssl.trustStore=/opt/cert/cacerts --projects !$exclusions"
			}
        } else {
            command = "mvn -f ${POM} ${SONARGOAL} -Djavax.net.ssl.trustStore=D:/cert/cacerts -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.java.binaries=."
			if (exclusions) {
				command = "${command} -Djavax.net.ssl.trustStore=D:/cert/cacerts --projects !$exclusions"
			}
        }
    } else if (buildengine == 'ant') {
        command = "ant sonar -f sonarbuild.xml -Djavax.net.ssl.trustStore=D:/cert/cacerts -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.login=%SONAR_AUTH_TOKEN% -Dsonar.projectVersion=${PIPELINE_VERSION}"
    } else if (buildengine == 'msbuild') {
      	// Due to SONARMSBRU-307 value of sonar.host.url and credentials should be passed on command line
        //command = "${exe} begin /k:\"${SONARPROJECTKEY}\" /n:\"${SONARPROJECTKEY}\" /v:\"${PIPELINE_VERSION}\" /d:sonar.branch=%BRANCH_NAME% /d:sonar.host.url=%SONAR_HOST_URL% /d:sonar.login=%SONAR_AUTH_TOKEN% > sonar_bld.log"
        command = "${msbuildScannerExe} end >> sonar_bld.log"
    } else if (buildengine == 'jenkins') {
        //command = "sonar-scanner -Djavax.net.ssl.trustStore=D:/cert/cacerts -Dsonar.projectKey=${SONARPROJECTKEY} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.branch=${BRANCH_NAME} -Dsonar.sources=."
      	if (isUnix()) {
			command = "sonar-scanner -Djavax.net.ssl.trustStore=/opt/cert/cacerts -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.sources=."
        } else {
            command = "sonar-scanner -Djavax.net.ssl.trustStore=D:/cert/cacerts -Dsonar.projectKey=${SONARPROJECTNAME} -Dsonar.projectVersion=${PIPELINE_VERSION} -Dsonar.projectName=${SONARPROJECTNAME} -Dsonar.sources=."
        }
    }    

    withSonarQubeEnv('Sonar') {
        if (buildengine == 'maven') {
            withMaven(maven: mvnVer ,mavenSettingsConfig: '94e8f1b0-9ffd-431c-ab7d-88a104d37857', mavenLocalRepo: '.repository', jdk: jdkVer) {  
                if (isUnix()) {
                    sh command
                } else {
                    bat command
                }
                
            }
        } else if (buildengine == 'ant') {
            withEnv(["Path+JDK=$javaHome/bin","JAVA_HOME=$javaHome","Path+ANT=$antHome/bin","ANT_HOME=$antHome"]) {  
                if (isUnix()) {
                    sh command
                } else {
                    bat command
                }
            }
        } else if (buildengine == 'msbuild') {
            withEnv(["Path+MSBUILD=$msbuildHome","Path+MSBUILDSCANNER=$msbuildScannerHome"]) {  
                bat command
            }
        } else if (buildengine == 'jenkins') {
            echo "$jenkinsScannerHome"
            withEnv(["Path+SONAR=$jenkinsScannerHome/bin","JAVA_HOME=$javaHome"]) {  
                if (isUnix()) {
                    sh command
                } else {
                    bat command
                }
            }
        }
        //put the important sonarqube results info into environment variables so it can be picked up by later piepline stages
        def properties = new Properties()
        if (buildengine == 'maven') {
            properties.load(new StringReader(readFile("target/sonar/report-task.txt")))
          	env.SONAR_CE_TASK_URL = properties.getProperty('ceTaskUrl')
        } else if (buildengine == 'ant') {
            properties.load(new StringReader(readFile("../.sonar/report-task.txt")))
            env.SONAR_CE_TASK_URL = properties.getProperty('ceTaskUrl')
        } else if (buildengine == 'jenkins') {
            properties.load(new StringReader(readFile(".scannerwork/report-task.txt")))
            env.SONAR_CE_TASK_URL = properties.getProperty('ceTaskUrl')
        } else if (buildengine == 'msbuild') {
            properties.load(new StringReader(readFile(".sonarqube/out/.sonar/report-task.txt")))
            env.SONAR_CE_TASK_URL = properties.getProperty('ceTaskUrl')
        }
        env.SONAR_DASHBOARD_URL = properties.getProperty('dashboardUrl')
    }    
}
