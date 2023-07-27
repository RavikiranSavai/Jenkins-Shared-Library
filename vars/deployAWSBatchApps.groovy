#!/usr/bin/env groovy

def call() {
	deleteDir()
  	dir ("deploy") {
  		unstash 'stageartefact'
	}
	wrap([$class: 'AnsiColorBuildWrapper']) {
        def functionMap = readJSON file: "${workspace}/deploy/${TARGET_ENVIRONMENT}/deploy-runtime/Manifests/${APPLICATION_NAME}-${COMPONENT_NAME}_BatchDeploy.json"
		if ("$PIPELINE_VERSION".contains("/")){
			env.ECR_TAG = "$PIPELINE_VERSION".replaceAll("/", "-")
		} else {
			env.ECR_TAG = "$PIPELINE_VERSION"	
		}
      	if (env.Container_ECR_URL) { 
      	} else {  
            env.Container_ECR_URL = "$ECR_URL".replaceAll("https://","")
            env.Container_ECR_URL = "$Container_ECR_URL".replaceAll("/","")
      	}
		withAWS(credentials:"${JENKINS_CRED_SCOPE}JenkinsServiceAWS") {
            if (isUnix()) {
				 error("Linux Platform not supported for Batch Deployment")
            } else {
				bat "cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & \"C:/Program Files/PowerShell/7/pwsh\" -f update_batch_imageurn.ps1 -manifest %workspace%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/Manifests/%APPLICATION_NAME%-%COMPONENT_NAME%_BatchDeploy.json -workspace %WORKSPACE% -imageurn %Container_ECR_URL%/%ECR_REPO%:%ECR_TAG%"
            }
			for (int i = 0; i < functionMap.size(); i++) {
				env.CloudWatchEvnName = "${functionMap.CloudWatchEventName[i]}"
				bat "cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & \"C:/Program Files/PowerShell/7/pwsh\" -f update_clw_jobdefinitionarn.ps1 -workspace %WORKSPACE% -cloudwatchname %CloudWatchEvnName%"
			}
		}
  	}
}
