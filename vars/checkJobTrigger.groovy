#!/usr/bin/env groovy

def call() {
	def causes = currentBuild.getBuildCauses()
	def json = env.WORKSPACE + "/causes.json"
	writeJSON(file: json, json: causes)
	def injson = readJSON file: json
	def userId = injson.userId
	//userid is an array list even if no userid found in json string
	def user = userId[0]
	return user
	
}