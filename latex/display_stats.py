with open("accelerometer-vector.csv", "r") as rf:
    data = [float(i.strip()) for i in rf.readlines()]

print(max(data))
