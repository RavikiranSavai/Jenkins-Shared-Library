#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call(String spec) {
	
	dir("${workspace}/testframework") {		
		deleteDir()
		def testFmkVars
		def testFmkRevNum
		if (spec.endsWith('.git')){
			testFmkVars = checkout(changelog: false, poll:false, scm:
				[
					$class: 'GitSCM', 
					branches: 
					[
						[name: 'refs/heads/master']
					], 
					doGenerateSubmoduleConfigurations: false, 
					extensions: [], 
					gitTool: 'Default', 
					submoduleCfg: [], 
					userRemoteConfigs: 
					[
						[credentialsId: 'BitbucketCheckout', url: spec]
					]
				]
			)
			testFmkRevNum = testFmkVars.GIT_COMMIT
		} else {
			urls = spec.split(',')
			for (url in urls) {
				//get folder name
				def folder = url.substring(url.lastIndexOf("/"))
				testFmkVars = checkout(changelog: false, poll:false, scm:
					[
					$class: 'SubversionSCM', 
					additionalCredentials: [], 
					excludedCommitMessages: '', 
					excludedRegions: '', 
					excludedRevprop: '', 
					excludedUsers: '', 
					filterChangelog: false, 
					ignoreDirPropChanges: true, 
					includedRegions: '', 
					locations: 
						[
						[credentialsId: 'Deployment', depthOption: 'infinity', ignoreExternalsOption: true, local: "./${folder}" , remote: url]  //,
						
						], 

					workspaceUpdater: [$class: 'UpdateUpdater']
					]
				)
			}
		}
		writeFile file: "${workspace}/_drop/revisiontestfmk.txt", text: spec + "/" + testFmkRevNum
				
	}
	//copy buildframework to drop staging folder for later upload to releasearea
	fileOperations([folderCopyOperation(destinationFolderPath: "${workspace}/_drop/test-runtime", sourceFolderPath: "${workspace}/testframework")])
	
}