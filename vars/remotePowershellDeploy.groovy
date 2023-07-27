#!/usr/bin/env groovy
/**
 * perform deployment to remote target using remote powershell (winrm)
 */
def call(Map map = [:]) {
	def releasearea = "${map.releasearea ?: 'smb'}"	
    //create a staging area in the workspace specific to the target env
	def ARTEFACT_PATH
    dir ("deploy") {
        if (releasearea=='smb') {
        	dir ("${TARGET_ENVIRONMENT}") {
          		deleteDir()
                //check for nexus info
                if (fileExists(file: "${BUILD_OUTPUT}/nexusversion-jenkins-deployframework.txt")) {
					bat "robocopy ${BUILD_OUTPUT} deploy-runtime nexusversion-jenkins-deployframework.txt & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
                    dir('deploy-runtime'){
						if (env.DEPLOY_FRAMEWORK_OVERRIDE) {
							pullArtefactFromNexus(version:"${DEPLOY_FRAMEWORK_OVERRIDE}")
						} else {
							pullArtefactFromNexus()
						}
                    }
				} else {
					bat "robocopy /E ${BUILD_OUTPUT}/deploy-runtime deploy-runtime *.* & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
				}
				ARTEFACT_PATH = "${BUILD_OUTPUT}/artefacts"
			}
        } else {
         	deleteDir()
			unstash 'artefact'
			ARTEFACT_PATH = "${WORKSPACE}/deploy"
        }
    }
    if (env.COMPONENT_LIST) {
    } else {
        env.COMPONENT_LIST = "${APPLICATION_NAME}.${COMPONENT_NAME}"
    }
	
    if (env.JENKINS_CRED_SCOPE) {
    } else {
      env.JENKINS_CRED_SCOPE = "NonProd"
    }
    dir ("deploy/${TARGET_ENVIRONMENT}/deploy-runtime/scripts") {
      //get credentials from jenkins to pass to powershell script
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${JENKINS_CRED_SCOPE}Jenkins_Deployment_Local_Account",
        usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            bat "powershell -f pipeline_deploy.ps1 -username ${USERNAME} -password ${PASSWORD} -deploy_scope componentlist -component_list ${COMPONENT_LIST} -environment_name ${TARGET_ENVIRONMENT} -version ${PIPELINE_VERSION} -target_artefact_staging ${TARGET_ARTEFACT_STAGING} -remoteartefactlocation ${ARTEFACT_PATH} -noprepare"
      }
    }
}
