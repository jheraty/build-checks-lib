#!/usr/bin/env groovy

def call() {
    pipeline {
        agent { label 'master' }
        stages {
            //In this stage, we need to get all the commit messages associated with the PR and put them in a list or array or file
            stage('Get_commit_messages') {
                steps {
                    echo "$env.CHANGE_ID"
                    script {
                        //def currentPRNum = env.CHANGE_ID
                        prNum = env.CHANGE_ID
                        echo prNum
                        prPayload = sh (script: 'curl -H "Authorization: Basic amgxODYwOTk6RCFzc2Fwb2ludDNk" -H "Accept: application/json" https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/pulls/"$CHANGE_ID"/commits', returnStdout: true)
                    }
                }
            }
            stage('Extract_commit_sha') {
                steps {
                    script {
                        def prCommitList = readJSON text: prPayload
                        writeJSON file: 'output.json', json: prCommitList
                        print prCommitList
                        pyRet = sh(returnStdout: true, script: "python compare.py").trim()
                        echo pyRet
                    }
                }
            }
        }
    }
}