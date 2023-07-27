#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call(Map map = [:]) {
    echo 'checking quality gate'
    def buildengine = "${map.buildengine ?: 'maven'}"
    echo "buildengine : ${buildengine}"
    def qgstatus
    def outData
    def outJson
    //timeout(time: 120, unit: 'SECONDS') { // Just in case something goes wrong, pipeline will be killed after a timeout
            withSonarQubeEnv('Sonar') {
                def ceTask
                timeout(time: 5, unit: 'MINUTES') {
                    waitUntil {
                    curlcmd = '''curl -k ${env.SONAR_CE_TASK_URL} -o ceTask.json'''
                    sh curlcmd
                    ceTask = readJSON file : 'ceTask.json'
                    echo ceTask.toString()
                    return "SUCCESS".equals(ceTask["task"]["status"])
                    }
                }
                def qualityGateUrl = env.SONAR_HOST_URL + "/api/qualitygates/project_status?analysisId=" + ceTask["task"]["analysisId"]
                sh '''curl -k $qualityGateUrl -o qualityGate.json'''
                def qualitygate = readJSON file : 'qualityGate.json'
                echo qualitygate.toString()
                qgstatus = qualitygate["projectStatus"]["status"]
            }
        def files = findFiles(glob: '**/leak.json')
        if (files) {
            echo 'leak.json already exists'
            def injson = readJSON file: 'leak.json'
            def currversion = injson["baseline_version"]
            echo "current baseline version: $currversion"
            
        } else {
            echo 'leak.json doesnt exists'
        }
        if ((qgstatus == 'WARN')||(qgstatus == 'SUCCESS')) {
            outData = ["baseline_version":"${PIPELINE_VERSION}"]
            outJson = groovy.json.JsonOutput.toJson(outData)
            writeFile file: 'leak.json', text: outJson, encoding: 'UTF-8'
        }
        
        if (qgstatus == 'WARN') {
            echo "**WARNING**: Quality gate status - ${qgstatus}"
            currentBuild.result = 'UNSTABLE'
        }
        if (qgstatus == 'ERROR') {
            echo "**ERROR**: Quality gate status - ${qgstatus}"
            currentBuild.result = 'FAILURE'
            error("Build failed because of quality gate error status")
        }
}
