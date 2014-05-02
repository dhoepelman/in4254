#!/usr/bin/env python3

import math

def process_file(input_file):
    with open(input_file, "r") as rf:
        for line in rf:
            c = line.split(",")[2:]
            
            x = float(".".join(c[0:2]))
            y = float(".".join(c[2:4]))
            z = float(".".join(c[4:6]))
            
            xy = math.sqrt(x**2 + y**2)
            xyz = math.sqrt(xy**2 + z**2)
            yield "{}\n".format(xyz)

with open("accelerometer-vector.csv", "w") as wf:
    wf.writelines(process_file("accelerometer.csv"))
