# Jenkins Shared Libraries

In Jenkins, a shared library is a way to store commonly used code(reusable code), such as scripts or functions, that can be used by different 
Jenkins pipelines. 

Instead of writing the same code again and again in multiple pipelines, you can create a shared library and use it in all the pipelines
that need it. This can make your code more organized and easier to maintain. 

Think of it like a library of books, Instead of buying the same book over and over again, you can borrow it from the library whenever you need it.



## Advantages

- Standarization of Pipelines
- Reduce duplication of code
- Easy onboarding of new applications, projects or teams
- One place to fix issues with the shared or common code
- Code Maintainence 
- Reduce the risk of errors

## Directory structure
The directory structure of a Shared Library repository is as follows:

<img width="424" alt="Directory_structure" src="https://github.com/RavikiranSavai/Jenkins-Shared-Library/assets/76962621/13f3bca6-18ba-43b1-bafe-265f9e6a505d">

**vars**: This directory holds all the global shared library code that can be called from a pipeline.
It has all the library files with a .groovy extension.

**src**: It is added to the class path during very script compilation. We can add custom groovy code to extend our shared library code.

**resources**: All the non-groovy files required for your pipelines can be managed in this folder.


'''


#!/usr/bin/env groovy
@Library('SharedPipelines')_ // Import the 'SharedPipelines' shared library.

pipeline {
	agent { label 'linux && jdk-1.10'} // Set the agent where the pipeline will run, in this case, a node with label 'linux && jdk-1.10'.

	environment { // Define environment variables for use in the pipeline.
		APPLICATION_NAME = 'AccountService'
		COMPONENT_NAME = 'Adapter'
		ECR_URL = 'https://494230123456.dkr.ecr.eu-west-1.amazonaws.com/'
		ECR_REPO = 'bluedoor/accountserviceadapter'
		TOOL_MAVEN = 'Maven_3.6.0-Linux'
		TOOL_JDK = 'JDK 1.13-Linux'
	}

	tools { // Define tools to be used in the pipeline.
		maven "$TOOL_MAVEN"
		jdk "$TOOL_JDK"
	}

	options { // Define pipeline options.
		timeout(time: 1, unit: 'HOURS') // Set a timeout for the entire pipeline to 1 hour.
      	buildDiscarder(logRotator(numToKeepStr: '10')) // Configure log rotation to keep the last 10 builds.
		skipDefaultCheckout() // Skip the default SCM checkout performed by Jenkins (custom checkout implemented later).
    }

	stages { // Define the stages of the pipeline.
		stage('Prepare') { // Stage for preparing the pipeline before starting the build.
			steps {
				script{
					// Check whether the branch name is valid.
					if (!(branchNamingCheck())) {
						error ('branch does not match required naming convention')
						// TODO: You may consider emailing developers to instruct them to delete the invalid branch or delete it automatically.
                    }

					// Check if the job was started by a user or automatically triggered.
					if (checkJobTrigger()){
						echo "job was started by a user, so we assume a deployment is required"
						env.BUILD_TYPE = 'CICD' // Set environment variable BUILD_TYPE to 'CICD'.
					} else {
						echo 'job was started by an automatic trigger, so we assume deployment is not required'
						env.BUILD_TYPE = 'CI' // Set environment variable BUILD_TYPE to 'CI'.
					}

					checkoutSource(deployframework:'false') // Custom checkout implementation with deployframework parameter set to 'false'.
				}
			}
        }

		stage ('Compile') { // Stage for compiling the project.
			steps {
				script {runMavenPackage(goal:'deploy', binaryrepo:'nexus', releasearea:'s3')} // Custom script to run Maven with specified goals and parameters.
			}
		}

		stage ('Sonarqube') { // Stage for running SonarQube analysis.
			steps {
				script {runSonarQubeAnalysis()} // Custom script to run SonarQube analysis.
			}
		}

		stage ('Quality Gate') { // Stage for checking the quality gate.
			steps {
				script {checkSQQualityGate()} // Custom script to check the quality gate.
			}
		}

		stage ('Docker Build') { // Stage for building Docker image.
			steps {
				script {
					def image = docker.build("$ECR_REPO") // Build a Docker image with the specified ECR repository URL.
				}
			}
		}

		stage ('Push to ECR') { // Stage for pushing the Docker image to ECR.
			when {
				beforeAgent true
				anyOf { branch 'develop'; branch 'release/*'; branch 'hotfix/*'} // Run this stage only for specific branches.
				expression {
					return (env.BUILD_TYPE == 'CICD'); // Run this stage only if the BUILD_TYPE is 'CICD'.
				}
			}
			steps {
				script {				
					pushImageToECR() // Custom script to push the Docker image to ECR.
				}
			}
		}

		stage ('Tag') { // Stage for tagging the source code.
			when {
				beforeAgent true
				anyOf { branch 'develop'; branch 'release/*'; branch 'hotfix/*'} // Run this stage only for specific branches.
				expression {
					return (env.BUILD_TYPE == 'CICD'); // Run this stage only if the BUILD_TYPE is 'CICD'.
				}
			}
			steps {
				script {
					tagSourceCode() // Custom script to tag the source code.
				}
			}
		}
	}

	post { // Define post-build actions.
		always {
			script {
				echo 'cleaning workspace!!!'
				deleteDir() // Clean up the workspace after each run.
			}
			sendEmailNotification currentBuild.result // Send an email notification based on the build result.
		}
	}
}

'''
