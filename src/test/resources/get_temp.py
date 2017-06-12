#!/usr/bin/python

import os
import glob
import time

base_dir = '/Users/toby/Workspace/github/temperature-machine/src/test/resources/examples/'
device_folder = glob.glob(base_dir + '28-000005e2fdc2')[0]
device_file = device_folder + '/w1_slave'

def read_temp_raw():
    f = open(device_file, 'r')
    lines = f.readlines()
    f.close()
    return lines

def read_temp():
    lines = read_temp_raw()
    while lines[0].strip()[-3:] != 'YES':
        time.sleep(0.2)
        lines = read_temp_raw()
    equals_pos = lines[1].find('t=')
    if equals_pos != -1:
        temp_string = lines[1][equals_pos+2:]
        temp_c = float(temp_string) / 1000.0
        return temp_c

while True:
        print(read_temp())
        print"0"
        quit()
