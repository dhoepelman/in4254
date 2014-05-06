reset

set terminal png size 800,475
#set terminal tikz size 800,475

classes = "Sitting Walking Running Stairs_Down Stairs_Up
set datafile separator ","
#set key outside;
set key right top;

classes = "Sitting Walking Running Stairs_Down Stairs_Up"
Mean = 2
StdDev = 5
Corr = 8
X = 0
Y = 1
Z = 2
XY = 0
YZ = 1
ZX = 2


set output "Corr.png"
set xlabel "CorrXY"
set ylabel "CorrYZ"
set zlabel "CorrZX"
splot for [class in classes] class.".csv" using Corr+XY:Corr+YZ:Corr+ZX title class with points

set output "Means.png"
set xlabel "MeanX"
set ylabel "MeanY"
set zlabel "MeanZ"
splot for [class in classes] class.".csv" using Mean+X:Mean+Y:Mean+Z title class with points

set output "StdDev.png"
set xlabel "StdDevX"
set ylabel "StdDevY"
set zlabel "StdDevZ"
splot for [class in classes] class.".csv" using StdDev+X:StdDev+Y:StdDev+Z title class with points

set output "Means.png"
set xlabel "MeanX"
set ylabel "MeanY"
set zlabel "MeanZ"
splot for [class in classes] class.".csv" using Mean+X:Mean+Y:Mean+Z title class with points

set output "X.png"
set xlabel "MeanX"
set ylabel "StdDevX"                                                                         
plot for [class in classes] class.".csv" using Mean+X:StdDev+X title class with points  

set output "Y.png"
set xlabel "MeanY"                                                                           
set ylabel "StdDevY"                                                                     
plot for [class in classes] class.".csv" using Mean+Y:StdDev+Y title class with points  

set output "Z.png"
set xlabel "MeanZ"   
set ylabel "StdDevZ"                                                                  
plot for [class in classes] class.".csv" using Mean+Z:StdDev+Z title class with points    