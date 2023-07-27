#!/usr/bin/env groovy

/**
 * Archive the latest version number (url only) of the deploy framework for use by downstream deploy stages/jobs
 */
def call(String deployframework) {	

	//for cicd build, archive the current latest version of the deploy framework stored in nexus
	if (deployframework != 'pipelineversion'){
		archiveNexusVersionInfo()
	}

	//some cicd jobs like mp ui actually need the deploy framework extracted as there is config file
	//tokenisation that needs to be done before the compile stage
	if (deployframework == 'extract'){
		dir('deployframework'){
			pullArtefactFromNexus(filecontainingurl:"${workspace}/_drop/nexusversion-jenkins-deployframework.txt")
		}
	}
	if (deployframework == 'pipelineversion'){
		withAWS(credentials:"NonProdJenkinsServiceAWS") {
			s3Download(file:'s3/nexusversion-jenkins-deployframework.txt', bucket:"${RELEASE_ARTEFACT_S3BUCKET}", path:"${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-jenkins-deployframework.txt", force:true)
		}
		if (fileExists(file: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-jenkins-deployframework.txt")) {
			fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**/s3/${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-jenkins-deployframework.txt", targetLocation: "${workspace}/deployframework")])
		} else {
			if (fileExists(file: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/deploy-runtime/nexusversion-jenkins-deployframework.txt")) {
				fileOperations([folderCopyOperation(destinationFolderPath: "${workspace}/deployframework/nexusversion-jenkins-deployframework.txt", sourceFolderPath: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/deploy-runtime")])
			} else {
				fileOperations([folderCopyOperation(destinationFolderPath: "${workspace}/deployframework", sourceFolderPath: "${WORKSPACE}/s3")])
			}
		}
		dir('deployframework'){			
			if (env.DEPLOY_FRAMEWORK_OVERRIDE) {
				pullArtefactFromNexus(version:"${DEPLOY_FRAMEWORK_OVERRIDE}")
			} else {
				pullArtefactFromNexus(filecontainingurl:"${workspace}/s3/nexusversion-jenkins-deployframework.txt")
			}
		}
	}
}
