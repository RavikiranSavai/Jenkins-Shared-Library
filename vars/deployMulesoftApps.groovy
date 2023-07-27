#!/usr/bin/env groovy

def call() {
  	unstash 'stageartefact'

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${JENKINS_CRED_SCOPE}JenkinsServiceMuleSoft",
      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

		def functionMap = readJSON file: "${workspace}/deploy/${TARGET_ENVIRONMENT}/deploy-runtime/Manifests/${APPLICATION_NAME}-${COMPONENT_NAME}_MuleDeploy.json"

		withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '442af402-463c-4167-98f1-558355e679cc', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
			if (isUnix()) {
				sh "mvn -f pom.xml -P jenkins clean package -U --batch-mode -Danypoint.uri=${functionMap.anypointURI[0]} -Danypoint.muleVersion=${functionMap.muleVersion[0]} -Danypoint.applicationName=${functionMap.applicationName[0]} -Danypoint.businessGroup=\"${functionMap.businessGroup[0]}\" -Danypoint.username=${USERNAME} -Danypoint.password=${PASSWORD} -Dcloudhub.env=${functionMap.environment[0]} -Dcloudhub.region=${functionMap.region[0]} -Dcloudhub.workerType=${functionMap.workerType[0]} -Dcloudhub.workers=${functionMap.workers[0]} mule:deploy -P cloudhub-deploy -DskipTests -Dmaven.test.skip=true"
			} else {
				bat "mvn -f pom.xml -P jenkins clean package -U --batch-mode -Danypoint.uri=${functionMap.anypointURI[0]} -Danypoint.muleVersion=${functionMap.muleVersion[0]} -Danypoint.applicationName=${functionMap.applicationName[0]} -Danypoint.businessGroup=\"${functionMap.businessGroup[0]}\" -Danypoint.username=${USERNAME} -Danypoint.password=${PASSWORD} -Dcloudhub.env=${functionMap.environment[0]} -Dcloudhub.region=${functionMap.region[0]} -Dcloudhub.workerType=${functionMap.workerType[0]} -Dcloudhub.workers=${functionMap.workers[0]} mule:deploy -P cloudhub-deploy -DskipTests -Dmaven.test.skip=true"
			}
		}
	}
}
