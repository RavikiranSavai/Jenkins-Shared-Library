#!/usr/bin/env groovy

def call(Map map = [:]) {
	//default to s3 for artefact repo
	def deployscope = "${map.deployscope ?: 'NONPROD'}"	

	echo 'calling cloudfront deploy job..'
	echo "name: 'ARTEFACT_NAME', value: ${PROJECT_NAME}.zip"
	echo "name: 'S3_BUCKET', value: ${TARGET_SERVER}"
	echo "name: 'DISTRIBUTION_ID', value: ${DISTRIBUTION_ID}"
	echo "name: 'JENKINS_CRED_SCOPE', value: ${JENKINS_CRED_SCOPE}"

	//this job assumes the artefact is archived to the current running job so location of artefact is not specified
	build job: "Common/CloudFrontDeployFromUpstream_Deploy$deployscope", parameters: [string(name: 'UPSTREAM_PROJECT', value: "${JOB_NAME}"),string(name: 'ARTEFACT_NAME', value: "${PROJECT_NAME}.zip"),string(name: 'S3_BUCKET', value: "$TARGET_SERVER"),string(name: 'DISTRIBUTION_ID', value: "$DISTRIBUTION_ID"),string(name: 'JENKINS_CRED_SCOPE', value: "$JENKINS_CRED_SCOPE")]
}