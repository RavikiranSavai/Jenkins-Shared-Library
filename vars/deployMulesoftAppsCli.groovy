#!/usr/bin/env groovy

def call() {
  	unstash 'stageartefact'
	wrap([$class: 'AnsiColorBuildWrapper']) {
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${JENKINS_CRED_SCOPE}JenkinsServiceMuleSoft",
        usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
          def functionMap = readJSON file: "${workspace}/deploy/${TARGET_ENVIRONMENT}/deploy-runtime/Manifests/${APPLICATION_NAME}-${COMPONENT_NAME}_MuleDeploy.json"
          if (isUnix()) {
              sh "anypoint-cli runtime-mgr cloudhub-application modify ${functionMap.applicationName[0]} ${workspace}/deploy/artefacts/staging/${functionMap.artefactName[0]} --runtime ${functionMap.muleVersion[0]} --workers ${functionMap.workers[0]} --workerSize ${functionMap.workerSize[0]} --username ${USERNAME} --password ${PASSWORD} --organization \"${functionMap.businessGroup[0]}\" --environment ${functionMap.environment[0]} --region ${functionMap.region[0]}"
          } else {
              bat "anypoint-cli runtime-mgr cloudhub-application modify ${functionMap.applicationName[0]} ${workspace}/deploy/artefacts/staging/${functionMap.artefactName[0]} --runtime ${functionMap.muleVersion[0]} --workers ${functionMap.workers[0]} --workerSize ${functionMap.workerSize[0]} --username ${USERNAME} --password ${PASSWORD} --organization \"${functionMap.businessGroup[0]}\" --environment ${functionMap.environment[0]} --region ${functionMap.region[0]}"
          }
      }
  	}
}
