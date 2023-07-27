#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call(String buildStatus = 'STARTED') {
	// build status of null means successful
  	buildStatus = buildStatus ?: 'SUCCESS'
	def subject = "${buildStatus}: Job '${PIPELINE_VERSION}'"
	def color = 'red'
	if (buildStatus == 'UNSTABLE') {
		color = 'orange'
	} else if (buildStatus == 'SUCCESS') {
		color = 'green'
	} else {
		color = 'red'
	}

	def sonardetail = ''
	if (env.SONAR_DASHBOARD_URL)
	{
		sonardetail = '<tr class='+"c3"+'><td class='+"c5"+'>SONARQUBE</td><td colspan='+"2"+' class='+"c7"+'><a href="'+SONAR_DASHBOARD_URL+'"><font color="000000">Dashboard</font></a></td></tr>'
	}
	
	def details = """<style type="text/css">
			.c1{font-size:13pt;font-family:"Arial";}
			.c2{width: 700px;margin: 0;padding: 0;table-layout: fixed;border-collapse: collapse;}
			.c3{margin: 0;padding: 0;}
			.c4{width: 700px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #1A5276;font-weight: bold;color: white;font-size:14pt;font-family:"Arial";}
			.c5{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #FFFFFF;font-weight: bold;font-size:13pt;font-family:"Arial";}
			.c6{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #FFFFFF;font-weight: bold;font-size:13pt;font-family:"Arial";}
			.c7{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: ${color};color: #000000;font-weight: bold;font-size:13pt;font-family:"Arial";}
			</style>
			<br><br><p class='c1'>Build '<b>${PIPELINE_VERSION}</b>' result was <b><font color="${color}">${buildStatus}</font></b>
				<br><br>
				<table class='c2'>
				<tbody class='c3'>
				<tr class='c3'>
				<td colspan='3' class='c4'>Quick links to the details</td>
				</tr>
				<tr class='c3'>
				<td rowspan='2' class='c5'>JENKINS</td>
				<td colspan='2' class='c6'><a href="${BUILD_URL}">Build ${env.BUILD_NUMBER} main page</a></td>
				</tr>
				<tr class='c3'>
				<td class='c5'><a href="${BUILD_URL}console">Console output</a></td>
				<td class='c5'><a href="${BUILD_URL}changes">Code commits</a></td>
				</tr>
				<tr class='c3'>
				<td class='c5'>ARTEFACTS</td>
				<td colspan='2' class='c6'><a href="${BUILD_OUTPUT}">Build Output</a></td>
				</tr>
				${sonardetail}
				</tbody></thead></table>
				
				<br><br>
				Regards, <br>
				Jenkins Admin <br>
				Any further queries please contact <b><a href="mailto:SJP_ENV_TEAM@sjpwealth.onmicrosoft.com">Env Release Management Team</a></b></p>	"""

	if (env.CUSTOM_EMAIL_LIST) {
		emailext (
			subject: subject,
			body: details,
			recipientProviders: [[$class: 'RequesterRecipientProvider'],[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider']],
			to: "${env.CUSTOM_EMAIL_LIST}"
		)
	} else {
		emailext (
			subject: subject,
			body: details,
			recipientProviders: [[$class: 'RequesterRecipientProvider'],[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider']]
		)
	}
	if (fileExists(file: "prisma-cloud-scan-results.json")) {
        prismaCloudPublish resultsFilePattern: 'prisma-cloud-scan-results.json'
    }
	if (fileExists(file: "NUnitTestReport/TestResult.xml")) {
    	nunit testResultsPattern: 'NUnitTestReport/TestResult.xml'
    }
}