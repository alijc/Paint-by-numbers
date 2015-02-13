#!/usr/bin/perl -w
#
# pbnxpm.pl  - color nonogram solver
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

use strict;
use vars qw($opt_t $opt_p $opt_f $opt_i $opt_c $opt_n);
use Getopt::Std;

# Usage
my $usage = "Usage: pbmxpm.pl [options] {xpm file} [xpm file]..\
Where possible options are:\
\ 
  -t        Don't display the title\
  -p        Don't display the preliminary solution\
  -f        Don't display the final solution(s)\
  -i        Don't display the number of iterations for the preliminary solution\
  -c        Don't display the count (number of solutions)\
  -n number Stop after the specified number of solutions.\n";

# Defaults for command-line options
$opt_p = 0;		# hide preliminary solution
$opt_f = 0;		# hide final solution
$opt_i = 0;		# hide iteration count
$opt_c = 0;		# hide solution count
$opt_t = 0;		# hide puzzle title
$opt_n = 99999;	# max number of solutions to find

# The list of color characters
my $colors = "";

# Character that denotes white in the puzzle 
my $white = ' ';

# This character denotes a hole (some number of white squares) in the keys
my $hole = '*';

# The keys for each row and column
my @rowKeys = ();
my @columnKeys = ();

# Every possible permutation of squares for each row and column
my @possibleRows = ();
my @possibleColumns = ();

# Size (in all dimensions) of the puzzle being solved
my $numColumns  = 0;
my $numRows = 0;
my $numColors = 0;

my $numSolutions = 0;


########################## Start of execution ###############################

# Get the options entered on the command line.
getopts("tpficn:") || die $usage;

# Process each file in the command-line arguments
my $file;
foreach $file (@ARGV) {
	print "$file\n" unless $opt_t;
	decodexpm($file);
	solve();
}

#########################################################################
# decodexpm: decode the picture in the xpm file
# Figure out the numbers of rows, columns and colors,
# and create the row and column keys that have to be matched.
sub decodexpm
{
	my ($file) = @_;

	open( IN, $file ) || die "Could not open $file for reading\n";
	my @in = <IN>;
	close IN;

	# Throw away the first two lines.
	shift @in;		# /* XPM */
	shift @in;		# static char *abc[]={

	# Read the numbers out of the next line.
	# "16 16 4 1",
	($numColumns, $numRows, $numColors) = split / /, substr(shift @in, 1); 

	# Read the color designators out of the next n lines
	# "b c #000000",
	$colors = "";
	$white = ' ';
	for (my $i=0; $i<$numColors; $i++) {
		my ($c, $unused, $value) = split / /, substr(shift @in, 1); 
		$colors .= $c;
		if ("ffffff" eq substr($value, 1, 6)) { $white = $c; }
	}

	# Read the image into memory
	# "#..#..aaa.......",
	my $image = new Image($numRows, $numColumns);
	for (my $r=0; $r<$numRows; $r++) {
		$image->setRow($r, substr(shift @in, 1, $numColumns));
	}

	# Create the row and column keys for this image
	@rowKeys = ();
	for (my $r=0; $r<$numRows; $r++) {
		push @rowKeys, createKey($image->getRow($r));
	}
	@columnKeys = ();
	for (my $c=0; $c<$numColumns; $c++) { 
		push @columnKeys, createKey($image->getColumn($c));
	}
	#print "$colors\n";	print "$white\n";	$image->print();
}

####################################################################
# createKey - Create the key for a single row or column of the puzzle.
sub createKey 
{
	# The key must begin and end with a white block
	my $key = $white . shift() . $white;

	# If there are only white squares, the key should be a single white block
	return $hole unless $key =~ /[^$white]/;

	# If there are any two dark adjacent colors, insert a 0-many white block
	# between them.
	for (my $i=length($key)-1; $i>0; $i--) {
		my $this = substr($key, $i, 1);
		my $prev = substr($key, $i-1, 1);
		if ( $this ne $white  &&  $prev ne $white  &&  $this ne $prev ) {
			substr( $key, $i, 0) = $hole;
		}
	}

	# All blocks of white squares must be turned into holes.
	$key =~ s/[$white]+/$hole/g; 

	# Now that we've gotten rid of all of the white squares, re-insert one
	# in each place where there are two adjacent same-colored blocks.
	for (my $i=length($key)-2; $i>0; $i--) {
		my $next = substr($key, $i+1, 1);
		my $this = substr($key, $i, 1);
		my $prev = substr($key, $i-1, 1);
		if ( $this eq $hole  &&  $prev eq $next ) {
			substr( $key, $i, 0) = $white;
		}
	}

	return $key;
}

##################################################################
# solve - find all possible solutions to the given keys.
sub solve
{
	my $key;

	# For each key, assemble a list of every possible row or column.
	@possibleRows = ();
	foreach $key (@rowKeys) {
		my @possibles = ();
		possibleLines($numColumns, $key, \@possibles);
		push(@possibleRows, [ @possibles ]);
	}

	@possibleColumns = ();
	foreach $key (@columnKeys) {
		my @possibles = ();
		possibleLines($numColumns, $key, \@possibles);
		push(@possibleColumns, [ @possibles ]);
	}

	# Verify the possible rows and columns against each other,
	# eliminating as many as we can.
	my $iteration = 0;
	while (trim(0)) {
		$iteration++;
	}
	print "$iteration iterations\n" unless $opt_i;

	# Now that we've narrowed the field a bit, 
	# use brute force to find the solution(s).
	my $image = new Image($numRows, $numColumns);
	$numSolutions = 0;
	weave($image, 0);

	print "$numSolutions solutions\n" unless $opt_c;
}

########################################################################
# possibleLines - Calculate every possible line that will satisfy this key.
sub possibleLines
{
	my ($length, $key, $possibles) = @_;
	my $numHoles = ($key =~ y/[*]//);  # (but y/[$hole]// doesn't work!)
	my $numExtraWhites = $length - (length($key) - $numHoles);

	# If there is only one hole left to fill in, then fill it and quit
	if ($numHoles == 1) {
		my $line = $key;
		$line =~ s/[$hole]/$white x $numExtraWhites/e;
		push(@$possibles, $line);
		#print "1 hole ---- $line\n";
	}

	# If there are no more white spaces to distribute, then we're done.
	# Remove all remaining holes and go home.
	elsif ($numExtraWhites == 0) {
		my $line = $key;
		$line =~ s/[$hole]//g;
		push(@$possibles, $line);
		#print "0 whtes --- $line\n";
	}

	# Put every possible number of white squares in the first hole,
	# then call ourselves to distribute the rest.
	else {
		for (my $i=$numExtraWhites; $i>=0; $i--) {
			my $line = $key;
			$line =~ s/[$hole]/$white x $i/e;
			possibleLines($length, $line, $possibles);
		}
	}
}

##################################################################
# trim - attempt to eliminate some of the possible rows and columns
sub trim
{
	my $image = new MImage($numRows, $numColumns);
	my $changed = 0;
	my $possibles;
	my $possible;

	# Or all of the possible rows together, to find some squares
	my $r = 0;
	foreach $possibles (@possibleRows) {
		foreach $possible (@$possibles) {
			$image->orRow($r, $possible);
		}
		$r++;
	}
	#$image->print();

	# Do the same to the columns
	my $image1 = new MImage($numRows, $numColumns);
	my $c = 0;
	foreach $possibles (@possibleColumns) {
		foreach $possible (@$possibles) {
			$image1->orColumn($c, $possible);
		}
		$c++;
	}
	#$image1->print();

	# And the images together, to get squares we know are there.
	$image->andImage($image1);
	#$image->print();

	# Now that we know a little bit about the image, we can get rid
	# of some of the rows and columns.
	$r = 0;
	foreach $possibles (@possibleRows) {
		my $oldLen = scalar @$possibles;
		@$possibles = $image->fitRows($r, $possibles);
		my $newLen = scalar @$possibles;
		$changed = 1 if ($oldLen != $newLen);
		$r++;
	}

	$c = 0;
	foreach $possibles (@possibleColumns) {
		my $oldLen = scalar @$possibles;
		@$possibles = $image->fitColumns($c, $possibles);
		my $newLen = scalar @$possibles;
		$changed = 1 if ($oldLen != $newLen);
		$c++;
	}

	if (!$changed) {
		$image->print() unless $opt_p;
	}

	return $changed;
}

################################################################
# weave - recursively weave the image
# Here we're working through the permutations of possible rows,
# checking for goodness after each one.  We continue to follow
# the trails that check out, but abandon the ones that fail.
sub weave
{
	my ($image, $thisRow) = @_;

	# Run through all possible sequences for the given row.
	my $possibles = $possibleRows[$thisRow];
	my $row;
	foreach $row (@$possibles) {
		$image->setRow($thisRow, $row);

		# If this row didn't bomb out...
		if (checkWeave($image, $thisRow)) {

			# If we're on the last row then this is a solution.
			if ($thisRow == ($numRows-1)) {
				$numSolutions++;
				$image->print()	unless $opt_f;
			}

			# Otherwise call ourselves to try the rest of the rows.
			else {
				weave($image, $thisRow+1);
			}

			# Quit if we've exceeded our limit
			return if ($numSolutions >= $opt_n);
		}
	}
}

#######################################################################
# checkWeave
# Make sure that some set of possible columns match the image so far.
sub checkWeave
{
	my ($image, $thisRow) = @_;

	for (my $c=0; $c<$numColumns; $c++) {
		my $column = substr($image->getColumn($c), 0, ($thisRow+1));
		my $found = 0;

		my $possibles = $possibleColumns[$c];
		my $col;
		foreach $col (@$possibles) {

			# If this column is satisfied, go on to the next one.
			if ($column eq substr($col, 0, ($thisRow+1))) {
				$found = 1;
				last;
			}
		}

		# If no match was found for this column, 
		# then the image that we're building is not a solution.
		return 0 if ( !$found );
	}

	# If we've reached this point then this is still a possible solution
	return 1;
}

#######################################################################
# Image
# Internal class to approximate a doubly-indexed array
# Each cell contains a single color.
package Image;

# Member data:
#
# $self->{nRows} - the number of rows in the image
# $self->{nCols} - the number of columns in the image
# $self->{data}  - the contents of the image

# The data is made up of an array of strings.
# Each string contains the color characters that make up a single row.

# new - constructor
sub new { 
	my $self = {};
	bless $self;
	my $class = shift;
	$self->{nRows} = shift;
	$self->{nCols} = shift;
	$self->{data} = [];
	for (my $r=0; $r<$self->{nRows}; $r++) {
		$self->setRow($r, '?' x $self->{nCols});
	}
	return $self;
}

# print - display the data of the image
sub print {
	my $self = shift;
	my $row;
	foreach $row (@{$self->{data}}) {
		print "$row\n";
	}
	print "\n";
}

# setRow - set a row's worth of data
sub setRow {
	my ($self, $r, $row) = @_;
#	${$self->{data}}[$r] = $row;
	splice( @{$self->{data}}, $r, 1, $row);
}

# getRow - read a row's worth of data
sub getRow {
	my ($self, $r) = @_;
	return ${$self->{data}}[$r];
}

# getColumn - read column's worth of data
sub getColumn {
	my ($self, $c) = @_;
	my $column = "";
	for (my $r=0; $r<$self->{nRows}; $r++) {
		$column .= $self->getSquare($r,$c);
	}
	return $column;
}

# getSquare - internal function to read a single square out of the data
sub getSquare {
	my ($self, $r, $c) = @_;
	return substr(@{$self->{data}}[$r], $c, 1);
#	return unpack("x$c a1", @{$self->{data}}[$r]);			# slightly slower
#	@{$self->{data}}[$r] =~ /.{$c}(.)/;		return $1;		# much slower
#	my @x = split //, @{$self->{data}}[$r];	return $x[$c];	# dog slow
}

#######################################################################
# MImage 
# Internal class to approximate a doubly-indexed array
# Each cell can contain multiple colors.
package MImage;

# Member data:
#
# $self->{nRows} - the number of rows in the image
# $self->{nCols} - the number of columns in the image
# $self->{data}  - the contents of the image

# The data is made up of a doubly-indexed array (ie, list of lists) of hashes.
# Each hash represents a single square, and holds, as its keys, the color
# characters that are valid for that square.
# (I also tried it using strings and lists to represent the squares, 
# but hashes were slightly faster.)

# new - constructor
sub new { 
	my $self = {};
	bless $self;
	my $class = shift;
	$self->{nRows} = shift;
	$self->{nCols} = shift;
	$self->{data} = [];
	for (my $r=0; $r<$self->{nRows}; $r++) {
		my @row = ();
		for (my $c=0; $c<$self->{nCols}; $c++) {
			push( @row, {} );
		}
		push @{$self->{data}}, [ @row ];
	}
	return $self;
}

# print - display the data of the image
sub print {
	my $self = shift;
	my ($row, $square);
	foreach $row (@{$self->{data}}) {
		foreach $square (@$row) {
			if ( 1 == keys %$square )	{ print keys %$square; }
			else						{ print '?'; }
			#print join("", keys %$square) . "|";
		}
		print "\n";
	}
	print "\n";
}

# orRow - add this row's colors to the image's row
sub orRow {
	my ($self, $r, $row) = @_;
	my @x = split //, $row;
	for (my $c=0; $c<$self->{nCols}; $c++) {
		${$self->{data}}[$r][$c]{ $x[$c] } = 1;
	}
}

# orColumn - add this column's colors to the image's column
sub orColumn {
	my ($self, $c, $column) = @_;
	my @x = split //, $column;
	for (my $r=0; $r<$self->{nRows}; $r++) {
		${$self->{data}}[$r][$c]{ $x[$r] } = 1;
	}
}

# andImage - create an image made up of the intersection of two images
sub andImage {
	my ($self, $other) = @_;
	for (my $r=0; $r<$self->{nRows}; $r++) {
		for (my $c=0; $c<$self->{nCols}; $c++) {
			my $char;
			foreach $char (keys %{${$self->{data}}[$r][$c]}) {
				delete ${${$self->{data}}[$r][$c]}{$char}
							 unless exists ${$other->{data}}[$r][$c]{$char};
			}
		}
	}
}

# fitRows - return the list of possible rows that fit into the current image
sub fitRows {
	my ($self, $r, $rows) = @_;
	my $template = "";
	for (my $c=0; $c<$self->{nCols}; $c++) {
		$template .= "[" . join( "", keys %{${$self->{data}}[$r][$c]}) . "]";
	}
	return grep(/$template/, @$rows);
}

# fitColumns - will this column fit into the current image?
sub fitColumns {
	my ($self, $c, $columns) = @_;
	my $template = "";
	for (my $r=0; $r<$self->{nRows}; $r++) {
		$template .= "[" . join( "", keys %{${$self->{data}}[$r][$c]}) . "]";
	}
	return grep(/$template/, @$columns);
}

