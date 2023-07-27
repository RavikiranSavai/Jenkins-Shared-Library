#!/usr/bin/env groovy

/**
 * update 'sonar.leak.period' property for project so that quality gate check has the correct baseline to compare against
 */
def call(String baselineversion) {
    //we need to make an api call to sonarqube to set the baseline version for assessing if there have been any leaks
    echo "setting leak period to $baselineversion"
    withSonarQubeEnv('Sonar') {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'SonarQubeJenkinsAccess',
                usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            def property = 'sonar.leak.period'
          	def leakproperty= property.replace(' ','')
            def leakbaselineversion= baselineversion.replace(' ','')
            //curlcmd = "curl -X POST -u ${PASSWORD}: -d id=${property} -d value=${baselineversion} -d resource=${SONARPROJECTKEY}:${BRANCH_NAME} ${SONAR_HOST_URL}/api/properties"
            if (isUnix()) {
                sh "curl -k -X POST -u ${PASSWORD}: -d key=${property} -d value=${baselineversion} -d resource=${SONARPROJECTNAME} -d component=${baselineversion} ${SONAR_HOST_URL}/api/settings/set"
            } else {
                //curlcmd = "curl -X POST -u %PASSWORD%: -d id=" + property + " -d value=" + baselineversion + " -d resource=${SONARPROJECTKEY}:${BRANCH_NAME} " + env.SONAR_HOST_URL + "/api/properties"
                bat "curl -X POST -u ${PASSWORD}: -d key=${property} -d value=${baselineversion} -d resource=${SONARPROJECTNAME} -d component=${baselineversion} ${SONAR_HOST_URL}/api/settings/set"
            }
        }
    }
}
