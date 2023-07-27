#!/usr/bin/env groovy

/**
 * Diff the current running builds commit id with the previous 1 commit to get the list
 * of files changed (number can be changed by 'historydepth' parameter), then return a list
 * of subfolders that match the 'searchpath' parameter
 */
def call(Map map = [:]) {
	def searchPath = "${map.searchpath ?: 'packages/local'}"
	def historyDepth = "${map.historydepth ?: '1'}"
	def commitDiff
	if (isUnix()){
		commitDiff = sh(returnStdout: true, script: "git diff --name-only $GIT_COMMIT~${historyDepth}..$GIT_COMMIT")
	} else {
		commitDiff = bat(returnStdout: true, script: "@git diff --name-only $GIT_COMMIT~${historyDepth}..$GIT_COMMIT")
	}
	echo "last ${historyDepth} commits: \n ${commitDiff}"
	def pathList = commitDiff.split() as List 
    def resultpath = pathList.findAll {it =~ /${searchPath}/ } 
    echo "found ${resultpath.size()} commit files matching search path '${searchPath}'"
  	def folderList = []
	for (def i=0;i<resultpath.size();i++){
      	   def FOLDER_NAME = resultpath[i].split('/')
           if(!(folderList.contains(FOLDER_NAME[2]))) {
				folderList.add(FOLDER_NAME[2])
        }
    }
	return folderList
}