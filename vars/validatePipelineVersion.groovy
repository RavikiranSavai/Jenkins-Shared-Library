#!/usr/bin/env groovy

def call(String releasearea = 'smb') {
	def version = "${PIPELINE_VERSION}".trim()
	if (version.startsWith("#")){
		echo 'stripping # from pipeline version'
		version = version.substring(1)
	}
	env.PIPELINE_VERSION = version
	echo "pipeline version provided by user: ${PIPELINE_VERSION}"
	currentBuild.displayName = "#${PIPELINE_VERSION}"

	if (releasearea == 'smb'){
		//derive build output location from version
		def upstreamjob = version.substring(0, version.lastIndexOf("."))
		env.BUILD_OUTPUT = "${RELEASEAREA}\\" + upstreamjob + "\\${PIPELINE_VERSION}" 
		echo "build output: ${BUILD_OUTPUT}"
		//check build output location actually exists, if not the pipeline version provided by the user does not exist
		if (fileExists(file: "${BUILD_OUTPUT}")) {
			echo 'verified that build output folder exists in release area'
		} else {
			error 'no build output folder exists for this pipeline version, please check you are using the correct version number'
		}		

	} else if (releasearea == 's3'){
		//derive branch name from version
      	env.BRANCH_NAME = "${PIPELINE_VERSION}".split('_').first()  
		def branchcomponent = "${PIPELINE_VERSION}".replaceAll("${BRANCH_NAME}_","")
		env.COMPONENT_FULL_NAME = branchcomponent.split('\\.').first()
		def branchescaped = "${BRANCH_NAME}".replace("/","")
		def versionescaped = "${PIPELINE_VERSION}".replace("/","")
		env.RELEASE_ARTEFACT_S3FOLDER = branchescaped + "/" + "${COMPONENT_FULL_NAME}" + "/" + versionescaped

		echo "build output, s3 bucket: ${RELEASE_ARTEFACT_S3BUCKET}, folder: ${RELEASE_ARTEFACT_S3FOLDER}"
		withAWS(credentials:"NonProdJenkinsServiceAWS") {
			revisionfile = s3FindFiles(bucket:"${RELEASE_ARTEFACT_S3BUCKET}", path:"${RELEASE_ARTEFACT_S3FOLDER}", glob:'revision.txt')
		}
		//check build output location actually exists, if not the pipeline version provided by the user does not exist
		if (revisionfile) {
			echo 'verified that build output folder exists in release area'
		} else {
			error 'no build output folder exists for this pipeline version, please check you are using the correct version number'
		}
	}	
}
