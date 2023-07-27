#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call(Map map = [:]) {
	
	if (env.PIPELINE_VERSION){
		//version information not derived from jenkins job name
		echo 'custom pipeline version provided'
		env.BUILD_OUTPUT = ""
		env.BRANCH_NAME = "${PIPELINE_VERSION}".split('_').first()
		if (env.GIT_URL) {
			env.COMPONENT_FULL_NAME = "$GIT_REPO_NAME"
		} else {
			def branchcomponent = "${PIPELINE_VERSION}".split('\\.').first()
			env.COMPONENT_FULL_NAME = branchcomponent.replaceAll("${BRANCH_NAME}_","")
		}
			
	} else {
		env.JOB_SHORTNAME = "${JOB_NAME}".split('/').last()
		if (env.GIT_URL) {
			if (env.BRANCH_NAME) {
				//branch_name variable is only available from multi branch pipeline jobs
				echo "BRANCH_NAME env variable already known from multibranch: $BRANCH_NAME"
				//for PR builds, GIT_URL and GIT_REPO_NAME are null so we need to construct a valid name
				if (env.BRANCH_NAME.matches("PR-.*")) {
					env.SONARPROJECTKEY = "${APPLICATION_NAME}-${COMPONENT_NAME}"
					env.COMPONENT_FULL_NAME = "${APPLICATION_NAME}-${COMPONENT_NAME}"
				} else {
					env.SONARPROJECTKEY = "$GIT_REPO_NAME"
					env.COMPONENT_FULL_NAME = "$GIT_REPO_NAME"
				}
			} else {
				env.BRANCH_NAME = "${JOB_SHORTNAME}".split('_').first()	
				env.SONARPROJECTKEY = "${JOB_SHORTNAME}".replaceAll("${BRANCH_NAME}_","")
				env.COMPONENT_FULL_NAME = "${JOB_SHORTNAME}".replaceAll("${BRANCH_NAME}_","")
			}
			
		} else {
			//if branch name variable is empty lets take a guess at branch name from the Jenkins job name
			env.BRANCH_NAME = "${JOB_SHORTNAME}".split('_').first()
			echo "BRANCH_NAME env variable not automatically set so lets construct it from the Jenkins job name: $BRANCH_NAME"
			env.COMPONENT_FULL_NAME = "${JOB_SHORTNAME}".replaceAll("${BRANCH_NAME}_","")
			echo "COMPONENT_FULL_NAME : ${COMPONENT_FULL_NAME}"
		}
		env.PIPELINE_VERSION = "${BRANCH_NAME}_${COMPONENT_FULL_NAME}.${BUILD_NUMBER}"
		env.BUILD_OUTPUT = "${RELEASEAREA}\\${JOB_SHORTNAME}\\${PIPELINE_VERSION}"
	}
	
	echo "branch name is ${BRANCH_NAME}, pipeline version is ${PIPELINE_VERSION}, build output is ${BUILD_OUTPUT}"
	
	currentBuild.displayName = "#${PIPELINE_VERSION}"
}
