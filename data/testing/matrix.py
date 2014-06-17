#!/usr/bin/env python3
from collections import defaultdict

with open("testing.csv", "r") as f:
    zero_dict = defaultdict(int)
    totals = defaultdict(zero_dict.copy)

    actual_expected = (i.rstrip().split(",")[1:] for i in f)
    for i in actual_expected:
        totals[i[1]][i[0]] += 1

    keys = sorted(i for i in totals)

    # Headers
    print("\t\t{}".format("\t".join(keys)))

    # For every expected key...
    for k in keys:
        # Compute total number of found values
        k_total = sum(totals[k].values())

        # Compute probability of each actual key
        z = ["{:.3f}".format(totals[k][l] / k_total) for l in keys]

        # Print expected key and probabilities of actual keys
        print("{}:\t{}".format(k, "\t".join(z)))
