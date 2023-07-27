#!/usr/bin/env groovy

def call(String tagdeployframework = 'false') {
	
	if (env.GIT_COMMIT) {
		//this is a git repo so lets tag the whole repo with jenkins pipeline version
		def prefix = 'https://'
		def repo = "${GIT_URL}".substring(prefix.size())

		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'BitbucketCheckout',
			usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
          		if (isUnix()) {
                      sh "git fetch https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo -f -p -t"
                      sh "git tag -a ${PIPELINE_VERSION} -m 'Jenkins'"
                      sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo --tags"
                } else {
                      bat "git fetch https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo -f --prune --prune-tags"
                      bat "git tag -a ${PIPELINE_VERSION} -m 'Jenkins'"
                      bat "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo --tags"
                }
			}
	} else {
		def revisionlines = readFile("_drop/revision.txt").split("\n")
		revisionlines.each
		{ 
			def from = it.substring(0, it.lastIndexOf("/"))
			TAGPATH = from.replaceAll("Branches", "Tags")
			def to = "${TAGPATH}/${PIPELINE_VERSION}"
			def rev = it.substring(it.lastIndexOf("/") + 1)
			withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'Deployment',
			usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
              	if (isUnix()) {
                      sh "svn copy --parents -r ${rev} \"${from}\" \"${to}\" --username ${USERNAME} --password ${PASSWORD} -m \"Maintenance: tagging from ${from}\""
                } else {
                      bat "svn copy --parents -r ${rev} \"${from}\" \"${to}\" --username ${USERNAME} --password ${PASSWORD} -m \"Maintenance: tagging from ${from}\""
                }
			}
		}
	}
	//optionally tag the deployment framework
	if (tagdeployframework == 'true') {
		def line = readFile("_drop/revisiondeployfmk.txt")
		def from = line.substring(0, line.lastIndexOf("/"))
		
      	/*	
      	def rev = line.substring(line.lastIndexOf("/") + 1)	
		TAGPATH = from.replaceAll("Branches", "Tags")
		to = "${TAGPATH}/${PIPELINE_VERSION}"
		
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'Deployment',
		usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
			bat "svn copy --parents -r ${rev} \"${from}\" \"${to}\" --username ${USERNAME} --password ${PASSWORD} -m \"Maintenance: tagging from ${from}\""
		}
        */
      	def prefix = 'https://'
		def repo = "${from}".substring(prefix.size())
      	dir ('deployframework') {
      		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'BitbucketCheckout',
				usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                if (isUnix()) {
                      sh "git fetch https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo -f -p -t"
                      sh "git tag -a ${PIPELINE_VERSION} -m 'Jenkins'"
                      sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo --tags"
                } else {
                      bat "git fetch https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo -f --prune --prune-tags"
                      bat "git tag -a ${PIPELINE_VERSION} -m 'Jenkins'"
                      bat "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@$repo --tags"
                }
			}
        }
	}
}
