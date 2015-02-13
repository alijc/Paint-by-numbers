set ylabel "seconds"
set xlabel "puzzles"
set grid
plot "alp1-times" t "pre-processed once" with linespoints lt 9, \
     "alrp1-times" t "pre-processed repeatedly" with boxes lt 12 lw 2 
     
