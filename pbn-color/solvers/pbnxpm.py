#!/usr/bin/python
#
# pbnxpm.py  - color nonogram solver
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# Copyright 2003 Alice J Corbin

import sys
import getopt
import string
import re

usage = """Usage: pbmxpm.py [options] {xpm file} [xpm file]..
Where possible options are:
  -t        Don't display the title
  -p        Don't display the preliminary solution
  -f        Don't display the final solution(s)
  -i        Don't display the number of iterations for the preliminary solution
  -c        Don't display the count (number of solutions)
  -n number Stop after the specified number of solutions.
  """

# Defaults for command-line options
optHideTitle    = 0;		# hide puzzle title
optHidePrelim   = 0;		# hide preliminary solution
optHideFinal    = 0;		# hide final solution
optHideIter     = 0;		# hide iteration count
optHideCount    = 0;		# hide solution count
optNumSols      = 99999;	# max number of solutions to find

# Character that denotes white in the puzzle 
white = ' '

# This character denotes a hole (some number of white squares) in the keys
hole = '*'

# ...and this is the compiled regular expression to find a hole
holeRE = re.compile('(['+hole+'])')

# The keys for each row and column
rowKeys = [];
columnKeys = [];

# Every possible permutation of squares for each row and column
possibleRows = [];
possibleColumns = [];

# Size of the puzzle being solved
numColumns = 0
numRows = 0

numSolutions = 0



#######################################################################
# Image
# Internal class to approximate a doubly-indexed array
# Each cell contains a single color.
class  Image:

    # Member data:
    # nRows  -  the number of rows in the image
    # nCols  -  the number of columns in the image
    # data   -  the contents of the image
    # The data is made up of an array of strings.
    # Each string contains the color characters that make up a single row.

    # init
    def __init__(self, numRows, numColumns):
        self.nRows = numRows
        self.nCols = numColumns
        self.data = []
        for r in range(int(numRows)):
            self.data.append("")

    # display - display the data of the image
    def display(self):
        for row in self.data:
            print row
        print ""

    # setRow - set a row's worth of data
    def setRow(self, r, row):
        self.data[r] = row

    # getRow - read a row's worth of data
    def getRow(self, r):
        return self.data[r]

    # getColumn - read a column's worth of data
    def getColumn(self, c, length=0):
        column = ""
        if length == 0: length = self.nRows

        for r in range(int(length)):
            column = column + self.data[r][c]

        return column



#######################################################################
# MImage 
# Internal class to approximate a doubly-indexed array
# Each cell can contain multiple colors.
class MImage:

    # Member data:
    # nRows   -  the number of rows in the image
    # nCols   -  the number of columns in the image
    # data    -  the contents of the image
    # The data is made up of a doubly-indexed array (list of lists) of
    # dictionaries, each of whose keys describe the possible valid colors
    # for its square.

    def __init__(self, numRows, numColumns):
        self.nRows = numRows
        self.nCols = numColumns
        self.data = []
        for r in range(int(numRows)):
            self.data.append([])
            for c in range(int(numColumns)):
                self.data[r].append({})

    # display - display the data of the image
    def display(self):
        #print self.data
        for row in self.data:
            line = ""
            for square in row:
                if 1 == len(square):    line = line + square.keys()[0]
                else:                   line = line + '?'
            print line
        print ''

    # orRow - add this row's colors to the image's row
    def orRow(self, r, row):
        for c in range(int(self.nCols)):
            self.data[r][c][row[c]] = 1

    # orColumn - add this column's colors to the image's column
    def orColumn(self, c, column):
        for r in range(int(self.nRows)):
            self.data[r][c][column[r]] = 1

    # andImage - create an image made up of the intersection of two images
    def andImage(self, other):
        for r in range(int(self.nRows)):
            for c in range(int(self.nCols)):
                newSquare = {}
                for color in self.data[r][c]:
                    if other.data[r][c].has_key(color):
                        newSquare[color] = 1
                self.data[r][c] = newSquare

    # fitRows - find the subset of the given possible rows that actually fit
    def fitRows(self, r, rows, changed):
        template = ""
        for square in self.data[r]:
            template = template + '[' + string.join(square.keys(),'') + ']'

        return self.fitLines(template, rows, changed)

    # fitColumns - find the subset of the given possibles that actually fit
    def fitColumns(self, c, columns, changed):
        template = ""
        for r in range(int(self.nRows)):
            template = template +'['+string.join(self.data[r][c].keys(),'')+']'
        
        return self.fitLines(template, columns, changed)

    # fitLines - find the subset of the given possibles that actually fit
    # (I tried using a filter call in here, but it slowed things down a bit,
    # probably because comparing the old and new lines to figure out if they
    # had changed more than offset any time savings.)
    def fitLines(self, template, lines, changed):
        m = re.compile(template)

        newLines = []
        for line in lines:
            if m.match(line):
                newLines.append(line)
            else:
                changed = 1           
        
        return newLines, changed


####################################################################
# createKey - Create the key for a single row or column of the puzzle.
def createKey(white, line):

    # The key must begin and end with a white block
    key = white + line + white

    # If there are only white squares, the key should be a single white block
    if line.count(white) == len(line):
        return hole

    # If there are any two dark adjacent colors, insert a hole between them.
    i = len(key) - 1
    while i > 0:
        this = key[i]
        prev = key[i-1]
        if this != white and prev != white and this != prev:
            key = key[:i] + hole + key[i:]
        i = i - 1

    # All blocks of white squares must be turned into holes.
    w = re.compile('(['+white+'])+')
    key = w.sub(hole, key)

    # Now that we've gotten rid of all of the white squares, re-insert one
    # in each place where there are two adjacent same-colored blocks.
    i = len(key) - 2
    while i > 0:
        next = key[i+1]
        this = key[i]
        prev = key[i-1]
        if this == hole and prev == next:
            key = key[:i] + white + key[i:]
        i = i - 1

    return key


#########################################################################
# decodexpm: decode the picture in the xpm file
# Figure out the numbers of rows, columns and colors,
# and create the row and column keys that have to be matched.
def decodexpm(file):

    f = open(file, 'r')

    # Throw away the first two lines.
    x = f.readline()
    x = f.readline()

    # Read the numbers out of the next line.
    # "16 16 4 1",
    numbers = f.readline()[1:-3]
    [ numColumns, numRows, numColors, unused ] = numbers.split();

    # Read the color designators out of the next n lines
    # "b c #000000",
    colors = ""
    white = ' '
    for i in range(int(numColors)):
        [c, unused, value] = f.readline()[1:].split()
        colors += c
        if "ffffff" == value[1:7]: white = c

    # Create an image object to hold the image
    image = Image(numRows, numColumns)

    # Read the image into the object
    # "#..#..aaa.......",
    r = 0
    for line in f.readlines():
        image.setRow(r, line[1:1+int(numColumns)])
        r = r+1
    
    # Create the row and column keys for this image
    for r in range(int(numRows)):
        rowKeys.append(createKey(white, image.getRow(r)))
    for c in range(int(numColumns)):
        columnKeys.append(createKey(white, image.getColumn(c)))

    # Return the things we've figured out about the image
    return numColumns, numRows, white, rowKeys, columnKeys


########################################################################
# possibleLines - Calculate every possible line that will satisfy this key.
def possibleLines(length, key, possibles):

    numHoles = key.count(hole)
    numExtraWhites = int(length) - (len(key) - numHoles)

    # If there is only one hole left to fill in, then fill it and quit
    if numHoles == 1:
        line = holeRE.sub((white * numExtraWhites), key)
        possibles.append(line)
        #print "1 hole ----- "+line

    # If there are no more white spaces to distribute, then we're done.
    # Remove all remaining holes and go home.
    elif numExtraWhites == 0:
        line = holeRE.sub('', key)
        possibles.append(line)
        #print "0 whites --- "+line

    # Put every possible number of white squares in the first hole,
    # then call ourselves to distribute the rest.
    else:
        i = numExtraWhites
        while i >= 0:
            line = holeRE.sub((white * i), key, 1)
            possibles = possibleLines(length, line, possibles)
            i = i - 1

    return possibles


#######################################################################
# checkWeave
# Make sure that some set of possible columns match the image so far.
def checkWeave(image, thisRow):
    
    for c in range(int(numColumns)):
        column = image.getColumn(c, thisRow+1)
        found = 0

        possibles = possibleColumns[c];

        # If this column is satisfied, go on to the next one.
        for col in possibles:
            if col[:(thisRow+1)] == column: found = 1; continue

        # If no match was found for this column,
        # then the image that we're building is not a solution.
        if not found: return 0

    # If we've reached this point then this is still a possible solution
    return 1


################################################################
# weave - recursively weave the image
# Here we're working through the permutations of possible rows,
# checking for goodness after each one.  We continue to follow
# the trails that check out, but abandon the ones that fail.
def weave(image, thisRow, numSolutions):

    # Run through all possible sequences for the given row.
    possibles = possibleRows[thisRow]
    for row in possibles:
        image.setRow(thisRow, row)

	# If this row didn't bomb out...
        if checkWeave(image, thisRow):

            # If we're on the last row then this is a solution.
            if thisRow == (int(numRows)-1):
                numSolutions = numSolutions + 1
                if not optHideFinal: image.display()

            # Otherwise call ourselves to try the rest of the rows.
            else:
                numSolutions = weave(image, thisRow+1, numSolutions)

            # Quit if we've exceeded our limit
            if (numSolutions >= optNumSols): return

    return numSolutions


##################################################################
# trim - attempt to eliminate some of the possible rows and columns
def trim():

    changed = 0
    image = MImage(numRows, numColumns)
    #image.display()

    # Or all of the possible rows together, to find some squares
    r = 0
    for possibles in possibleRows:
        for possible in possibles:
            image.orRow(r, possible)
        r = r + 1
    #image.display()

    # Do the same to the columns
    image1 = MImage(numRows, numColumns)
    c = 0
    for possibles in possibleColumns:
        for possible in possibles:
            image1.orColumn(c, possible)
        c = c + 1
    #image1.display()

    # And the images together, to get squares we know are there.
    image.andImage(image1)
    #image.display()

    # Now that we know a little bit about the image, we can get rid
    # of some of the rows and columns.
    for r in range(len(possibleRows)):
        possibleRows[r], changed = image.fitRows(r, possibleRows[r], changed)

    for c in range(len(possibleColumns)):
        possibleColumns[c], changed = \
                            image.fitColumns(c, possibleColumns[c], changed)

    if (not changed) and (not optHidePrelim):
        image.display()

    return changed


##################################################################
# solve - find all possible solutions to the given keys.
def solve():

    # For each key, assemble a list of every possible row or column.
    del possibleRows[:]
    for key in rowKeys:
        possibles = []
        possibles = possibleLines(numColumns, key, possibles)
        possibleRows.append(possibles)

    del possibleColumns[:]
    for key in columnKeys:
        possibles = []
        possibles = possibleLines(numRows, key, possibles)
        possibleColumns.append(possibles)

    # Verify the possible rows and columns against each other,
    # eliminating as many as we can.
    iteration = 0
    while trim():
       iteration = iteration + 1

    if not optHideIter: print iteration,; print "iterations"

    # Now that we've narrowed the field a bit,
    # use brute force to find the solution(s).
    image = Image(numRows, numColumns)
    numSolutions = 0
    numSolutions = weave(image, 0, numSolutions)

    if not optHideCount: print numSolutions,; print "solutions"


########################## Start of execution ###############################

# Get the options entered on the command line.
try:    optlist, args = getopt.getopt(sys.argv[1:], "tpficn:")
except: print usage; sys.exit(1)
    
for (option, value) in optlist:
    if   option == '-t': optHideTitle    = 1
    elif option == '-p': optHidePrelim   = 1
    elif option == '-f': optHideFinal    = 1
    elif option == '-i': optHideIter     = 1
    elif option == '-c': optHideCount    = 1
    elif option == '-n': optNumSols      = value

if 0 == len(args):   print usage; sys.exit(1)

# Process each file in the command-line arguments
for file in args:

    # Print out the title of the file (unless told not to)
    if not optHideTitle: print file

    # Decode the xpm file
    numColumns, numRows, white, rowKeys, columnKeys = decodexpm(file)

    #print numColumns+' '+numRows+' '+white
    #for row in rowKeys:  print row
    #for column in columnKeys: print column

    # Find all solutions to the puzzle
    solve()

