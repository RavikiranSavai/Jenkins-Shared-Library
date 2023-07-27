#!/usr/bin/env groovy

def call(Map map = [:]) {
	//default to s3 for artefact repo
	def deployscope = "${map.deployscope ?: 'NONPROD'}"	
	def version
	
	//if pipeline version has forward slash from git branch name then we need to replace it as beanstalk will not accept it
	if ("$PIPELINE_VERSION".contains("/")){
		version = "$PIPELINE_VERSION".replaceAll("/", "-")
	} else {
		version = "$PIPELINE_VERSION"	
	}
	if (env.IsMaintenance == 'true'){
  		echo "Is Maintenance Needed ?? - ${IsMaintenance}"
		version = version+"_Maint_"+env.buildver
	}
	if (env.IsRedeployment == 'true'){
		echo "Is Redeployment Needed ?? - ${IsRedeployment}"
		version = version+"_"+env.buildver
	}

	if (env.BUNDLE_NAME) {		 
	} else {
		env.BUNDLE_NAME = "${BEANSTALK_APP_NAME}"
	}

	echo 'calling beanstalk deploy job..'
	echo "name: 'EB_APPLICATION_NAME', value: ${BEANSTALK_APP_NAME}"
	echo "name: 'EB_ENVIRONMENT_NAME', value: ${TARGET_SERVER}"
	echo "name: 'VERSION', value: ${version}"
	
	if (deployscope == "NONPROD") {
		env.Deploy_Bucket = "elasticbeanstalk-eu-west-1-494230530197"
		env.Profile = "Dev"
		env.scope = "NonProd"
	} else if(deployscope == "PROD") {
		env.Deploy_Bucket = "elasticbeanstalk-eu-west-1-172330397760"
		env.Profile = "PROD"
		env.scope = "Prod"
	} else if(deployscope == "NONPRODPS") {
		env.Deploy_Bucket = "elasticbeanstalk-eu-west-1-855078055508"
		env.Profile = "PSDev"
		env.scope = "PSNonProd"
	} else if(deployscope == "PRODLegacy") {
		env.Deploy_Bucket = "elasticbeanstalk-us-east-1-470977673322"
		env.Profile = "Legacy"
		env.scope = "Legacy"
	} else if(deployscope == "NONPRODLegacy") {
		env.Deploy_Bucket = "elasticbeanstalk-us-east-1-470977673322"
		env.Profile = "Legacy"
		env.scope = "Legacy"
	}
 
  	unstash 'stageartefact'

  	withAWS(credentials:"${scope}JenkinsServiceAWS") {
		bat "aws s3 cp  ${workspace}/deploy/artefacts/staging/${BUNDLE_NAME}.zip s3://${Deploy_Bucket}/${TARGET_SERVER}/${TARGET_SERVER}-${version}.zip"
		bat "aws elasticbeanstalk create-application-version --application-name ${BEANSTALK_APP_NAME} --version-label ${TARGET_SERVER}-${version} --source-bundle S3Bucket=\"${Deploy_Bucket}\",S3Key=\"${TARGET_SERVER}/${TARGET_SERVER}-${version}.zip\""
	}
	build job: "Common/BeanstalkDeployFromUpstream_Deploy", parameters: [string(name: 'TARGET_SERVER', value: "${TARGET_SERVER}"),string(name: 'VERSION', value: "${version}"),string(name: 'PROFILE', value: "${Profile}")]
}