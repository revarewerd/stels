#!/usr/bin/python
# -*- coding: utf-8 -*-
__author__ = 'nickl'

import urllib2
import json
import datetime
import sys

arg_names = ['arg','secs']
args = dict(zip(arg_names, sys.argv))
secs = int(args.get("secs","30"))
response = urllib2.urlopen('http://localhost:9080/EDS/sessions/list')
#response = open("list.txt")
data = json.load(response)
""":type : list"""
data.sort(key=lambda e: e.get("lastAccess"), reverse=True)

def dfrt(i):
    return datetime.datetime.fromtimestamp(i / 1000)

datetime_now = datetime.datetime.now()
print "now =", datetime_now.strftime('%Y-%m-%d %H:%M:%S')

print "last %d secs =" % secs, sum((datetime_now - dfrt(e.get("lastAccess"))).total_seconds() < secs for e in data), "/",\
    len(data)

for e in data:
    print("\t".join(
        [
            e.get("login", "---").encode('utf-8'),
            dfrt(e.get("lastAccess")).strftime('%Y-%m-%d %H:%M:%S'),
            dfrt(e.get("creationTime")).strftime('%Y-%m-%d %H:%M:%S'),
            e.get("remoteAddr", "---").encode('utf-8'),
        ]
    ))






