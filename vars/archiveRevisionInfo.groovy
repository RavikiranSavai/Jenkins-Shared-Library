#!/usr/bin/env groovy

def call() {
	if ("${SOURCE_PROVIDER}" == "svn") {
		
		//copy revision information to drop staging folder for later upload to releasearea
		writeFile file: "${workspace}/_drop/revision.txt", text: "$SVN_URL/$SVN_REVISION"
	} else if ("${SOURCE_PROVIDER}" == "git") {	
		
		//copy revision information to drop staging folder for later upload to releasearea
		writeFile file: "${workspace}/_drop/revision.txt", text: "GIT_COMMIT: $GIT_COMMIT GIT_BRANCH: $GIT_BRANCH"
    }
	
}
