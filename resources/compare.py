#!/usr/bin/env python
import sys, string, io, os.path, subprocess, re
import json
import urllib2
from urllib import urlencode


#output.json is created by the repo jenkinsfile  which calls this python script

def sendStatus(sha,jdata):
    request = urllib2.Request('https://github-stage.td.teradata.com/api/v3/repos/tddb/dbsv2/statuses/%s' % (sha), jdata)
    request.add_header("Authorization", "Basic %s" % "amgxODYwOTk6RCFzc2Fwb2ludDNk")
    response = urllib2.urlopen(request)
    rCode =  response.getcode()
    return rCode


#Check that one of these files exists
aexists = os.path.exists('allowed')
dexists = os.path.exists('disallowed')

if aexists or dexists:
    print "Checking allowed DRs..."
    history = []
    with open("output.json") as data_file:

        #Put the PR payload into a JSON file###
        data = json.loads(data_file.read())

        #Count the total of commits in PR data####
        prList = len(data)
        i = 0

        #Open the "Allowed" list
        allowed = open("allowed")
        allowedList = allowed.read()

        #For each commit in the PR, check it against "Allowed"####
        while i < prList:
            message = data[i]['commit']['message']
            sha = data[i]['sha']


            #Pull DR out of commit message####
            drs = re.findall(r'\bDR-?\d{5,6}\b', message) #empty if no matches found
            if len(drs) == 0:
                rdata = {'state':'failure', 'description':'No DR number in this commit message.', 'context':'DR Allowed Check'}
                jdata = json.dumps(rdata)
                rCode = sendStatus(sha,jdata)
                print sha
                history.append("failure")
                print "Failure: There is no valid DR number in this commit message."
            for dr in drs:
                print message
                if len(drs) > 0:
                    allowedDR = re.findall(dr, allowedList) #empty if not on the allowed list
                    if len(allowedDR) > 0:
                        rdata = {'state':'success', 'description':'Associated DR found on Allowed list.', 'context':'DR Allowed Check'}
                        jdata = json.dumps(rdata)
                        rCode = sendStatus(sha,jdata)
                        print sha
                        history.append("success")
                        print "Success: DR number found on Allowed list."
                    else:
                        rdata = {'state':'failure', 'description':'Associated DR not on Allowed list.', 'context':'DR Allowed Check'}
                        jdata = json.dumps(rdata)
                        rCode = sendStatus(sha,jdata)
                        print sha
                        history.append("failure")
                        print "Failure: DR number is not on the Allowed list."
            i = i+1
    print "Checking for disallowed DRs..."
    with open("output.json") as data_file:
        data = json.loads(data_file.read())
        prList = len(data)
        i=0
        disallowed = open("disallowed")
        disallowedList = disallowed.read()

        while i < prList:
            message = data[i]['commit']['message']
            sha = data[i]['sha']

            #Pull DR out of commit message####
            drs = re.findall(r'\bDR-?\d{5,6}\b', message) #empty if no matches found
            if len(drs) == 0:
                rdata = {'state':'failure', 'description':'No DR number in this commit message.', 'context':'DR Allowed Check'}
                jdata = json.dumps(rdata)
                rCode = sendStatus(sha,jdata)
                print sha
                history.append("failure")
                print "Failure: There is no valid DR number in this commit message."
            for dr in drs:
                print message
                if len(drs) > 0:
                    disallowedDR = re.findall(dr, disallowedList) #empty if not on the disallowed list
                    if len(disallowedDR) > 0:
                        rdata = {'state':'failure', 'description':'Associated DR found on Disallowed list.', 'context':'DR Allowed Check'}
                        jdata = json.dumps(rdata)
                        rCode = sendStatus(sha,jdata)
                        print sha
                        history.append("failure")
                        print "Failure: DR number found on Disallowed list."
                    else:
                        rdata = {'state':'success', 'description':'Associated DR not on Disallowed list.', 'context':'DR Allowed Check'}
                        jdata = json.dumps(rdata)
                        rCode = sendStatus(sha,jdata)
                        print sha
                        history.append("success")
                        print "Success: DR number not found on Disallowed list."
            i = i+1
    #histlist = write_list(history)
    for item in history:
        if item == "failure":
            rdata = {'state':'failure', 'description':'At least one commit has an invalid DR.', 'context':'DR Allowed Check'}
            jdata = json.dumps(rdata)
            #sha = data[i]['sha']
            rCode = sendStatus(sha,jdata)
            break
        else:
            continue
else:
    sys.exit(0)

