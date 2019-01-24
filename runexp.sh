#!/bin/bash

function valid_ip()
{
    local  ip=$1
    local  stat=1

    if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
        OIFS=$IFS
        IFS='.'
        ip=($ip)
        IFS=$OIFS
        [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
            && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
        stat=$?
    fi
    return $stat
}

dataRootDir='data'

echo Enter User Name:
read user

echo Enter inetAddr
read addr

while ! valid_ip $addr
do
	echo "IP NOT VALID"
	read addr
done
	

if [ -d $HOME/$dataRootDir ]; then
	echo Data output:$HOME/$dataRootDir
else 
	mkdir data
fi

list=$(find $HOME -name "MoMatch*")

d=''
r=''
for file in $list
do
	if [ -d $file ]
	then
		d=$file
		break
	fi
done

if [ "$d" = "$r" ]; then
	echo 'Project Home Not Found!'
	exit
fi


function doexp() {
	java0=$d/$1
	path0=$HOME/$dataRootDir/$2/$user

	rm -rf $path0
	if ! [ -d $HOME/$dataRootDir/$2 ]; then
		mkdir $HOME/$dataRootDir/$2
		if ! [ -d $HOME/$dataRootDir/$2/$user ]; then
			mkdir $HOME/$dataRootDir/$2/$user
		fi
	fi

	if [ -f $java0 ]; then
		echo 'Java for GE Found.'
		sleep 2
		echo 'Initializing GE ...'
		java -jar $java0 $path0 $addr
	else
		echo "$java0 Not Found!"
		exit
	fi
}

declare -a arr0=("gestureWatch/lite_server/lite_server.jar" "gestureWatch/lite_server/lite_server.jar" "rotationIoT/lite_server/lite_server.jar" "watchwalk/lite_server/lite_server.jar")

declare -a arr1=("sdata" "kdata" "rdata" "wdata")

declare -a arr2=("gesture watch3" "numpad watch3" "swtich watch2" "walk watch1")


declare -i index=0
while true
do	
	echo "Current Session: ${arr2[$index]}, Start Session?[y/n/s]"	
	read cmd

	while [ "$cmd" != "y" ] && [ "$cmd" != "n" ] && [ "$cmd" != "s" ]
	do
		echo 'Invalid Input!'
		read cmd
	done
	
	if [ $index -lt 3 ]; then 
		echo "Press c to sync time after devices connected!"
	fi
	
	if [ "$cmd" = "y" ]; then
		doexp ${arr0[$index]} ${arr1[$index]}
		index=$index+1
	elif [ "$cmd" = "n" ]; then
		exit
	elif [ "$cmd" = "s" ]; then
		index=$index+1
		if [ $index = 4 ]; then
			index=0
		fi
	fi
done





