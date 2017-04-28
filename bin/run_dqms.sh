#!/usr/bin/env bash

cd /Users/rickrees/Documents/ge/workspace2/dqms-0.1

./bin/dqms -Dpidfile.path=./dqms.pid -Dconfig.file=./dev.conf -Dhttp.port=9020 -J-Xms512M -J-Xmx1024M -J-server

# -Dhttp.port=1234 -Dhttp.address=127.0.0.1
#-Xms512M -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M