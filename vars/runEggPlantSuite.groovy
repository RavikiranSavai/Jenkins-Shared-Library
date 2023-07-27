#!/usr/bin/env groovy

/**
 * Execute an eggplant suite using eggplant functional client app
 *
  */
def call(Map map = [:]) {
	def apptestcredentialid = "${map.apptestcredentialid ?: ''}"
	def suitename = "${map.suitename ?: ''}"
	def scriptname = "${map.scriptname ?: 'Main'}"
	def appurl = "${map.appurl ?: ''}"
	def vmusercredentialid = "${map.vmusercredentialid ?: 'EggplantVMAccount'}"
	def vmname = "${map.vmname ?: 'SJP-EGGPLANT010'}"
	
	//execute eggplant suite
	withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${apptestcredentialid}",
	usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${vmusercredentialid}",
		usernameVariable: 'EPUSER', passwordVariable: 'EPPASSWORD']]) {
			def cmd = "@\"C:/Program Files/Eggplant/runscript.bat\" \"${suitename}/Scripts/${scriptname}.script\" -param \"%USERNAME%\" -param \"%PASSWORD%\" -param \"${appurl}/\" -host ${vmname} -username %EPUSER% -password %EPPASSWORD% -type RDP -port 3389 -LicenserHost 10.129.96.55 -ReportFailures yes -CommandLineOutput yes"
			echo cmd
			//execute bat but suppress terminating error when test cases fail (returnStatus:true)
			def ret = bat(returnStatus: true, returnStdout: true, script: cmd)
			echo "return value from bat: " + ret

			dir("${suitename}/Results/${scriptname}"){
				zip archive: true, dir: ".", glob: "**/*.*", zipFile: "results.zip"
			}
			if (ret != "0"){
				error "non zero return value from eggplant execution"
			}							
		}
	}
}