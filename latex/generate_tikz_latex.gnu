#!/usr/bin/env gnuplot
reset

set terminal tikz
set output "accelerometer-vector.tex"

set linetype 1 lc rgb '#ff8a00' lt 1 lw 1

plot "accelerometer-vector.csv" with lines
