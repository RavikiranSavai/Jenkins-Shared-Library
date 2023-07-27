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
	
	//this job assumes the artefact is archived to the current running job so location of artefact is not specified
	build job: "Common/BeanstalkDeployFromUpstream_Deploy$deployscope", parameters: [string(name: 'EB_APPLICATION_NAME', value: "${BEANSTALK_APP_NAME}"),string(name: 'EB_ENVIRONMENT_NAME', value: "${TARGET_SERVER}"),string(name: 'VERSION', value: version),string(name: 'UPSTREAM_PROJECT', value: "${JOB_NAME}"),string(name: 'ARTEFACT_NAME', value: "${BUNDLE_NAME}.zip")]
}
