#!/usr/bin/env groovy

def call(Map map = [:]) {
	def buildtool = "${map.buildtool ?: 'maven'}"
	def profile = "${map.profile ?: 'unittests'}"
	
	echo "Build Tool Marked : ${buildtool}"
	if (buildtool == 'maven') {
		withMaven(maven: "$TOOL_MAVEN", mavenSettingsConfig: '94e8f1b0-9ffd-431c-ab7d-88a104d37857', mavenLocalRepo: '.repository', jdk: "$TOOL_JDK") {
			if (isUnix()) {
				sh "mvn -P jenkins,$profile test"
			} else {
				bat "mvn -P jenkins,$profile test"	
			}
		}
	} else if (buildtool == 'msbuild') {
	    bat "D:/Applications/opencover.4.7.1189/tools/OpenCover.Console.exe -target:D:/Applications/NUnit.org/nunit-console/nunit3-console.exe -targetargs:\"${workspace}/${TEST_PROJECT_NAME}/${TEST_PROJECT_NAME}.dll\" -register:user -output:opencover.xml"
		fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: "TestResult.xml", targetLocation: "${WORKSPACE}/NUnitTestReport")])
	}
}
