#!/usr/bin/env groovy

def call() {
	prismaCloudScanImage ca: '', cert: '', dockerAddress: 'unix:///var/run/docker.sock', image: "${Container_ECR_URL}/${ECR_REPO}:${ECR_TAG}", key: "${APPLICATION_NAME}-${COMPONENT_NAME}", logLevel: 'info', podmanPath: '', project: '', resultsFile: 'prisma-cloud-scan-results.json', ignoreImageBuildTime:true
}