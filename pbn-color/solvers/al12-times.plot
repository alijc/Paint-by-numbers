set ylabel "seconds"
set xlabel "puzzles"
set grid
plot "al1-times" t "first algorithm" with boxes lt 3, \
     "al2-times" t "second algorithm" with impulses lt 7 lw 3
