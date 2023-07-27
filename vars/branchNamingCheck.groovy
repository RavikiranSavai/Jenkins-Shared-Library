#!/usr/bin/env groovy

def call(Map map = [:]) {
	def branchmodel = "${map.branchmodel ?: 'gitflow'}"		
	def teamsuffixes = "${map.teamsuffixes ?: ''}"	
	
	def developpattern
	def subfolderwildcard
	
	if (branchmodel == 'gitflow'){
		if (teamsuffixes) {
			echo "team suffixes provided - ${teamsuffixes}"
			def list = teamsuffixes.split(',')
			list.each {
				if (developpattern) {
					developpattern = developpattern + '|' + "develop${it}"
				} else {
					developpattern = "develop${it}"
				}
				if (subfolderwildcard) {
					subfolderwildcard = subfolderwildcard + '|' + "${it}.*"
				} else {
					subfolderwildcard = "${it}.*"
				}
			}
		} else {
			echo "no team suffixes provided"
			developpattern = 'develop'
			subfolderwildcard = '.*'
		}
		echo "developpattern - ${developpattern}"
		echo "subfolderwildcard - ${subfolderwildcard}"
		
		if (env.BRANCH_NAME.matches("master|main|${developpattern}|PR-.*|(release|bugfix|hotfix|feature)\\/(${subfolderwildcard})")) {
			echo 'branch matches valid pattern'
			return true
		} else {
			echo 'stop build as doesnt match branch naming pattern'
			return false
		}
	} else {
		echo "alternate branch model specified '${branchmodel}', no naming rules have been defined here"
	}
}
