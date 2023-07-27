#!/usr/bin/env groovy

/**
 * Interogate sonarqube quality gate results
 */
def call() {
    //create a staging area in the workspace specific to the target env
    dir ("deploy") {
        dir ("${LOGICAL_ENVIRONMENT_NAME}") {
            deleteDir()
            bat "robocopy /E ${BUILD_OUTPUT}/deploy-runtime deploy-runtime *.* & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
        }
        dir ("artefacts") {
            deleteDir()
            bat "robocopy /E ${BUILD_OUTPUT}/artefacts . *.* & @if %ERRORLEVEL% GTR 3 ( exit %ERRORLEVEL% ) & @set ERRORLEVEL=0"
        }
    }
    //get credentials from jenkins to pass to powershell script
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "Jenkins_Deployment_Local_Account",
    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        bat 'cd %WORKSPACE%/deploy/%LOGICAL_ENVIRONMENT_NAME%/deploy-runtime/scripts & powershell -f pipeline_deploy.ps1 -username %USERNAME% -password %PASSWORD% -deploy_scope componentlist -component_list %APPLICATION_NAME%.%COMPONENT_NAME% -environment_name %LOGICAL_ENVIRONMENT_NAME% -version %PIPELINE_VERSION% -remoteartefactlocation %BUILD_OUTPUT%/artefacts -nodeploy'
    }


}