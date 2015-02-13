//
// pbnxpm.C  - color nonogram solver
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// Copyright 2003 Alice J Corbin

#include <string>
#include <vector>
#include <fstream>
#include <iostream>
#include <unistd.h>
#include <values.h>


// Usage
static std::string usage="Usage: pbmxpm [options] {xpm file} [xpm file]..\n\
Where possible options are:\n\
\n\
  -p        Don't display the preliminary solution\n\
  -f        Don't display the final solution(s)\n\
  -c        Don't display the count\n\
  -t        Don't display the title\n\
  -e        Do exhaustive preliminary processing\n\
  -n number Stop after the specified number of solutions.\n";

// Command-line options
static bool prelimOpt = true;
static bool finalOpt = true;
static bool countOpt = true;
static bool titleOpt = true;
static bool exhaustiveOpt = false;
static int  maxSolutionsOpt = MAXINT;  

// Size (in all dimensions) of the puzzle being solved
static int numRows;
static int numColumns;
static int numColors;

// The list of color characters
static std::string colors;

// Character that denotes white in the puzzle 
static char  white;

// How many solutions have we found for it?
static int numSolutions;

// Convenience typedef for a vector of strings
typedef std::vector<std::string> stringVector;

// The keys for each row and column
stringVector rowKeys;
stringVector columnKeys;

// Every possible permutation of squares for each row and column
std::vector<stringVector> possibleRows;
std::vector<stringVector> possibleColumns;

// Number at which we balk at performing the fits
static const unsigned int TooBig = 5000; // 500 is OK, 8000 is a bit much

// Wildcard used to stand for 0 to many white squares
static const std::string hole(1,'*');


// Internal class to approximate a doubly-indexed array
// Each cell contains a single color.
class Image
{
	std::vector<std::string> data;
	int nRows, nCols;
	static const char unknown = '?';

public:
	Image(int r, int c) { 
		nRows = r;
		nCols = c;
		data.resize(nRows); 
	};
	//~Image();
	inline void setRow(int r, std::string row) { data[r] = row; };
	inline std::string getRow(int r) { return data[r]; };
	inline std::string getColumn(int c) {
		std::string column = "";
		for (int r=0; r<nRows; r++) {
			column += data[r][c];
		}
		return column;
	};
	inline void print() {
		for (int r=0; r<nRows; r++) {
			std::cout << data[r] << std::endl;
		}
	};
};


// Internal class to approximate a doubly-indexed array
// Each cell contains a bitset of its possible colors.
class BImage
{
	std::vector< std::vector< int > > data;
	int nRows, nCols;
	static const char unknown = '?';

public:
	BImage(int r, int c) { 
		nRows = r;
		nCols = c;
		data.resize(nRows); 
		for (int r=0; r<nRows; r++) {
			data[r].resize(nCols);
			for (int c=0; c<nCols; c++) {
				data[r][c] = 0;
			}
		}
	};
	//~BImage();

	// Convert a color character from the xpm file to a bitset bit.
	inline int  getBit(char color) {
		return (1 << colors.find(color));
	}

	// and methods
	inline void orRow(int r, std::string& row) {
		for (int c=0; c<numColumns; c++) {
			data[r][c] |= getBit(row[c]);
		}
	}

	inline void orColumn(int c, std::string& column) {
		for (int r=0; r<numRows; r++) {
			data[r][c] |= getBit(column[r]);
		}
	}

	// or method 
	inline void andImage(BImage& otherImage) {
		for (int r=0; r<numRows; r++) {
			for (int c=0; c<numColumns; c++) {
				data[r][c] &= otherImage.data[r][c];
			}
		}
	}

	// fits method
	// Report if the strings have a possible match
	inline bool fitsRow(int r, std::string& row) {
		for (int c=0; c<numColumns; c++) {
			if (!(data[r][c] & getBit(row[c]))) {
				return false;
			}
		}
		return true;
	}
	inline bool fitsColumn(int c, std::string& column) {
		for (int r=0; r<numRows; r++) {
			if (!(data[r][c] & getBit(column[r]))) {
				return false;
			}
		}
		return true;
	}

	inline void print() {
		for (int r=0; r<nRows; r++) {
			for (int c=0; c<nCols; c++) {
				switch (data[r][c]) {
				case 0x1:	std::cout << colors[0];	break;
				case 0x2:	std::cout << colors[1];	break;
				case 0x4:	std::cout << colors[2];	break;
				case 0x8:	std::cout << colors[3];	break;
				case 0x12:	std::cout << colors[4];	break;
				default:	std::cout << unknown;	break;
				}
			}
		std::cout << std::endl;
		}
	};
};


// error - print a message and quit
void error(const std::string p, const std::string p2 = "")
{
	std::cerr << p << ' ' << p2 << '\n';
	std::exit(1);
}

// possibleLines - Calculate every possible line that will satisfy this key.
void possibleLines(int length, std::string& key, stringVector& possibles)
{
	//try {
	int numHoles = std::count(key.c_str(), key.c_str()+key.length(), hole[0]);
	int numExtraWhites = length - (key.length() - numHoles);

	// If there is only one hole left to fill in, then fill it and quit
	if (numHoles == 1) {
		std::string line(key);
		std::string whiteSpace(numExtraWhites, white);
		line.replace(line.find_first_of(hole), 1, whiteSpace);
		possibles.push_back(line);
		return;
	}

	// If there are no more white spaces to distribute, then we're done.
	// Remove all remaining holes and go home.
	if (numExtraWhites == 0) {
		std::string line(key);
		std::string::size_type h;
		while (line.npos != (h = line.find_first_of(hole))) {
			line.replace(h, 1, "");
		}
		possibles.push_back(line);
		return;
	}

	// Put every possible number of white squares in the first hole,
	// then call ourselves to distribute the rest.
	for (int i=numExtraWhites; i>=0; i--) {
		std::string line(key);
		std::string whiteSpace(i, white);
		line.replace(line.find_first_of(hole), 1, whiteSpace);

		possibleLines(length, line, possibles);
	}
	// }
	// catch(...) { std::cerr << "Ouch!" << std::endl;}
}



// trim - attempt to eliminate some of the possible rows and columns
bool trim()
{
	BImage image(numRows, numColumns);

	bool changed = false;

	// Or all of the possible rows together, to find some squares
	for (int r=0; r<numRows; r++) {
		for (unsigned int i=0; i<possibleRows[r].size(); i++) {
			image.orRow(r, possibleRows[r][i]);
		}
	}

	// Create another image, doing the same thing except oring the columns
	BImage image1(numRows, numColumns);
	for (int c=0; c<numColumns; c++) {
		for (unsigned int i=0; i<possibleColumns[c].size(); i++) {
			image1.orColumn(c, possibleColumns[c][i]);
		}
	}

	// And the two images together
	image.andImage(image1);

	// Now that we know a little bit about the image, we can get rid
	// of some of the rows and columns.
	for (int r=0; r<numRows; r++) {
		if ((possibleRows[r].size() > TooBig) && !exhaustiveOpt) {
			// For some reason that I don't understand, the fits functions
			// take substantially longer than the or functions, and for large
			// numbers of possible rows this adds up to an intolerable
			// amount of time.  What I was going to do if the number was
			// too big to fit, was to throw away the old set of possibles
			// and rebuild it, checking the fit and throwing out possibles
			// recursively.  But, by gum, not doing anything in this case
			// also works.
		}
		else {
			stringVector::iterator p = possibleRows[r].begin();
			while (p != possibleRows[r].end()) {
				if (image.fitsRow(r, *p)) 	p++;
				else {
					possibleRows[r].erase(p);
					changed = true;
				}
			}
		}
	}

	for (int c=0; c<numColumns; c++) {
		if ((possibleColumns[c].size() > TooBig) && !exhaustiveOpt) {
			//std::cout << "...skipping..." << std::flush;
		}
		else {
			stringVector::iterator p = possibleColumns[c].begin();
			while (p != possibleColumns[c].end()) {
				if (image.fitsColumn(c, *p)) 	p++;
				else {
					possibleColumns[c].erase(p);
					changed = true;
				}
			}
		}
	}

	//image.print(); std::cout << std::endl;
	if (!changed && prelimOpt) {
		image.print();
	}

	return changed;
}


// Make sure that some set of possible columns match the image so far.
bool checkWeave(Image& image, int thisRow)
{

	for (int c=0; c<numColumns; c++) {
		std::string column = image.getColumn(c).substr(0,(thisRow+1));

		bool found = false;
		for (unsigned int j=0; j<possibleColumns[c].size(); j++) {
			std::string xxx = possibleColumns[c][j].substr(0,(thisRow+1));

			// If this column is satisfied, go on to the next one.
			if (column == xxx) {
				found = true;
				break;
			}
		}

		// If no match was found for this column, 
		// then the image that we're building is not a solution.
		if ( !found ) {
			return false;
		}
	}

	// If we've reached this point then this is still a possible solution
	return true;
}

// weave - recursively weave the image
// Here we're working through the permutations of possible rows,
// checking for goodness after each one.  We continue to follow
// the trails that check out, but abandon the ones that fail.
void weave(Image& image, int thisRow)
{
	// Run through all possible sequences for the given row.
	for (unsigned int i=0; i<possibleRows[thisRow].size(); i++) {
		image.setRow(thisRow, possibleRows[thisRow][i]);

		// If this row didn't bomb out...
		if (checkWeave(image, thisRow)) {

			// If we're on the last row then this is a solution.
			if (thisRow == (numRows-1)) {
				numSolutions++;

				if (finalOpt) {
					std::cout << numSolutions << std::endl;
					image.print();
				}
			}

			// Otherwise call ourselves to try the rest of the rows.
			else {
				weave(image, thisRow+1);
			}

			// Quit if we've exceeded our limit
			if (numSolutions >= maxSolutionsOpt) return;
		}
	}
}

// solve - find all possible solutions to the given keys.
void solve()
{

	// For each key, assemble a list of every possible row or column.
	possibleRows.clear();
	for (int r=0; r<numRows; r++) {
		stringVector possibles;
		possibleLines(numColumns, rowKeys[r], possibles);
		possibleRows.push_back(possibles);
	}

	possibleColumns.clear();
	for (int c=0; c<numColumns; c++) {
		stringVector possibles;
		possibleLines(numRows, columnKeys[c], possibles);
		possibleColumns.push_back(possibles);
	}

	// Verify the possible rows and columns against each other,
	// eliminating as many as we can.
	while (trim()) {
		;
	}

	// Now that we've narrowed the field a bit, 
	// use brute force to find the solution(s).
	Image image(numRows, numColumns);
	numSolutions = 0;
	weave(image, 0);

	if (countOpt)	std::cout << numSolutions << " solutions" << std::endl;
}


// createKey - Create the key for a single row or column of the puzzle.
std::string createKey(std::string imageString)
{
	std::string key = imageString;	// start with a copy of the image string
	std::string specialCase(1,'+'); // marker for 1 to many white squares
									// final marker for 1 to many white squares
	std::string specialCase1("*"+std::string(1,white)); 

	// If there are only white squares, the key should be a single white block
	if ( key.npos == key.find_first_not_of(white)) {
		return hole;		// We're done
	}


	// If there are any two dark adjacent colors, insert a 0-many white block
	// between them.
	for (int i=key.length()-1; i>0; i--) {
		if (key[i] != white  &&  key[i-1] != white  &&  key[i] != key[i-1]) {
			key.insert(i,hole);
		}
	}

	// The key must begin and end with a 0-many white block
	key.replace(0, key.find_first_not_of(white), hole);
	key.replace(key.find_last_not_of(white)+1, key.npos, hole);

	// All interior blocks of white squares must be replaced...
	std::string::size_type w;
	while (key.npos != (w = key.find_first_of(white))) {
		std::string::size_type d = key.find_first_not_of(white,w);

		// ...by either a 'normal' 0 to many white block,
		if (key[w-1] != key[d])	key.replace(w, d-w, hole);

		// ...or by a 1 to many white block.
		else key.replace(w, d-w, specialCase);
	}

	// Now that we've gotten rid of all of the white squares, replace each 
	// special-case 1 to many block with a white square and a 'normal' hole.
	while (key.npos != (w = key.find_first_of(specialCase))) {
		key.replace(w, 1, specialCase1);
	}

	return key;
}

// decodexpm: decode the picture in the xpm file
// Figure out the numbers of rows, columns and colors,
// and create the row and column keys that have to be matched.
void decodexpm(char* filename)
{
	char buffer[66];
	char unused;
	int i, r, c;

	std::ifstream in (filename);
	if (!in) error("cannot open", filename);

	// Throw away the first two lines.
	in.getline(buffer, sizeof(buffer));		// /* XPM */
	in.getline(buffer, sizeof(buffer));		// static char *abc[]={

	// Read the numbers out of the next line.
	//"16 16 4 1",
	in >> unused >> numColumns >> numRows >> numColors >> buffer;

	// Read the color designators out of the next n lines
	// "b c #000000",
	colors.resize(numColors);
	white = ' ';
	for (i=0; i<numColors; i++) {
		in >> unused >> colors[i] >> unused >> buffer;
		if (0 == strcmp(buffer,"#ffffff\","))
			white = colors[i];
	}

	// Read the image into memory
	// "#..#..aaa.......",
	Image image(numRows, numColumns);
	for (r=0, i=0; r<numRows; r++) {
		std::string row;
		in.width(numColumns);
		in >> unused >> row >> buffer;
		image.setRow(r, row);
 	}

	// Create the row and column keys for this image
	rowKeys.resize(numRows);
	for (r=0; r<numRows; r++) {
		rowKeys[r] = createKey(image.getRow(r));
	}

	columnKeys.resize(numColumns);
	for (c=0; c<numColumns; c++) {
		columnKeys[c] = createKey(image.getColumn(c));
	}

	in.close();
}


int main(int argc, char* argv[])
{
	// Read the options from the command line.
	char opt;
	while (-1 != (opt = getopt(argc, argv, "pfcten:"))) {
		switch (opt) {
		case 'p': prelimOpt = false;				break;
		case 'f': finalOpt = false;					break;
		case 'c': countOpt = false;					break;
		case 't': titleOpt = false;					break;
		case 'e': exhaustiveOpt = true;				break;
		case 'n': maxSolutionsOpt = atoi(optarg);	break;

		default: error( usage );
		}
	}

	// Read the filename(s) from the command line and decode it.
	for (int i=optind; i<argc; i++) {
		if (titleOpt) {
			std::cout << argv[i] << std::endl;
		}
		decodexpm(argv[i]);
		solve();
	}

	exit(0);
}
