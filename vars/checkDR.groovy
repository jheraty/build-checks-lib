#!/usr/bin/env groovy
//This file was created as a jenkins shared library to do the following things:
////Check for existance of a correctly formatted DR in the commit message associated with a push.
////Check for presence of that DR on one of two files: the "allowed" and "disallowed" files
////Send status check back to GitHub with "success" or "failure" based on whether the DR was present in the commit message,
////formatted correctly, listed on the "allowed" list or NOT listed on the "disallowed" list.
//It was determined that we did not want to do all of these things in this shared library so rather than lose all this cool stuff,
//I decided to create a different lib with only what i needed.
//CAUTION: There is a bug below in the script section starting on line #40. The compareResult is returned and each of the square
//bracketed operations completed but the curl script isn't running.


def call() {
    pipeline {
        agent { label 'master' }
        //options {
        //      skipDefaultCheckout true
        //}
        stages {
            stage('Get_commit_message') {
                steps {
                    script {
                        gitCommitMsg = sh(returnStdout: true, script: 'git log -1 --pretty=format:%s').trim()
                    }
                }
            }
            stage('Check_if_DR_passes') {
                when {
                    expression {
                        gitCommitMsg =~ /^DR[1-9]{5}|^DR[1-9]{6}|^DR-[1-9]{5}|^DR-[1-9]{6}/
                        print gitCommitMsg
                    }
                }
                steps {
                    //sh """curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"success", "description":"Properly formatted DR was detected.Thanks", "context":"DR Integration Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null"""
                    script {
                        compareResult = sh(returnStdout: true, script: "python compare.py").trim()
                    }
                    script {
                        sh '''
                            if [ compareResult = "This DR is allowed." ]
                            then
                                curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"success", "description":"This DR is allowed.", "context":"DR Allowed Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null
                            elif [ compareResult = "This DR is not on the allowed list." ]
                            then
                                curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"failure", "description":"This DR is not on the allowed list.", "context":"DR Allowed Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null
                            elif [ compareResult = "This DR is NOT allowed." ]
                            then
                                curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"success", "description":"This DR is allowed.", "context":"DR Allowed Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null
                            elif [ compareResult = "This DR is not on the disallowed list." ]
                            then
                                curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"failure", "description":" This DR is on the DISallowed list.", "context":"DR Allowed Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null
                            fi'''
                    }
                }
            }
            stage('if_DR_fails') {
                when {
                    expression {
                        !(gitCommitMsg =~ /^DR[1-9]{5}|^DR[1-9]{6}|^DR-[1-9]{5}|^DR-[1-9]{6}/)
                    }
                }
                steps {
                    sh """curl -H "Authorization: Basic amgxODYwOTk6MXRzYWJ1blcpcmxk" --data '{"state":"failure", "description":"No valid DR was detected in your commit message.", "context":"DR Integration Check"}' https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/""$GIT_COMMIT"" > /dev/null"""
                }
            }
        }
    }
}
