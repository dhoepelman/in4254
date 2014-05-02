#!/bin/sh
python process_csv_vector.py
gnuplot generate_tikz_latex.gnu
