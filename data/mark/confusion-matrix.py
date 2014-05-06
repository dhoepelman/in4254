#!/usr/bin/env python3

from collections import defaultdict, Counter

matrix = defaultdict(list)

with open("testing2.csv", "r") as rf:
    for line in rf:
        data = line.split(",")
        matrix[data[2].rstrip()].append(data[1].rstrip())
    for i in matrix:
        c = Counter(matrix[i])
        t = sum(c.values())
        print(i, c)
        for j in c:
            print(i, j, c[j]/t)
