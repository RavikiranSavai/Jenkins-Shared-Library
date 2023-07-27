#!/usr/bin/env groovy

def call(String scriptpath = "${workspace}\\deploy\\${TARGET_ENVIRONMENT}\\deploy-runtime\\Scripts" ) {
	def comp
	
	//default componet list if not explcitly specified
	if (env.COMPONENT_LIST) {
	} else {
		env.COMPONENT_LIST = "${APPLICATION_NAME}.${COMPONENT_NAME}"		
	}
	//we can only return a single deployment target so if a list is supplied just use the first component
	if ("$COMPONENT_LIST".contains(",")){
		comp = env.COMPONENT_LIST.split(',').first()
	} else {
		comp = env.COMPONENT_LIST
	}
	//get target server from powershell deployment framework
	def msg = powershell(returnStdout: true, script: "${scriptpath}\\getdeploytargetforcomponent.ps1 ${comp} ${TARGET_ENVIRONMENT}")
	//remove any carriage returns that always returned from powershell step
	env.TARGET_SERVER = msg.replaceAll("\r", "").replaceAll("\n", "");
	echo "target server: ${TARGET_SERVER}"	
	
}
