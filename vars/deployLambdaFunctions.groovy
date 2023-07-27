#!/usr/bin/env groovy

def call() {
  	
  	dir ("deploy") {
		dir ("${TARGET_ENVIRONMENT}") {
			deleteDir()
		}
  		unstash 'stageartefact'
	}

  	withAWS(credentials:"${JENKINS_CRED_SCOPE}JenkinsServiceAWS") {
		bat label: 'Uploading the artefact to S3', script: "aws s3api put-bucket-tagging --bucket ${TARGET_SERVER} --tagging \"TagSet=[{Key=jenkins_version,Value=${PIPELINE_VERSION}}]\""
		def functionMap = readJSON file: "${workspace}\\deploy\\${TARGET_ENVIRONMENT}\\deploy-runtime\\manifests\\${APPLICATION_NAME}-${COMPONENT_NAME}_LambdaDeploy.json"
		for (int i = 0; i < functionMap.size(); i++) {
			if ("${functionMap.functionname[i]}" != "null" || "${functionMap.layername[i]}" != "null") {
				def s3release = "${RELEASE_ARTEFACT_S3FOLDER}".split('/').last()
				
				def artefactszip = "${functionMap.zip[i]}"
				def s3key = "${APPLICATION_NAME}/${COMPONENT_NAME}/${s3release}.${artefactszip}"

				files = s3FindFiles bucket: "${TARGET_SERVER}", glob: "${s3key}"
				echo "is Artefact file Found in S3?? - ${files.length}"
				if (files.length <= 0) {
					echo "Artefact file not found in S3 Location - ${TARGET_SERVER}"
					s3Upload bucket: "${TARGET_SERVER}", file: "deploy/artefacts/${artefactszip}", path: "${s3key}"
				} else {
					echo "Artefact file found in S3 Location - ${TARGET_SERVER}, so skipping the artefact upload into S3 Bucket."
				}
				if ("${functionMap.deploytype[i]}" == "Lambda-Layer") {
					bat label: 'Publishing the Lambda Layer in Version', script: "aws lambda publish-layer-version --layer-name ${functionMap.layername[i]} --description \"Layer created for the build - ${PIPELINE_VERSION}\" --content S3Bucket=${TARGET_SERVER},S3Key=${s3key}"
					sleep(time:30,unit:"SECONDS")
				} else {
					echo "Lambda Function Name - ${functionMap.functionname[i]}"
					echo "Artefact Name - ${functionMap.zip[i]}"
					echo "Updating the Latest code into the Lambda Function - ${functionMap.functionname[i]}!!!"
					bat label: 'Updating the Source Code of the Lambda Function', script: "aws lambda update-function-code --function-name ${functionMap.functionname[i]} --s3-bucket ${TARGET_SERVER} --s3-key ${s3key}"
					sleep(time:30,unit:"SECONDS")
					if("${functionMap.layers[i]}" != "null"){
						bat label: 'Updating the Lambda Function - Layer', script: "aws lambda update-function-configuration --function-name \"${functionMap.functionname[i]}\" --layers ${functionMap.layers[i]}"
						echo "Layers of the Lambda Function - ${functionMap.functionname[i]} is updated!!!"
						sleep(time:30,unit:"SECONDS")
					}
					else {
						echo "Skipped the update of the Lambda Function's layer - ${functionMap.functionname[i]}, as it is not configured!!!"
					}
					if("${functionMap.environmentvariable[i]}" != "null"){
						bat label: 'Updating the Configuration of the Lambda Function', script: "aws lambda update-function-configuration --function-name ${functionMap.functionname[i]} --handler \"${functionMap.handlers[i]}\" --environment Variables=\"${functionMap.environmentvariable[i]}\""
						echo "Configurations of the Lambda Function - ${functionMap.functionname[i]} is updated!!!"
						sleep(time:30,unit:"SECONDS")
					}
					else{
						bat label: 'Updating the Handler of the Lambda Function', script: "aws lambda update-function-configuration --function-name ${functionMap.functionname[i]} --handler \"${functionMap.handlers[i]}\""
						echo "Skipped the configurations of the Lambda Function - ${functionMap.functionname[i]}, just updated the handler alone!!!"
						sleep(time:30,unit:"SECONDS")
					}
					if("${functionMap.aliasname[i]}" != "null"){
	                	bat label: 'Publish the Lambda Version', script: "aws lambda publish-version --function-name ${functionMap.functionname[i]} >PublishVersion.json"
						bat label: 'Get the List of Alias', script: "aws lambda list-aliases --function-name ${functionMap.functionname[i]} >List4Alias.json"
						def aliasMap = readJSON file: "List4Alias.json"
						def aliasChk = 0
						for (int j = 0; j < aliasMap.Aliases.size(); j++) {
							aliasName = aliasMap.Aliases.Name[j]
							aliasNameSize = aliasMap.Aliases.Name[j].size()
							echo "Found Alias - ${aliasName}"
							if(aliasName == "${functionMap.aliasname[i]}" && aliasNameSize == "${functionMap.aliasname[i]}".size()) {
								aliasChk = 1   
							}
						}
						def versionMap = readJSON file: "PublishVersion.json"
						echo "Version to be Deployed - ${versionMap.Version}"
						if(aliasChk == 0){
							bat label: 'Create the Alias for Lambda', script: "aws lambda create-alias --function-name ${functionMap.functionname[i]} --description \"Alias - ${functionMap.aliasname[i]} created for the build - ${PIPELINE_VERSION}\" --function-version ${versionMap.Version} --name ${functionMap.aliasname[i]}"
							echo "Created a New Alias - ${functionMap.aliasname[i]} for the Lambda - ${functionMap.functionname[i]}"
						} else {
							bat label: 'Update the Alias for Lambda', script: "aws lambda update-alias --function-name ${functionMap.functionname[i]} --name ${functionMap.aliasname[i]} --function-version ${versionMap.Version} --description \"Alias - ${functionMap.aliasname[i]} updated for the build - ${PIPELINE_VERSION}\""
							echo "Updated the Alias - ${functionMap.aliasname[i]} for the Lambda - ${functionMap.functionname[i]}"
						}
					}
				}
			}
		}
	}
}
