#!/bin/bash
set -e

#example useage ./start-api-local.sh -d -p 8081
apiPort=8081

while getopts ":dsp:" opt
do
	case $opt in
		d) 
			debugMode=true
			echo "Option set to start API in debug mode."
			debugFlags="-Xdebug -Xnoagent  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001  -Djava.compiler=NONE" 
		;;
		s) 
			skipMode=true
			echo "Option set to skip build."
		;;
		p)
			apiPort=$OPTARG
			echo "Option set run API on port ${apiPort}"
		;;
		help|\?)
			echo -e "Usage: [-d]  [-s] [-p <port>]"
			echo -e "\t d - debug. Starts the API in debug mode, which an IDE can attach to on port 8001"
			echo -e "\t p <port> - Starts the API on a specific port (default 8080)"
			echo -e "\t s - skip.  Skips the build"
			exit 0
		;;
	esac
done

if [ -z "${skipMode}" ]
then
	echo 'Building RVF API webapp..'
	sleep 1
	mvn clean install -Dapple.awt.UIElement='true' -DrvfConfigLocation=/tmp
	echo
fi

configLocation="$(pwd)/config"
echo "Starting RVF API webapp on port ${apiPort} with config directory ${configLocation}"
echo
java -Xmx4g ${debugFlags} -DENV_NAME=$(whoami)  -DrvfConfigLocation=${configLocation} -jar api/target/dependency/webapp-runner.jar api/target/api.war --path /api --port ${apiPort}

