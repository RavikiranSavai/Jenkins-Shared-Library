#!/usr/bin/env groovy

//def call(Closure body) {
def call() {
    node() {
        String path = pwd()
        String branchName = env.BRANCH_NAME
        if (branchName) {
			//echo File.separator
            String[] arrpath = path.split(java.util.regex.Pattern.quote(File.separator))
			println arrpath
            //def workspaceRoot = arrpath[0..<2].join(File.separator)
            def workspaceRoot = "H:\\WS"
			echo "workspaceRoot: $workspaceRoot"
            def currentWs = arrpath[-1]
            String newWorkspace = env.JOB_NAME.replace('/', '-')
            newWorkspace = newWorkspace.replace(File.separator, '-')
            newWorkspace = newWorkspace.replace('%2f', '-')
            newWorkspace = newWorkspace.replace('%2F', '-')
            if (currentWs =~ '@') {
                newWorkspace = "${newWorkspace}@${currentWs.split('@')[-1]}"
            }
            path = "${workspaceRoot}${File.separator}${newWorkspace}"
            //env.WORKSPACE = path
        }
        return path
        //ws(path) {
        //    body()
        //}
    }
}