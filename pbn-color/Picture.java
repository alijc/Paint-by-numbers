//
// Picture
//
// This class exists to create a picture (usually from a file) to solve.
//
import java.awt.Color;
import java.io.*;
import java.util.*;
import java.net.URL;

class Picture
{

	// Constructor
	public Picture() {
	}

	// methods
	public Color[][] create( URL base, String filename ) 
		throws UnknownFileTypeException, IOException {

		if	(filename.length() == 0)	return makeDefaultPicture();
		else {
			InputStream is = new URL(base, filename).openStream();
			if		( filename.endsWith( ".xbm" ) )	return makeXbmPicture(is);
			else if ( filename.endsWith( ".xpm" ) )	return makeXpmPicture(is);
			else
				throw new UnknownFileTypeException(filename);
		}
	}

	// Read from a file in .xbm format.
	private Color[][] makeXbmPicture( InputStream is ) throws IOException {

		BufferedInputStream in = new BufferedInputStream(is, 4000);
		Reader reader = new BufferedReader(new InputStreamReader(in));
		StreamTokenizer     st = new StreamTokenizer(reader);

		// Figure out the dimensions and allocate the color array.
		do { st.nextToken(); } while ( st.ttype != st.TT_NUMBER );
		int numCols = (int)st.nval;
		do { st.nextToken(); } while ( st.ttype != st.TT_NUMBER );
		int numRows = (int)st.nval;
		Color colors[][] = new Color[numRows][numCols];

		// Ignore any further #defines.
		st.commentChar('#');

		for ( int r=0; r<numRows; r++ ) {

			// Read enough numbers to satisfy this row.
			for ( int c=0,i=0; i<((numCols+7)/8); i++ ) {
				do {
					st.nextToken();
				} while (st.ttype != st.TT_WORD || !(st.sval).startsWith("x"));

				// Convert this hex representation into an integer.
				int x = Integer.valueOf((st.sval).substring(1),16).intValue();

				for ( int b=1, j=0; (j<8 && c<numCols); b<<=1, j++, c++ ) {
					colors[r][c] = ((x & b) > 0 ) ? Color.black : Color.white;
				}
			}
		}

		return colors;
	}

	// Read from a file in .xpm format.
	private Color[][] makeXpmPicture( InputStream is ) throws IOException {

		BufferedInputStream in = new BufferedInputStream(is, 4000);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		// Discard the first two lines of the file.
		reader.readLine();
		reader.readLine();

		// The next line contains the sizes and the number of colors
		StringTokenizer sizes = new StringTokenizer(reader.readLine()," \"");
 		int numCols = Integer.parseInt(sizes.nextToken());
 		int numRows = Integer.parseInt(sizes.nextToken());
 		int numColors = Integer.parseInt(sizes.nextToken());
 		Color colors[][] = new Color[numRows][numCols];

		// The next set of lines defines the colors used
		Hashtable ht = new Hashtable(numColors);
		for (int i=0; i<numColors; i++) {
			StringTokenizer st = new StringTokenizer(reader.readLine()," \"");
			String ch = st.nextToken();
			String unused = st.nextToken();
 			String color = st.nextToken();
 			if	(color.equals("#ffffff")) { ht.put(ch,Color.white); }
			//else ht.put(ch,Color.decode(color));
 			else if (color.equals("#000000")) { ht.put(ch,Color.black); }
 			else if (color.equals("#ff0000")) { ht.put(ch,Color.red); }
 			else if (color.equals("#00ff00")) { ht.put(ch,Color.green); }
 			else if (color.equals("#0000ff")) { ht.put(ch,Color.blue); }
			else System.out.println("unknown color: "+color);
		}

		// The rest of the file maps out the image
		for ( int r=0; r<numRows; r++ ) {
			String line = reader.readLine().substring(1,1+numCols);
 			for ( int c=0; c<numCols; c++ ) {
				colors[r][c] = (Color)ht.get(line.substring(c,c+1)); 
			}
		}

		return colors;
	}

	// Temporary code for inputting a simple test picture
	static final String defaultPicture[] = {
		"...xx...",
		"...xx...",
		"...xx.xx",
		"...xx.xx",
		"xx.xx.xx",
		"xx.xx.xx",
		"xx.xxxxx",
		"xx.xxxx.",
		"xxxxx...",
		".xxxx...",
		"...xx...",
		"..xxxx..",
	};
	private Color[][] makeDefaultPicture() {
		Color colors[][] = new Color[12][8];
		for ( int r=0; r<12; r++ ) {
			String thisRow = defaultPicture[r];
			for ( int c=0; c<8; c++ ) {
				if ( thisRow.charAt(c) == 'x' )	colors[r][c] = Color.black;
				else							colors[r][c] = Color.white;
			}
		}
		return colors;
	}

}

class UnknownFileTypeException extends Exception
{
  String m_string;

  // constructor
  public UnknownFileTypeException( String s ) {
	super(s);
	m_string = s;
  }

  public String toString() {
	return "Unknown file type: " + m_string;
  }
}
