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
The directory structure for a Jenkins shared library typically follows a specific convention to make it recognizable by Jenkins. Below is the standard directory structure for a Jenkins shared library:

```
<root>
├── src/
│   ├── org/
│   │   └── jenkinsci/
│   │       └── plugins/
│   │           └── sharedlibrary/
│   │               ├── GlobalVar.groovy
│   │               └── Utils.groovy
├── vars/
│   ├── myCustomStep.groovy
│   └── myOtherStep.groovy
├── resources/
│   ├── someResourceFile.txt
│   └── templates/
│       └── myTemplate.txt
├── Jenkinsfile
├── README.md
└── vars.txt
```

Explanation of the main directories and files:

1. `src/`: This directory contains the source code of the shared library. It follows the package naming convention for Groovy code. For example, `org.jenkinsci.plugins.sharedlibrary`.

2. `src/org/jenkinsci/plugins/sharedlibrary/`: This is the package directory that contains the Groovy files defining the shared library functions or utilities. You can have multiple files in this directory, each containing different reusable functions.

3. `vars/`: This directory contains custom steps that can be directly called in Jenkins pipelines. Each Groovy file in this directory defines a single custom step.

4. `resources/`: This directory can contain any resource files that might be required by the library, such as text files, properties files, or even templates.

5. `Jenkinsfile`: This file is optional and is used for defining the Jenkins pipeline for testing the shared library itself. It's typically used for continuous integration and testing purposes.

6. `README.md`: This file is optional but recommended. It can contain documentation about the shared library, including how to use it, examples, and explanations of each function.

7. `vars.txt`: This file is optional and can contain documentation or descriptions of the custom steps defined in the `vars/` directory.


## --------------------------------------------------------------------------------------
## Simple Declarative Pipeline that demonstrates the basic structure and stages of a CI/CD pipeline:

```groovy
#!/usr/bin/env groovy
@Library('SharedPipelines') _ 		// Import the 'SharedPipelines' shared library.

pipeline {
    agent any
    environment {
        MAVEN_HOME = tool 'Maven_3.6.0-Linux'
        JDK_HOME = tool 'JDK 1.13-Linux'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage('Build') {
            steps {
                // Checkout source code from version control
                git url: 'https://github.com/example/repo.git'

                // Build the project using Maven
                sh "${MAVEN_HOME}/bin/mvn clean package"

		script {ProcessBuilder(goal:'deploy', binaryrepo:'nexus', releasearea:'s3')}
            }
        }
        stage('Test') {
            steps {
                // Run unit tests
                sh "${MAVEN_HOME}/bin/mvn test"
            }
        }
        stage('Deploy') {
            steps {
                // Deploy the application to a staging environment
                sh "scp target/my-app.jar user@staging-server:/path/to/staging"
            }
        }
        stage('Promote to Production') {
            when {
                branch 'master'
            }
            steps {
                // Promote the application to the production environment
                sh "ssh user@production-server 'cp /path/to/staging/my-app.jar /path/to/production'"
            }
        }
    }
    post {
        always {
            // Clean up workspace
            cleanWs()
        }
        success {
            echo 'Build and deployment successful!'
        }
        failure {
            echo 'Build or deployment failed!'
        }
    }
}
```
- Pipeline runs on any available agent (`agent any`).
- Defines environment variables for Maven and JDK tools (`MAVEN_HOME` and `JDK_HOME`).
- Uses the `options` block to set up log rotation, keeping the last 10 builds.
- Pipeline has four stages: Build, Test, Deploy, and Promote to Production.
- Build stage:
  - Checks out the source code from a Git repository using `git` step.
  - Builds the project using Maven with the `clean package` goal.
- Test stage:
  - Runs unit tests using Maven with the `test` goal.
- Deploy stage:
  - Deploys the application to a staging server using Secure Copy (SCP).
  - The application artifact is assumed to be generated in the `target` directory after the build.
- Promote to Production stage:
  - Only runs for the `master` branch.
  - Promotes the application from the staging server to the production server using SSH.
- `post` section:
  - `always` block cleans up the workspace after each run using `cleanWs()` step.
  - `success` block echoes a message for a successful build and deployment.
  - `failure` block echoes a message for a failed build or deployment.
- This is a basic example, and you can customize it for your specific project needs and add more stages and integrations as required.



**To run Maven using a Groovy script, you can use the `ProcessBuilder` class to execute Maven commands. 
Here's an example of a Groovy script to run Maven commands:**

```groovy

#!/usr/bin/env groovy

def mavenHome = "/path/to/maven" // Update this with the path to your Maven installation

// Define the Maven command to execute (e.g., mvn clean package)
def mavenCommand = "clean package"

// Define the working directory where Maven will be executed (e.g., the project directory)
def workingDirectory = "/path/to/your/project"

try {
    // Set up the ProcessBuilder to run the Maven command
    ProcessBuilder processBuilder = new ProcessBuilder()
    processBuilder.command("${mavenHome}/bin/mvn", mavenCommand.split())

    // Set the working directory for the process
    processBuilder.directory(new File(workingDirectory))

    // Redirect the process output to the console
    processBuilder.redirectErrorStream(true)

    // Start the process
    Process process = processBuilder.start()

    // Read and print the output of the process
    process.inputStream.eachLine { line ->
        println(line)
    }

    // Wait for the process to finish
    process.waitFor()

    // Check the exit code of the process
    int exitCode = process.exitValue()
    if (exitCode == 0) {
        println("Maven build successful!")
    } else {
        println("Maven build failed! Exit code: ${exitCode}")
    }
} catch (Exception e) {
    println("Error executing Maven: ${e.message}")
    e.printStackTrace()
    System.exit(1)
}
```

Make sure to update the `mavenHome` variable with the path to your Maven installation. Also, modify the `mavenCommand` variable to specify the Maven goals you want to run (e.g., "clean package" or any other goals). Set the `workingDirectory` variable to the path of your Maven project where the `pom.xml` is located.


## In a realtime production CI CD

```groovy
#!/usr/bin/env groovy
@Library('SharedPipelines') _ 		// Import the 'SharedPipelines' shared library.

pipeline {
	agent { label 'linux && jdk-1.10'} 		// Set the agent where the pipeline will run, in this case, a node with label 'linux && jdk-1.10'.

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
```

## How to Creat a Jenkins Shared Library involves several steps. 
A Shared Library allows you to define reusable code and integrate it with your Jenkins pipelines. 

**Here's a step-by-step guide to create a Jenkins Shared Library:**

1. **Prerequisites:**
   - You need access to a Jenkins server or Jenkins Cloud-based solution.
   - You should have a version control system (e.g., Git) to host your Shared Library code.

2. **Set up your Version Control Repository:**
   - Create a new Git repository to host your Shared Library code. This repository will contain the Jenkinsfile and any other utility Groovy scripts.

3. **Create the Shared Library Directory Structure:**
   - In your Git repository, create a directory named `vars`. This directory will hold the individual reusable steps (functions) of the Shared Library.
   - Optionally, create a `src` directory if you need to organize utility or helper functions.

4. **Define Shared Library Functions:**
   - Create Groovy scripts with your reusable functions in the `vars` directory.
   - Each file in the `vars` directory represents a single function that can be used in Jenkins pipelines.

5. **Define a Global Pipeline Library in Jenkins:**
   - Go to your Jenkins instance, navigate to **"Manage Jenkins" > "Configure System."**
   - Scroll down to the "Global Pipeline Libraries" section.
   - Click on "Add" to create a new library.
   - Provide a name for the library, specify the version, and set the retrieval method to "Modern SCM" (if using Git).
   - Enter the repository URL, credentials (if required), and any other necessary settings.

6. **Enable Library Auto-Loading (Optional):**
   - If you want Jenkins to auto-load your Shared Library, you need to configure it in the Jenkinsfile of your pipeline. Add the following line at the beginning of your Jenkinsfile:
     ```
     @Library('your-library-name') _
     ```

7. **Use Shared Library Functions in Pipelines:**
   - In your Jenkins pipeline (Jenkinsfile), you can now use the functions defined in your Shared Library. For example:
     ```groovy
     // Using the function named 'buildApp' defined in the Shared Library
     buildApp()
     ```

8. **Push Changes to the Git Repository:**
   - Commit and push your Shared Library code to the Git repository.

9. **Test the Shared Library:**
   - Create or update a Jenkins pipeline to use your Shared Library functions.
   - Run the pipeline and verify that the Shared Library functions are working as expected.

10. **Continuous Improvement:**
    - Maintain and update your Shared Library regularly based on your organization's needs.
    - Collaborate with your team to improve and add new functions to the library.

By following these steps, we can create and leverage a Jenkins Shared Library to promote **reusability**, **maintainability**, and **consistency** across your Jenkins pipelines.


