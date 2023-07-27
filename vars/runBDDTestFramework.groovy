#!/usr/bin/env groovy

/**
 * Download automation test framework (test script files etc) from Nexus
 *
 * Typical use case is the automation test framework which is stored in nexus
 * nexus to find the corresponding downloadurl
 */
def call(Map map = [:]) {
	def isframework = "${map.isframework ?: 'true'}"
	
	if (isframework == 'true') {
		deleteDir()
		if (env.AUTOMATION_TESTFRAMEWORK_VERSION) {
			def downloadurl = "https://nexusapprepo.sjp.co.uk/repository/maven-releases/com/sjp/test/automation-testframework/${AUTOMATION_TESTFRAMEWORK_VERSION}/automation-testframework-${AUTOMATION_TESTFRAMEWORK_VERSION}.jar"
			def extension = downloadurl.substring(downloadurl.lastIndexOf("."))
			def urlchunks = downloadurl.split('/')
			def repo = urlchunks[4]
			def groupid = urlchunks[5..7].join('.')
			def artefactId = urlchunks[8]
			def searchstring

			//get download url for custom version
			if (downloadurl.contains('maven')){
				//need to do a search in nexus to find the correct download url
				if (isUnix()) {
					searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version&repository=${repo}&maven.artifactId=${artefactId}&maven.extension=${extension.substring(1)}&maven.baseVersion=${AUTOMATION_TESTFRAMEWORK_VERSION}"
					ret = sh(script: "curl -X GET '${searchstring}'", returnStdout: true)
				} else {
					searchstring = "https://nexusapprepo.sjp.co.uk/service/rest/v1/search/assets?sort=version^&repository=${repo}^&maven.artifactId=${artefactId}^&maven.extension=${extension.substring(1)}^&maven.baseVersion=${AUTOMATION_TESTFRAMEWORK_VERSION}"
					ret = bat(returnStdout: true, script: "@curl -X GET ${searchstring}")
				}
				echo "return value: ${ret}"

				def searchresults = readJSON text: ret
				downloadurl = "${searchresults.items.downloadUrl[0]}"
				echo "artefact found at the following url: ${downloadurl}"
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
	}
	
	if (isframework == 'true') {		
		dir ("${TEST_SCENARIO}") {
			withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '442af402-463c-4167-98f1-558355e679cc', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
				if (isUnix()) {
					sh "mvn -P jenkins clean test"
				} else {
					bat "mvn -P jenkins clean test"
				}
			}
		}
	} else {
		withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '442af402-463c-4167-98f1-558355e679cc', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
			if (isUnix()) {
				sh "mvn clean test"
			} else {
				bat "mvn clean test"
			}
		}
		env.TEST_SCENARIO = "${APPLICATION_NAME}-${COMPONENT_NAME}"
	}
  	junit "**/target/surefire-reports/*.xml"
	cucumber buildStatus: 'UNSTABLE',
		failedFeaturesNumber: 1,
        failedScenariosNumber: 1,
        skippedStepsNumber: 1,
        failedStepsNumber: 1,
        reportTitle: "${TEST_SCENARIO}",
        fileIncludePattern: '**/*cucumber.json',
        sortingMethod: 'ALPHABETICAL',
        trendsLimit: 100
}
