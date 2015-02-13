#!/usr/bin/ruby -w
#
# pbnxpm.rb  - color nonogram solver
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

require 'getopts'


########################## Global data ###############################

# Usage
Usage = "Usage: pbmxpm.rb [options] {xpm file} [xpm file]..\n\
Where possible options are:\n\
\n\
  -t        Don't display the title\n\
  -p        Don't display the preliminary solution\n\
  -f        Don't display the final solution(s)\n\
  -i        Don't display the number of iterations for the preliminary solution\n\
  -c        Don't display the count (number of solutions)\n\
  -n number Stop after the specified number of solutions.\n";

# Defaults for command-line options
$OPT_p = false		# hide preliminary solution
$OPT_f = false		# hide final solution
$OPT_i = false		# hide iteration count
$OPT_c = false		# hide solution count
$OPT_t = false		# hide puzzle title
$OPT_n = 99999		# max number of solutions to find

# Character that denotes white in the puzzle 
$white = ' '

# This character denotes a hole (some number of white squares) in the keys
Hole = '*'
HoleRE = Regexp.compile( /[#{Hole}]/ )


# The keys for each row and column
$rowKeys = []
$columnKeys = []

# Every possible permutation of squares for each row and column
$possibleRows = []
$possibleColumns = []

# Size (in all dimensions) of the puzzle being solved
$numColumns  = 0
$numRows = 0
$numColors = 0

$numSolutions = 0


#######################################################################
# Image
# Internal class to approximate a doubly-indexed array
# Each cell contains a single color.

# Member data:
#
# @nRows - the number of rows in the image
# @nCols - the number of columns in the image
# @data  - the contents of the image (TODO: use a Matrix for this)

# The data is made up of an array of strings.
# Each string contains the color characters that make up a single row.

class Image

  # initialize
  def initialize(nRows, nCols)
    @nRows = nRows
    @nCols = nCols
    @data = []
  end

  # dump
  def dump
    puts @data.inspect
  end

  # print - display the data of the image
  def print()
    @data.each { |row| puts row }
    puts ""
  end
 
  # setRow - set a row's worth of data
  def setRow(r, row)
    @data[r] = row
  end

  # getRow - read a row's worth of data
  def getRow(r)
    return @data[r]
  end

  # getColumn - read column's worth of data
  def getColumn(c)
    column = ""
    @data.each do |row|
      column += row[c,1]
    end
    return column
  end

end


#######################################################################
# MImage 
# Internal class to approximate a doubly-indexed array
# Each cell can contain multiple colors.

# Member data:
#
# @nRows - the number of rows in the image
# @nCols - the number of columns in the image
# @data  - the contents of the image

# The data is made up of a doubly-indexed array of strings.
# Each hash represents a single square, and holds the color
# characters that are valid for that square.
# (I also tried it using arrays and hashes to represent the squares, 
# but strings were slightly faster.)

class MImage

  # initialize
  def initialize(nRows, nCols)
    @nRows = nRows
    @nCols = nCols
    @data = []
    (0...@nRows).each do |r|
      @data[r] = Array.new(@nCols, "")
    end
    @changed = false
  end

  # dump
  def dump()
    puts @data.inspect
  end

  # print - display the data of the image
  def print()
    @data.each do |row|
      x = ""
      row.each do |square|
	if 1 == square.length
	  x += square
	else
	  x += '?'
	end
      end
      puts x
    end
    puts ""
  end

  # accessor for the data
  def data()
    return @data
  end

  # orRow - add this row's colors to the image's row
  def orRow(r, row)
    (0...@nCols).each do |c|
      color = row[c,1]
      @data[r][c] += color unless @data[r][c].include?(color)
    end
  end

  # orColumn - add this column's colors to the image's column
  def orColumn(c, column)
    (0...@nRows).each do |r|
      color = column[r,1]
      @data[r][c] += color unless @data[r][c].include?(color)
    end
  end

  # andImage - create an image made up of the intersection of two images
  def andImage(other)
    (0...@nRows).each do |r|
      (0...@nCols).each do |c|
	# Find all the colors that we have, but the square in the 
	# other image doesn't, and delete those from ourselves.
	@data[r][c].delete!( @data[r][c].delete(other.data[r][c]) )
      end
    end
  end

  # fitRows - return the list of possible rows that fit into the current image
  def fitRows(r, rows)
    template = ''
    @data[r].each { |square| template += '[' + square + ']' }
    newRows = rows.grep(/#{template}/)
    @changed = true if ( rows.length != newRows.length )
    return newRows
  end

  # fitColumns - will this column fit into the current image?
  def fitColumns(c, columns)
    template = ''
    (0...@nRows).each { |r| template += '[' + @data[r][c] + ']' }
    newColumns = columns.grep(/#{template}/)
    @changed = true if ( columns.length != newColumns.length )
    return newColumns
  end

  def changed(val=nil)
    @changed = val if val
    @changed
  end
    
end

####################################################################
# createKey - Create the key for a single row or column of the puzzle.
def createKey(line)
  key = $white + line + $white

  # If there are only white squares, the key should be a single white block
  return Hole unless $whiteRE.match(key)
  
  # If there are any two dark adjacent colors, insert a 0-many white block
  # between them.
  (key.length-1).downto(1) do |i| 
    this = key[i,1]
    prev = key[i-1,1]
    key[i,0] = Hole if (this != $white  &&  prev != $white  &&  this != prev)
  end

  # All blocks of white squares must be turned into holes.
  key.gsub!($whiteRE, Hole)

  # Now that we've gotten rid of all of the white squares, re-insert one
  # in each place where there are two adjacent same-colored blocks.
  (key.length-2).downto(1) do |i|
    succ = key[i+1,1]
    this = key[i,1]
    prev = key[i-1,1]
    key[i,0] = $white if ( this == Hole  &&  prev == succ )
  end

  return key
end

#########################################################################
# decodexpm: decode the picture in the xpm file
# Figure out the numbers of rows, columns and colors,
# and create the row and column keys that have to be matched.
def decodexpm(file)
  begin
    lines = IO.readlines(file)

    # Throw away the first two lines.
    lines.shift		# /* XPM */
    lines.shift		# static char *abc[]={

    # Read the numbers out of the next line.
    # "16 16 4 1",
    numColumns, numRows, numColors = lines.shift[1..-1].split
    $numColumns = numColumns.to_i
    $numRows = numRows.to_i
    $numColors = numColors.to_i

    # Read the color designators out of the next n lines
    # "b c #000000",
    colors = "";
    $white = ' ';
    (1..$numColors).each do 
      c, unused, value = lines.shift[1..-4].split
      colors += c
      $white = c if 'ffffff' == value[1..-1]
    end
    $whiteRE = Regexp.compile( /[#{$white}]+/ )

    # Read the image into memory
    # "#..#..aaa.......",
    image = Image.new($numRows, $numColumns)
    (0...$numRows).each { |r| image.setRow(r, lines.shift[1..$numColumns]) }

    # Create the row and column keys for this image
    $rowKeys.clear
    (0...$numRows).each { |r| $rowKeys.push createKey(image.getRow(r)) }
    #puts $rowKeys.inspect

    $columnKeys.clear
    (0...$numColumns).each {|c| $columnKeys.push createKey(image.getColumn(c))}
    #puts $columnKeys.inspect
    #image.print

  rescue SystemCallError	
    puts "Could not open #{file} for reading"; exit
  end
end


########################################################################
# possibleLines - Calculate every possible line that will satisfy this key.
def possibleLines(length, key, possibles)

  numHoles = key.count(Hole)
  numExtraWhites = length - (key.length - numHoles)

  # If there is only one hole left to fill in, then fill it and quit
  if (numHoles == 1)
    line = key.sub(HoleRE, $white * numExtraWhites )
    possibles.push(line)
    #puts "1 hole ---- #{line}"

  # If there are no more white spaces to distribute, then we're done.
  # Remove all remaining holes and go home.
  elsif (numExtraWhites == 0)
    line = key.gsub(HoleRE, "")
    possibles.push(line)
    #puts "0 whtes --- #{line}";

  # Put every possible number of white squares in the first hole,
  # then call ourselves to distribute the rest.
  else
    numExtraWhites.downto(0) do |i|
      line = key.sub(HoleRE, $white * i )
      possibleLines(length, line, possibles);
    end
  end

end


##################################################################
# trim - attempt to eliminate some of the possible rows and columns
def trim()

  # Or all of the possible rows together, to find some squares
  r = 0
  image = MImage.new($numRows, $numColumns)
  $possibleRows.each do |possibles|
    possibles.each { |possible| image.orRow(r, possible) }
    r += 1
  end
  #image.print

  # Do the same to the columns
  c = 0
  image1 = MImage.new($numRows, $numColumns)
  $possibleColumns.each do |possibles|
    possibles.each { |possible| image1.orColumn(c, possible) }
    c += 1
  end
  #image1.print

  # And the images together, to get squares we know are there.
  image.andImage(image1)
  #image.print

  # Now that we know a little bit about the image, we can get rid
  # of some of the rows and columns.
  image.changed(false)
  r = -1
  $possibleRows = $possibleRows.collect do |row|
    r += 1
    image.fitRows(r, row)
  end

  c = -1
  $possibleColumns = $possibleColumns.collect do |column|
    c += 1
    image.fitColumns(c, column)
  end

  if (!image.changed)
    image.print() unless $OPT_p;
  end

  return image.changed
end

#######################################################################
# checkWeave
# Make sure that some set of possible columns match the image so far.
def checkWeave(image, thisRow)

  (0...$numColumns).each do |c|
    column = image.getColumn(c)[0..thisRow]
    found = false

    $possibleColumns[c].each do |col|

      # If this column is satisfied, go on to the next one.
      if (column == col[0..thisRow])
	found = true
	next
      end
    end

    # If no match was found for this column, 
    # then the image that we're building is not a solution.
    return false if !found
  end

  # If we've reached this point then this is still a possible solution
  return true

end


################################################################
# weave - recursively weave the image
# Here we're working through the permutations of possible rows,
# checking for goodness after each one.  We continue to follow
# the trails that check out, but abandon the ones that fail.
def weave(image, thisRow)

  # Run through all possible sequences for the given row.
  $possibleRows[thisRow].each do |row|
    image.setRow(thisRow, row)
    #puts "#{thisRow}  -  #{row} - "
    #image.dump

    # If this row didn't bomb out...
    if checkWeave(image, thisRow)

      # If we're on the last row then this is a solution.
      if (thisRow == ($numRows-1))
	$numSolutions += 1
	image.print()	unless $OPT_f

      # Otherwise call ourselves to try the rest of the rows.
      else 
	weave(image, thisRow+1)
      end

      # Quit if we've exceeded our limit
      return if ($numSolutions >= $OPT_n)

    end
  end
end

##################################################################
# solve - find all possible solutions to the given keys.
def solve()

  # For each key, assemble a list of every possible row or column.
  $possibleRows = $rowKeys.collect do |key|
    possibles = []
    possibleLines($numColumns, key, possibles)
    possibles
  end
  #puts $rowKeys.inspect
  #puts $possibleRows.inspect

  $possibleColumns = $columnKeys.collect do |key|
    possibles = []
    possibleLines($numRows, key, possibles)
    possibles
  end
  #puts $columnKeys.inspect
  #puts $possibleColumns.inspect

  # Verify the possible rows and columns against each other,
  # eliminating as many as we can.
  iteration = 0
  while (trim())
    iteration += 1
  end
  puts "#{iteration} iterations" unless $OPT_i

  # Now that we've narrowed the field a bit, 
  # use brute force to find the solution(s).
  image = Image.new($numRows, $numColumns)
  $numSolutions = 0
  weave(image, 0)

  puts "#{$numSolutions} solutions" unless $OPT_c;

end

########################## Start of execution ###############################

# Get the options entered on the command line.
if nil == getopts("tpficn:") || 0 ==  ARGV.length
  puts Usage;  exit
end
$OPT_n = ($OPT_n == nil) ? 99999 : $OPT_n.to_i

# Process each file in the command-line arguments
ARGV.each do |file|
  puts file unless $OPT_t
  decodexpm file
  solve
end



