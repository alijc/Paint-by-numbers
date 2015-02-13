set ylabel "seconds"
set xlabel "puzzles"
set grid
plot "alp1-times" t "first algorithm, pre-processed" with linespoints lt 9, \
     "alp2-times" t "second algorithm, pre-processed" with boxes lt 11
