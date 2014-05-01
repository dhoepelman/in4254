# This script prints the position of the robot, its believes (the probabilities), and the one-based index of the highest probability

Prr = 0.6
Pbr = 0.4

Pbb = 0.8
Prb = 0.2

# Number of color elements
n = 10

# Number of steps the robot performs
iter_steps = 7

# Start position of robot, one-based, 1 means first position
robot_senses_index = 1

x = [1/n]*n
c = ["blue", "red", "red", "blue", "blue", "blue", "red", "red", "blue", "blue"]

def sense(x, robot_senses):
    for i in range(len(x)):
        if robot_senses == "red" and c[i] == "red":
            x[i] = Prr * x[i]
        elif robot_senses == "blue" and c[i] == "red":
            x[i] = Pbr * x[i]
        elif robot_senses == "red" and c[i] == "blue":
            x[i] = Prb * x[i]
        elif robot_senses == "blue" and c[i] == "blue":
            x[i] = Pbb * x[i]

    # Normalize
    total = sum(x)
    return [v/total for v in x]

def move(x):
    y = [0.0]*(n+2)
    for i in range(len(x)):
        y[i] += x[i] * 0.1
        y[i+1] += x[i] * 0.8
        y[i+2] += x[i] * 0.1
    return y[:n]

# Print initial position and probabilities
print(robot_senses_index, "[", " ".join("{:.3f}".format(p) for p in x), "]", "n/a")

for j in range(iter_steps):
    x = move(sense(x, c[robot_senses_index-1]))
    robot_senses_index += 1

    index_max_p = x.index(max(x))+1

    # Print one-based position
    print(robot_senses_index, "[", " ".join("{:.3f}".format(p) for p in x), "]", index_max_p)
