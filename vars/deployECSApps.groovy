#!/usr/bin/env groovy

def call() {
  	dir ("deploy") {
		dir ("${TARGET_ENVIRONMENT}") {
			deleteDir()
		}
  		unstash 'stageartefact'
	}
	wrap([$class: 'AnsiColorBuildWrapper']) {
        def functionMap = readJSON file: "${workspace}/deploy/${TARGET_ENVIRONMENT}/deploy-runtime/Manifests/${APPLICATION_NAME}-${COMPONENT_NAME}_ECSDeploy.json"
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
				sh "aws ecs describe-task-definition --task-definition ${AWS_ECS_TASK_DEFINITION} --output json | jq '.taskDefinition.containerDefinitions[0]' | jq -s . >container-definition-update-image.json"
                def containerDefinitionJson = readJSON file: container-definition-update-image.json, returnPojo: true
    			containerDefinitionJson[0]['image'] = "${Container_ECR_URL}/${ECR_REPO}:${ECR_TAG}".inspect()
   				echo "task definiton json: ${containerDefinitionJson}"
    			writeJSON file: container-definition-update-image.json, json: containerDefinitionJson
                sh("aws ecs register-task-definition --region ${AWS_ECR_REGION} --family ${AWS_ECS_TASK_DEFINITION} --execution-role-arn ${AWS_ECS_EXECUTION_ROLE} --requires-compatibilities ${AWS_ECS_COMPATIBILITY} --network-mode ${AWS_ECS_NETWORK_MODE} --cpu ${AWS_ECS_CPU} --memory ${AWS_ECS_MEMORY} --container-definitions file://container-definition-update-image.json")
                def taskRevision = sh(script: "aws ecs describe-task-definition --task-definition ${AWS_ECS_TASK_DEFINITION} | egrep \"revision\" | tr \"/\" \" \" | awk '{print \$2}' | sed 's/\"\$//'", returnStdout: true)\
				sh("aws ecs update-service --cluster ${AWS_ECS_CLUSTER} --service ${AWS_ECS_SERVICE} --task-definition ${AWS_ECS_TASK_DEFINITION}:${taskRevision}")
            } else {
				bat "cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & \"C:/Program Files/PowerShell/7/pwsh\" -f update_ecr_imageurn.ps1 -manifest %workspace%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/Manifests/%APPLICATION_NAME%-%COMPONENT_NAME%_ECSDeploy.json -workspace %WORKSPACE% -imageurn %Container_ECR_URL%/%ECR_REPO%:%ECR_TAG% -awsecscluster %AWS_ECS_CLUSTER% -awsecsservice %AWS_ECS_SERVICE% -awsecstaskdef %AWS_ECS_TASK_DEFINITION% -awsecrregion %AWS_ECR_REGION%"
            }
        }
  	}
}
