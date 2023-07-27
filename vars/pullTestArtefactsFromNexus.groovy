#!/usr/bin/env groovy

/**
 * Download test artefacts from Nexus
 *
 * by default this function will get the nexus url for downloading the test artefacts from 
 * 'nexusversion-testframework.txt' which links to the latest version at the time the cicd job
 * was executed. This 'pipelineversion' version can be overriden in which case the nexus search
 * api will be called to find the specific download url 
 */
def call(Map map = [:]) {
	//def extension = "${map.artefactextension ?: 'jar'}"	
	def version = "${map.version ?: 'pipelineversion'}"	
	//def repo = "${map.nexusrepo ?: 'maven-releases'}"	
	
	withAWS(credentials:"NonProdJenkinsServiceAWS") {
		s3Download(file:'nexusversion-testframework.txt', bucket:"${RELEASE_ARTEFACT_S3BUCKET}", path:"${RELEASE_ARTEFACT_S3FOLDER}/nexusversion-testframework.txt", force:true)
	}

	def downloadurl = readFile("nexusversion-testframework.txt")
	def extension = downloadurl.substring(downloadurl.lastIndexOf("."))
	def urlchunks = downloadurl.split('/')
	def repo = urlchunks[2]
	def groupid = urlchunks[3..5].join('.')
	def artefactId = urlchunks[6]
	def searchstring
	
	//get download url
	if (version != 'pipelineversion'){
		if (downloadurl.contains('maven')){
			//need to do a search in nexus to find the correct download url
			searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version^&repository=${repo}^&maven.artifactId=${artefactId}^&maven.extension=${extension}^&maven.baseVersion=${version}"
			ret = bat(returnStdout: true, script: "@curl -X GET ${searchstring}")
			echo "return value: ${ret}"

			def searchresults = readJSON text: ret
			downloadurl = "${searchresults.items.downloadUrl[0]}"
			echo "test artefact found at the following url: ${downloadurl}"
		}
	}
	//finally download
	bat(returnStdout: true, script: "@curl -O ${downloadurl}")

	def artefacts = findFiles(glob: "*${extension}")
	if (artefacts){
		def artefact = "${artefacts[0].name}"
		echo "artefact found: ${artefact}"
		if ("${artefact}".endsWith(".jar")) {
			//extract jar
			bat(returnStdout: true, script: "@jar xf ${artefact}")
		}
	} else {
		error "downloaded artefact not found"
	}
}