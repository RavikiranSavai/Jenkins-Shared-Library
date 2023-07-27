#!/usr/bin/env groovy

def call(Map map = [:]) {
	//default to s3 for artefact repo
	def deployscope = "${map.deployscope ?: 'NONPROD'}"	

	//have a guess at codedeploy app name if not specifi
	if (env.CODEDEPLOY_APP_NAME) {		 
	} else {
		env.CODEDEPLOY_APP_NAME = "${APPLICATION_NAME}.${COMPONENT_NAME}"
	}

	echo 'calling codedeploy job..'
	echo "name: 'CODEDEPLOY_APPLICATION_NAME', value: ${CODEDEPLOY_APP_NAME}"
	echo "name: 'CODEDEPLOY_DEPLOYMENT_GROUP', value: ${TARGET_SERVER}"
	echo "name: 'VERSION', value: $PIPELINE_VERSION"

	//this job assumes the artefact is archived to the current running job so location of artefact is not specified
	build job: "Common/CodeDeployFromUpstream_Deploy$deployscope", parameters: [string(name: 'CODEDEPLOY_APPLICATION_NAME', value: "${CODEDEPLOY_APP_NAME}"),string(name: 'CODEDEPLOY_DEPLOYMENT_GROUP', value: "${TARGET_SERVER}"),string(name: 'UPSTREAM_PROJECT', value: "${JOB_NAME}")]
}