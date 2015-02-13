###################
# The following is general notes on installing FIMS to run on a Jetty Server
# Jetty 9.1 installation
###################
# Following instructions at:
http://www.eclipse.org/jetty/documentation/9.1.2.v20140210/startup-unix-service.html

# ALSO:
mkdir /opt/jettydev/temp/logs
chown -R jetty /opt/jettydev/temp/logs
chgrp -R jetty /opt/jettydev/temp/logs

# we can create two installations: jetty and jettydev (copy all modules over)

# Strategy following is to run as root but use setuid feature of jetty
###################
# start.ini:
###################
jetty= port 8080
jettydev  = port 8179

## SetUID Configuration
jetty.startServerAsPrivileged=false
jetty.username=jetty
jetty.groupname=jetty
jetty.umask=002

###################
# /etc/default/jetty
###################
JETTY_HOME=/opt/jetty
JETTY_BASE=/opt/web/mybase
TMPDIR=/opt/jetty/temp
JAVA_OPTIONS=-Djava.library.path=libsetuid-linux
JETTY_ARGS=--module=webapp,deploy,http,setuid

# Copying the following files to /opt/jetty/lib/setuid/ directory....
-rw-rw-r--  1 jetty jetty 914597 Jan 16 12:50 jna-4.1.0.jar
-rw-rw-r--  1 jetty jetty  17609 Jan 16 12:50 libsetuid.so
-rw-rw-r--  1 jetty jetty  10588 Jan 16 12:50 jetty-setuid.jar

###################
# /etc/sysconfig/iptables
###################
# add the following line
-A INPUT -m state --state NEW -m tcp -p tcp --dport 8179 -j ACCEPT