#!/usr/bin/env groovy

def call() {
	deleteDir()
  	dir ("deploy") {
  		unstash 'stageartefact'
	}
	wrap([$class: 'AnsiColorBuildWrapper']) {
        def functionMap = readJSON file: "${workspace}/deploy/${TARGET_ENVIRONMENT}/deploy-runtime/Manifests/${APPLICATION_NAME}-${COMPONENT_NAME}_ECSCLWDeploy.json"
		env.AWS_ECS_CLUSTER = functionMap.AWS_ECS_CLUSTER[0]
		env.AWS_ECS_SERVICE = functionMap.AWS_ECS_SERVICE[0]
		env.AWS_ECS_TASK_DEFINITION = functionMap.AWS_ECS_TASK_DEFINITION[0]
		env.AWS_ECR_REGION = functionMap.AWS_ECR_REGION[0]
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
				 error("Linux Platform not supported for ECS CloudWatch Deployment")
            } else {
				bat "cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & \"C:/Program Files/PowerShell/7/pwsh\" -f update_ecs_taskdef.ps1 -manifest %workspace%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/Manifests/%APPLICATION_NAME%-%COMPONENT_NAME%_ECSCLWDeploy.json -workspace %WORKSPACE% -imageurn %Container_ECR_URL%/%ECR_REPO%:%ECR_TAG% -awsecscluster %AWS_ECS_CLUSTER% -awsecsservice %AWS_ECS_SERVICE% -awsecstaskdef %AWS_ECS_TASK_DEFINITION% -awsecrregion %AWS_ECR_REGION%"
            }
			for (int i = 0; i < functionMap.size(); i++) {
				env.CloudWatchEvnName = "${functionMap.CloudWatchEventName[i]}"
				bat "cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & \"C:/Program Files/PowerShell/7/pwsh\" -f update_clw_taskdefinitionarn.ps1 -workspace %WORKSPACE% -cloudwatchname %CloudWatchEvnName%"
			}
		}
  	}
}
