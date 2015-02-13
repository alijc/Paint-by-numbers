set ylabel "seconds"
set xlabel "puzzles"
set grid
plot "rb-20-times" t "old" with boxes lt 3, \
     "new-20-times" t "new" with impulses lt 4 lw 3

