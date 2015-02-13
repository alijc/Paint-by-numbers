set ylabel "seconds"
set xlabel "puzzles"
set grid
plot "pl-16-times" t "Perl" with boxes lt 3, \
     "py-16-times" t "Python" with impulses lt 4 lw 3, \
     "c-16-times"  t "C++" with lines lt 9

