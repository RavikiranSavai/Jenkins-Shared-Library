#!/usr/bin/env groovy

def call(Map map = [:]) {
	def buildtool = "${map.buildtool ?: 'maven'}"
	def deployframework = "${map.deployframework ?: 'latest'}"
	def releasearea = "${map.releasearea ?: 'smb'}"
  	def cleardeployframework = "${map.cleardeployframework ?: 'true'}"

	//always create a fresh drop staging folder
	dir ('_drop') {
		deleteDir()
	}
  	if (cleardeployframework!='false') {
  		dir ('deployframework') {
  			deleteDir()
  		}
  	}
		
	//get source code
	final scmVars = checkout scm
	if (scmVars.GIT_COMMIT) {
		env.SOURCE_PROVIDER = 'git'
		env.GIT_COMMIT = scmVars.GIT_COMMIT
		env.GIT_BRANCH = scmVars.GIT_BRANCH
		env.GIT_URL = scmVars.GIT_URL
		echo "GIT_COMMIT: $GIT_COMMIT"
		echo "GIT_BRANCH: $GIT_BRANCH"
		echo "GIT_URL: $GIT_URL"
		env.GIT_REPO_NAME = env.GIT_URL.replaceFirst(/^.*\/([^\/]+?).git$/, '$1')
		echo "GIT_REPO_NAME: $GIT_REPO_NAME"
	} else if (scmVars.SVN_REVISION) {
		env.SVN_REVISION = scmVars.SVN_REVISION
		env.SVN_URL = scmVars.SVN_URL
		echo "SVN_REVISION: $SVN_REVISION"
		echo "SVN_URL: $SVN_URL"
		
		//echo 'no git env vars detected so assume this is svn'
		env.SOURCE_PROVIDER = 'svn'
    } else {
		env.SVN_REVISION = scmVars.SVN_REVISION
		env.SVN_URL = scmVars.SVN_URL
		echo "SVN_REVISION: $SVN_REVISION"
		echo "SVN_URL: $SVN_URL"
		
		//echo 'no git env vars detected so assume this is svn'
		env.SOURCE_PROVIDER = 'svn'
    }

	// create unique jenkins version number
	generatePipelineVersion()

	archiveRevisionInfo()
	
	if (deployframework!='false') {
		getDeployFramework(deployframework)
	}
}
