#!/usr/bin/env groovy

def call() {
  	
 	if (env.ARTEFACT_EXTENSION) {		 
	} else {
		env.ARTEFACT_EXTENSION = ".war"
	}
  	if (env.JENKINS_CRED_SCOPE) {		 
	} else {
		env.JENKINS_CRED_SCOPE = "NonProd"
	}
  
  	dir ("deploy") {
		deleteDir()
  		unstash 'artefact'

		//hit target server with basic ssh command to add to trusted hosts
		withCredentials([sshUserPrivateKey(credentialsId: "${JENKINS_CRED_SCOPE}JenkinsSSHRoot", keyFileVariable: 'keyfile', usernameVariable: 'user')]) {
			bat "ssh -i ${keyfile} ${user}@${TARGET_SERVER} -T -oStrictHostKeyChecking=no"
        	bat "ssh -i ${keyfile} ${user}@${TARGET_SERVER} sudo chown -R ${user} /intellect/tomcat8"
		}
		echo "attempting to copy ${BUNDLE_NAME}${ARTEFACT_EXTENSION} to ${TARGET_SERVER}"
		withCredentials([sshUserPrivateKey(credentialsId: "${JENKINS_CRED_SCOPE}JenkinsSSHRoot", keyFileVariable: 'keyfile', usernameVariable: 'user')]) {
			bat "scp -i ${keyfile} %WORKSPACE%/deploy/${BUNDLE_NAME}${ARTEFACT_EXTENSION} ${user}@${TARGET_SERVER}:/intellect/tomcat8/webapps"
			bat "ssh -i ${keyfile} ${user}@${TARGET_SERVER} sudo /intellect/tomcat8/bin/shutdown.sh"
			bat "ssh -i ${keyfile} ${user}@${TARGET_SERVER} sudo /intellect/tomcat8/bin/startup.sh"
		}
    }
}
