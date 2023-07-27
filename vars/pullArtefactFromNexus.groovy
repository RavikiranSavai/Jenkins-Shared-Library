#!/usr/bin/env groovy

/**
 * Download deploy framework (token files etc) from Nexus
 *
 * by default this function will get the nexus url for downloading the artefact from the 'filecontainingurl' parameter
 * the typical use case is the deployment framework which is stored in nexus and the version linked to each cicd job execution
 * is archived in the releasearea. If a different artefact version is required (for example if some last minute token updates are
 * required) then the new version can be passed to the 'pipelineversion' parameter and then a search will be performed in 
 * nexus to find the corresponding downloadurl
 */
def call(Map map = [:]) {	
	def version = "${map.version ?: 'pipelineversion'}"	
	def filecontainingurl = "${map.filecontainingurl ?: 'nexusversion-jenkins-deployframework.txt'}"

	def downloadurl = readFile("${filecontainingurl}")
	def extension = downloadurl.substring(downloadurl.lastIndexOf("."))
	def urlchunks = downloadurl.split('/')
	def repo = urlchunks[4]
	def groupid = urlchunks[5..7].join('.')
	def artefactId = urlchunks[8]
	def searchstring
	
	//get download url for custom version
	if (version != 'pipelineversion'){
		if (downloadurl.contains('maven')){
			//need to do a search in nexus to find the correct download url
			
			if (isUnix()) {
				searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version&repository=${repo}&maven.artifactId=${artefactId}&maven.extension=${extension.substring(1)}&maven.baseVersion=${version}"
				ret = sh(script: "curl -X GET '${searchstring}'", returnStdout: true)
			} else {
				searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version^&repository=${repo}^&maven.artifactId=${artefactId}^&maven.extension=${extension.substring(1)}^&maven.baseVersion=${version}"
				ret = bat(returnStdout: true, script: "@curl -X GET ${searchstring}")
			}
			echo "return value: ${ret}"

			def searchresults = readJSON text: ret
			downloadurl = "${searchresults.items.downloadUrl[0]}"
			echo "artefact found at the following url: ${downloadurl}"
		}
	}
	//finally download
	echo "downloading from ${downloadurl}..."
	
	if (isUnix()) {
		sh "curl -O ${downloadurl}"		
	} else {
		bat(returnStdout: true, script: "@curl -O ${downloadurl}")
	}

	def artefacts = findFiles(glob: "*${extension}")
	if (artefacts){
		def artefact = "${artefacts[0].name}"
		echo "extracting artefact : ${artefact}"
		if ("${artefact}".endsWith(".jar")) {
			//extract jar
			if (isUnix()) {
				sh "jar xf ${artefact}"
			} else {
				def javaHome = tool 'JDK 1.8'
				withEnv(["Path+JAVA_HOME=$javaHome\\bin"]) {
					bat(returnStdout: true, script: "@jar xf ${artefact}")
				}
			}
		}
	} else {
		error "downloaded artefact not found"
	}
}
