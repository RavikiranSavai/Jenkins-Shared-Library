#!/usr/bin/env groovy

def call(String releasearea = 'smb') {
	if (env.BRANCH_NAME.matches("PR-.*")) {
		echo 'skipping push to drop location for pull request builds'
	} else if (releasearea == 'none') {
		echo 'skipping push to drop location as release area is none'
	} else {
		dir("${workspace}") {
			if (releasearea == 'smb') {
				if (isUnix()) {
					error("unix based slave, we dont have a solution for copying to the windows based releasearea share, switch to s3")
				} else {
					dir('_drop') {
						bat "robocopy . ${BUILD_OUTPUT} *.* /S & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
					}
				}	
			} else if (releasearea == 's3') {
				def branchescaped = "${BRANCH_NAME}".replace("/","")
				def versionescaped = "${PIPELINE_VERSION}".replace("/","")
				env.RELEASE_ARTEFACT_S3FOLDER = branchescaped + "/" + "${COMPONENT_FULL_NAME}" + "/" + versionescaped
				
				if (fileExists(file: '_drop/')) {
					echo 'drop staging folder found'
					withAWS(credentials:"NonProdJenkinsServiceAWS") {
						s3Upload bucket: "${RELEASE_ARTEFACT_S3BUCKET}", file: "_drop/", path: "${RELEASE_ARTEFACT_S3FOLDER}"
					}
				} else {
					echo 'No drop staging folder so nothing to push'
				}
				
			} 
		}
	}
}