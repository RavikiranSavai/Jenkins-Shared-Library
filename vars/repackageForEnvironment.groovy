#!/usr/bin/env groovy

def call(Map map = [:]) {
	//default to s3 for artefact repo
	def releasearea = "${map.releasearea ?: 'smb'}"	
	def artefactlink = "${map.artefactlink ?: 'archive'}"
	def deployorchestrator = "${map.deployorchestrator ?: 'beanstalk'}"
	def apptype = "${map.apptype ?: 'general'}"

	if (env.ARTEFACT_EXTENSION) {		 
	} else {
		env.ARTEFACT_EXTENSION = ".zip"
	}
	//create a staging area in the workspace specific to the target env
	dir ("deploy") {
		deleteDir()
		dir ("${TARGET_ENVIRONMENT}") {
		}
		dir ("artefacts") {
		}
	}
  	dir ("s3") {
		deleteDir()
	}
	if(releasearea=='s3'){
		//download artifacts from s3
		withAWS(credentials:"NonProdJenkinsServiceAWS") {
			s3Download(file:'s3/', bucket:"${RELEASE_ARTEFACT_S3BUCKET}", path:"${RELEASE_ARTEFACT_S3FOLDER}/", force:true)
		}
		//copy files downloaded from S3 to generic folder location that works for both s3 and file share
		fileOperations([folderCopyOperation(destinationFolderPath: 'deploy/artefacts', sourceFolderPath: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/artefacts")])
		if (fileExists(file: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-jenkins-deployframework.txt")) {
			fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**/s3/${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-jenkins-deployframework.txt", targetLocation: "deploy/${TARGET_ENVIRONMENT}/deploy-runtime")])
		} else {
			fileOperations([folderCopyOperation(destinationFolderPath: "deploy/${TARGET_ENVIRONMENT}/deploy-runtime", sourceFolderPath: "${WORKSPACE}/s3/${RELEASE_ARTEFACT_S3FOLDER}/deploy-runtime")])
		}
	} else if (releasearea=='smb') {
		//download artifacts from network file share
		dir ("deploy") {
			dir ("${TARGET_ENVIRONMENT}") {
				if (fileExists(file: "${BUILD_OUTPUT}/nexusversion-jenkins-deployframework.txt")) {
					fileOperations([folderCopyOperation(destinationFolderPath: "deploy-runtime", sourceFolderPath: "${BUILD_OUTPUT}/nexusversion-jenkins-deployframework.txt")])
				} else {
					fileOperations([folderCopyOperation(destinationFolderPath: "deploy-runtime", sourceFolderPath: "${BUILD_OUTPUT}/deploy-runtime")])
				}
			}
			dir ("artefacts") {
				deleteDir()
				bat "robocopy ${BUILD_OUTPUT}/artefacts . *.* & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
				if (fileExists(file: "${BUILD_OUTPUT}/artefacts/${deployorchestrator}_files")) {
					echo "${deployorchestrator}_files folder found so lets copy it to staging area"
					dir ("${deployorchestrator}_files") {
						bat "robocopy /E ${BUILD_OUTPUT}/artefacts/${deployorchestrator}_files . *.* /E /XF ${BUILD_OUTPUT}/artefacts/${deployorchestrator}_files/aws-windows-deployment-manifest.json & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
					}
                  	if (fileExists(file: "${BUILD_OUTPUT}/artefacts/${deployorchestrator}_files/aws-windows-deployment-manifest.json")) {
                    	bat "robocopy /E ${BUILD_OUTPUT}/artefacts/${deployorchestrator}_files . aws-windows-deployment-manifest.json & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
                    }
				}
			}
		}		
	}
	//pull deployment framework from nexus if file exists containing download url
	dir ("deploy/${TARGET_ENVIRONMENT}/deploy-runtime") {
		if (fileExists(file: "nexusversion-jenkins-deployframework.txt")) {
			if (env.DEPLOY_FRAMEWORK_OVERRIDE) {
				pullArtefactFromNexus(version:"${DEPLOY_FRAMEWORK_OVERRIDE}")
			} else {
				pullArtefactFromNexus()
			}
		}
	}

	lookupDeployTarget()	
	
	if(releasearea=='s3'){
		if (env.BUNDLE_NAME) {
		} else {
			env.BUNDLE_NAME = "${APPLICATION_NAME}.${COMPONENT_NAME}"	
		}
		if (apptype=='general') {
			//create the detokenised package in the staging folder in the jenkins workspace only
			withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'Jenkins_Deployment_Local_Account',
			usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
				bat 'cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & powershell -f pipeline_deploy.ps1 -username %USERNAME% -password %PASSWORD% -deploy_scope componentlist -component_list %COMPONENT_LIST% -environment_name %TARGET_ENVIRONMENT% -version %PIPELINE_VERSION% -norepo -nodeploy'
			}		
			//when switching to using s3 for storing release artefacts, we attach the detokenised artefacts to the running job 
			//rather than pushing it back to s3 as typcially artefacts get pushed to s3 as part of the deploy action itself
			if (artefactlink=='archive') {
				dir('deploy/artefacts/staging') {
					archiveArtifacts artifacts: "**/${env.BUNDLE_NAME}${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
				}
				stash excludes: '**/.git/**,**/target/**,**/.repository/**,**/s3/**,**/_drop/**', includes: "**/*.*", name: 'stageartefact'
			} else if (artefactlink=='stash') {
				dir('deploy/artefacts/staging') {
					stash includes: "**/${env.BUNDLE_NAME}${ARTEFACT_EXTENSION}", name: 'artefact'
                  	archiveArtifacts artifacts: "**/${env.BUNDLE_NAME}${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
				}
			} else if (artefactlink=='s3stash') {
				dir ("deploy/artefacts/staging") {
					fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "*${ARTEFACT_EXTENSION}", targetLocation: "${WORKSPACE}/deploy/${TARGET_ENVIRONMENT}")])
				}
				dir('deploy') {
                  	dir("${TARGET_ENVIRONMENT}") {
                      	archiveArtifacts artifacts: "**/${env.BUNDLE_NAME}${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
                  	}
					stash includes: "**/*.*", name: 'artefact'
				}
			} else if (artefactlink=='fullstash') {
				dir ("deploy") {
					dir("artefacts") {
						archiveArtifacts artifacts: "**/**${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
					}
				}
				stash excludes: '**/.git/**,**/target/**,**/.repository/**,**/s3/**,**/_drop/**', includes: "**/*.*", name: 'stageartefact'
			}
		} else {
			if (artefactlink=='fullstash') {
				dir ("deploy") {
					dir("artefacts") {
						archiveArtifacts artifacts: "**/**${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
					}
				}
				stash excludes: '**/.git/**,**/target/**,**/.repository/**,**/s3/**,**/_drop/**', includes: "**/*.*", name: 'stageartefact'
			} else {
				dir ("deploy") {
					stash includes: "**/*.*", name: 'stageartefact'
					dir("artefacts") {
						archiveArtifacts artifacts: "**/**${ARTEFACT_EXTENSION}", caseSensitive: false, defaultExcludes: false
					}
				}
			}
		}
	} else if (releasearea=='smb') {
		//create the detokenised package and ensure its pushed back to the fileshare
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'Jenkins_Deployment_Local_Account',
		usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
			bat 'cd %WORKSPACE%/deploy/%TARGET_ENVIRONMENT%/deploy-runtime/scripts & powershell -f pipeline_deploy.ps1 -username %USERNAME% -password %PASSWORD% -deploy_scope componentlist -component_list %COMPONENT_LIST% -environment_name %TARGET_ENVIRONMENT% -version %PIPELINE_VERSION% -remoteartefactlocation %BUILD_OUTPUT%/artefacts -nodeploy'
		}
    }
}
