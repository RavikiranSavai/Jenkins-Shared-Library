#!/usr/bin/env groovy

def call(Map map = [:]) {
    def sonar = "${map.sonar ?: 'true'}"
	def publish = "${map.publish ?: 'true'}"
	def packageonly = "${map.packageonly ?: 'false'}"
	def webdeploypackage = "${map.webdeploypackage ?: 'false'}"
	def iispath = "${map.iispath ?: 'Default Web Site'}"
	def awscodedeploy = "${map.awscodedeploy ?: 'false'}"
	def releasearea = "${map.releasearea ?: 'smb'}"
	def additonalargs = "${map.additonalargs ?: ''}"

    echo "sonar : ${sonar}, publish : ${publish}, webdeploypackage : ${webdeploypackage}, iispath : ${iispath}, packageonly : ${packageonly}"

	dir("target") {
		deleteDir()
	}
	if (sonar == 'true') {
		echo 'starting msbuild scanner..'
		def scannerexe
		def msbuildScannerHome
      	env.SONAR_SCANNER_OPTS = "-Djavax.net.ssl.trustStore=D:/cert/cacerts"
		if (env.TOOL_SONARMSBUILD) {
			echo 'sonar msbuild version explicitly specified'
			msbuildScannerHome = tool "${env.TOOL_SONARMSBUILD}"
			scannerexe = 'SonarQube.Scanner.MSBuild.exe'
		} else {
			msbuildScannerHome = tool 'SonarMSBuild-45'
			scannerexe = 'SonarScanner.MSBuild.exe'
		}

		//start msbuild scanner
		withSonarQubeEnv('Sonar') {
            //bat "\"${msbuildScannerHome}/${scannerexe}\" begin /k:\"${APPLICATION_NAME}-${COMPONENT_NAME}\" /n:\"${APPLICATION_NAME}-${COMPONENT_NAME}\" /v:\"${PIPELINE_VERSION}\" /d:sonar.branch=${BRANCH_NAME} ${additonalargs} > sonar_bld.log"
          	env.SONARPROJECTNAME = BRANCH_NAME.replace('/',':')
          	env.SONARPROJECTNAME = "${APPLICATION_NAME}-${COMPONENT_NAME}:${SONARPROJECTNAME}"
          	if (env.TEST_PROJECT_NAME) {
          		bat "\"${msbuildScannerHome}/${scannerexe}\" begin /k:\"${SONARPROJECTNAME}\" /n:\"${SONARPROJECTNAME}\" /v:\"${PIPELINE_VERSION}\" /d:\"sonar.cs.opencover.reportsPaths=opencover.xml\" ${additonalargs} > sonar_bld.log"
            } else {
              	bat "\"${msbuildScannerHome}/${scannerexe}\" begin /k:\"${SONARPROJECTNAME}\" /n:\"${SONARPROJECTNAME}\" /v:\"${PIPELINE_VERSION}\" ${additonalargs} > sonar_bld.log"
            }
		}
	}
	//need to run clean as a standalone target as it causes issues in a one liner with webdeploy options
	bat "\"${tool 'MSBuild-14'}msbuild.exe\" /t:Clean /p:Configuration=Release /p:Platform=\"Any CPU\" "
	dir ("${PROJECT_NAME}/_PublishedWebsites"){
		deleteDir()
	}
	if (fileExists(file: 'nuget.config')) {
		bat "nuget.exe restore"
	}
	if (publish == 'true') {
		//call msbuild. 'DeployOnBuild' generates a webdeploy package that although we're not using, ensures config file transformations are run to inject tokens				
		//if a Jenkins publish profile exists lets use it
		if (fileExists(file: "${PROJECT_NAME}/Properties/PublishProfiles/Jenkins.pubxml")) {
			echo 'using Jenkins publish profile'
			bat "\"${tool 'MSBuild-14'}msbuild.exe\" /p:PublishProfile=Jenkins /p:DeployOnBuild=true /p:DeployIisAppPath=\"${iispath}\" /p:OutDir=. /p:Configuration=Release /p:Platform=\"Any CPU\" "
		} else {
			bat "\"${tool 'MSBuild-14'}msbuild.exe\" /p:DeployOnBuild=true /p:DeployIisAppPath=\"${iispath}\" /p:OutDir=. /p:Configuration=Release /p:Platform=\"Any CPU\" "
		}
	} else {
		bat "\"${tool 'MSBuild-14'}msbuild.exe\" /p:Configuration=Release /p:Platform=\"Any CPU\" "	
	}
	if (awscodedeploy == 'true') {
      	//copying the deploy script and appspec into package which can be tokenized and detokenized in CD
  		//bat "robocopy _build/_codedeploy_files ${PROJECT_NAME}/obj/Release/Package/PackageTmp/codedeploy_files *.* /S & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
		//bat "robocopy _build/_codedeploy_files ${PROJECT_NAME}/obj/Release/Package/PackageTmp appspec.yml /S & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"      
        dir ("_build/_codedeploy_files") {
            fileOperations([fileCopyOperation(excludes: '', flattenFiles: false, includes: "**", renameFiles: false, sourceCaptureExpression: '', targetLocation: "${WORKSPACE}/${PROJECT_NAME}/obj/Release/Package/PackageTmp/codedeploy_files", targetNameExpression: '')])
            fileOperations([fileCopyOperation(excludes: '', flattenFiles: false, includes: "appspec.yml", renameFiles: false, sourceCaptureExpression: '', targetLocation: "${WORKSPACE}/${PROJECT_NAME}/obj/Release/Package/PackageTmp", targetNameExpression: '')])
        }
  	}
	if (webdeploypackage == 'false') {
		if (packageonly == 'true') {
			//just zip up the resulting obj folder (not web deploy) and config files have been transformed
			zip archive: true, dir: "${PROJECT_NAME}/obj/Release", glob: '**', zipFile: "_drop/artefacts/${PROJECT_NAME}.tokenised.zip"
		} else {
			//just zip up the resulting packagetmp folder which gives us a normal zip (not web deploy) and config files have been transformed
			zip archive: true, dir: "${PROJECT_NAME}/obj/Release/Package/PackageTmp", glob: '**', zipFile: "_drop/artefacts/${PROJECT_NAME}.tokenised.zip"
		}
    } else {
		dir ("${PROJECT_NAME}/_PublishedWebsites/${PROJECT_NAME}_Package") {
			fileOperations([fileRenameOperation(destination: "${PROJECT_NAME}.tokenised.zip", source: "${PROJECT_NAME}.zip")])
			fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "**\\*.tokenised.zip", targetLocation: "${WORKSPACE}/_drop/artefacts")])
		}
	}
	if (fileExists(file: "${WORKSPACE}/${PROJECT_NAME}/beanstalk_files/")) {
      	dir ("${WORKSPACE}/${PROJECT_NAME}/beanstalk_files") {
  			//bat "robocopy ${WORKSPACE}/${PROJECT_NAME}/beanstalk_files ${WORKSPACE}/_drop/artefacts/beanstalk_files /MIR /XD & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
      		fileOperations([fileCopyOperation(excludes: '', flattenFiles: false, includes: "**", renameFiles: false, sourceCaptureExpression: '', targetLocation: "${WORKSPACE}/_drop/artefacts/beanstalk_files", targetNameExpression: '')])
      	}
    }
	//finally push 'drop' staging area to releasearea
	pushToDropLocation(releasearea)
}
