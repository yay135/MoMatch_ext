#!/bin/bash

for dfile in /dev/bus/usb/*

do
	if [ -d $dfile ]; then
		
		for file in $dfile/*
			
		do 
			if [ -c $file ]; then
				echo $file
				chmod o+w $file
				echo 'User permitted'
			fi

		done
	fi
done



