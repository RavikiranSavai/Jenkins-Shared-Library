#!/usr/bin/env groovy

def call(Map map = [:]) {
	def repo = "${map.nexusrepo ?: 'maven-releases'}"
	def artefact = "${map.artifactId ?: 'jenkins-deployframework'}"
	def extension = "${map.extension ?: 'jar'}"
	def filesuffix = "${map.filesuffix ?: ''}"
	if (filesuffix == ''){
		filesuffix = artefact
	}
	def searchstring
	def ret
	if (isUnix()) {
		searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version&repository=${repo}&maven.artifactId=${artefact}&maven.extension=${extension}"
		echo searchstring
		ret = sh(script: "curl -X GET '${searchstring}'", returnStdout: true)
	} else {
		searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version^&repository=${repo}^&maven.artifactId=${artefact}^&maven.extension=${extension}"
		ret = bat(returnStdout: true, script: "@curl -X GET ${searchstring}")	
		
	}
	def searchresults = readJSON text: ret
	def url = "${searchresults.items.downloadUrl[0]}"
	echo "latest version can be downloaded using the following url: ${url}"
	
	writeFile file: "${workspace}/_drop/nexusversion-${filesuffix}.txt", text: url
	
	
}