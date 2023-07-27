#!/usr/bin/env groovy

def call(Map map = [:]) {
	def dockerfilefolder = "${map.subfolder ?: ''}"
	def tag = "${map.tag ?: ''}"
	if (dockerfilefolder == ''){
		push(tag)
	} else{
		dir(dockerfilefolder) {
			push(tag)
		}
	}	
}

def push(String extratag = '') {
	//git branches may contain forward slash which will be reflected in the pipeline version variable
	//ECR tags cannot contain forward slash, so we need to massage the version format before push
	if ("$PIPELINE_VERSION".contains("/")){
		env.ECR_TAG = "$PIPELINE_VERSION".replaceAll("/", "-")
	} else {
		env.ECR_TAG = "$PIPELINE_VERSION"	
	}
  
 	env.Container_ECR_URL = "$ECR_URL".replaceAll("https://","")
 	env.Container_ECR_URL = "$Container_ECR_URL".replaceAll("/","")

  	echo "version: $ECR_TAG"
	def image = docker.build("$ECR_REPO")
	docker.withRegistry("$ECR_URL", 'ecr:eu-west-1:DevAWSCreds') {
		image.push("$ECR_TAG")
		if (extratag){
			image.push(extratag)
		}
	}
}
