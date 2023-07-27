#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call(String buildStatus = 'STARTED') {
	// build status of null means successful
  	buildStatus = buildStatus ?: 'SUCCESS'
	if (env.TARGET_ENVIRONMENT) {
	} else {
		env.TARGET_ENVIRONMENT = "${LOGICAL_ENVIRONMENT_NAME}"	
	}
	def subject = "${buildStatus}: Deploy Job - version '${PIPELINE_VERSION}' to '${TARGET_ENVIRONMENT}'"
	def color = 'red'
	if (buildStatus == 'UNSTABLE') {
		color = 'orange'
	} else if (buildStatus == 'SUCCESS') {
		color = 'green'
	} else {
		color = 'red'
	}

	def details = """<style type="text/css">
						.c1{font-size:13pt;font-family:"Arial";}
						.c2{width: 700px;margin: 0;padding: 0;table-layout: fixed;border-collapse: collapse;}
						.c3{margin: 0;padding: 0;}
						.c4{width: 700px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #1A5276;font-weight: bold;color: white;font-size:14pt;font-family:"Arial";}
						.c5{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #FFFFFF;font-weight: bold;font-size:13pt;font-family:"Arial";}
						.c6{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: #FFFFFF;font-weight: bold;font-size:13pt;font-family:"Arial";}
						.c7{width: 350px;margin: 0;padding: 15pt;border: 1px solid #ccc;text-align:center;background: green;font-weight: bold;font-size:13pt;font-family:"Arial";}
						</style>
						<br><br><p class='c1'>Attempt to deploy version '<b>${PIPELINE_VERSION}</b>' to '<b>${TARGET_ENVIRONMENT}</b>', result was <b><font color="${color}">${buildStatus}</font></b>
							<br><br>
							<table class='c2'>
							<tbody class='c3'>
							<tr class='c3'>
							<td colspan='3' class='c4'>Quick links to the details</td>
							</tr>
							<tr class='c3'>
							<td rowspan='2' class='c5'>JENKINS</td>
							<td colspan='2' class='c6'><a href="${JOB_URL}">${env.JOB_NAME} job main page</a></td>
							</tr>
							<tr class='c3'>
							<td class='c5'><a href="${BUILD_URL}">Build ${env.PIPELINE_VERSION} main page</a></td>
							<td class='c7'><a href="${BUILD_URL}console"><font color="000000">Console Output</font></a></td>
							</tr>
							</tbody></thead></table>
							
							<br><br>
							Regards, <br>
							Jenkins Admin <br>
							Any further queries please contact <b><a href="mailto:SJP_ENV_TEAM@sjpwealth.onmicrosoft.com">Env Release Management Team</a></b></p>"""

	if (env.CUSTOM_EMAIL_LIST) {
		emailext (
			subject: subject,
			body: details,
			recipientProviders: [[$class: 'RequesterRecipientProvider']],
			to: "${env.CUSTOM_EMAIL_LIST}"
		)
	} else {
		emailext (
			subject: subject,
			body: details,
			recipientProviders: [[$class: 'RequesterRecipientProvider']]
		)
	}
}
