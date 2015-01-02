#!/bin/bash
set -e

#example useage ./start-api-local.sh -d -p 8081

while getopts ":dp:" opt
do
	case $opt in
		d) 
			debugMode=true
			echo "Option set to start API in debug mode."
			debugFlags="-Xdebug -Xnoagent  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001  -Djava.compiler=NONE" 
		;;
		p)
			apiPort=$OPTARG
			echo "Option set run API on port ${apiPort}"
		;;
		help|\?)
			echo -e "Usage: [-d]  [-p <port>]"
			echo -e "\t d - debug. Starts the API in debug mode, which an IDE can attach to on port 8001"
			echo -e "\t p <port> - Starts the API on a specific port (default 8080)"
			exit 0
		;;
	esac
done

echo 'Building RVF API webapp (skipping tests)..'
sleep 1
mvn clean install -Dapple.awt.UIElement='true' -DskipTests=true
echo

echo "Starting RVF API webapp on port ${apiPort}."
echo
configLocation="$(pwd)/config"
java -Xmx4g ${debugFlags} -DENV_NAME=$(whoami) -jar api/target/validation-api.jar -DrvfConfigLocation=${configLocation} -httpPort=${apiPort}


