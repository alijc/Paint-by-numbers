//
//
// Keyarray
//
//
import java.awt.Color;

class Keyarray 
{

    // private variables
    Grid	m_grid;	// do we need to save this???
    int	m_numKeys;
    Key	m_keys[];

    // constructor
    public Keyarray( Grid grid, boolean forRow ) {
		m_grid = grid;
		if ( forRow )	m_numKeys = grid.getNumRows();
		else			m_numKeys = grid.getNumCols();

		m_keys = new Key[m_numKeys];
		for ( int i=0; i<m_numKeys; i++ ) {
			m_keys[i] = new Key(grid, forRow, i );
		}
    }

    // methods
    public int	getMaxSize() {
		int maxSize = 0;
		for ( int i=0; i<m_numKeys; i++ ) {
			maxSize = Math.max( maxSize, m_keys[i].getSize() );
		}
		return maxSize;
    }

    public int	getSize(int index)		{ return m_keys[index].getSize(); }

    public int	getNumber(int i, int j)	{ return m_keys[i].getNumber(j); }
    public int[] getNumbers(int index)	{ return m_keys[index].getNumbers(); }

    public Color getBgColor(int i, int j)	{ return m_keys[i].getBgColor(j); }
    public Color getFgColor(int i, int j)	{ return m_keys[i].getFgColor(j); }

    public void	setColors(int i)		{ m_keys[i].setColors(); }
}




class Key
{
	// private variables
	Grid	m_grid;
	boolean	m_forRow;
	int		m_index;
	int		m_numbers[];
	Color	m_bgColors[];
	Color	m_fgColors[];

	// constructor
	public Key( Grid grid, boolean forRow, int index ) {
		m_grid		= grid;
		m_forRow	= forRow;
		m_index		= index;

		// Assemble the number list for this row or column.
		int itemp[] = new int[32];	// should be more than enough
		Color ctemp[] = new Color[32];	// should be more than enough
		int tempLength = makeNumbers( false, itemp, ctemp );

		// Copy the temporary array into the permanent one
		// also, initialize the colors.
		m_numbers = new int[tempLength];
		m_bgColors = new Color[tempLength];
		m_fgColors = new Color[tempLength];
		for ( int i=0; i<tempLength; i++ ) {
			m_numbers[i] = itemp[i];
			m_fgColors[i] = ctemp[i];
			m_bgColors[i] = Color.lightGray;
		}
	}

	// methods
	public int	getSize()				{ return m_numbers.length; }
	public int	getNumber( int index )	{ return m_numbers[index]; }
	public int[] getNumbers()			{ return m_numbers; }
	public Color getBgColor( int index )	{ return m_bgColors[index]; }
	public Color getFgColor( int index )	{ return m_fgColors[index]; }

	// Check the visible colors in the grid to see which of our numbers
	// have been "solved".  
	public void setColors() {

		final Color correct = Color.white;
		final Color wrong   = Color.pink;
		final Color unset = Color.lightGray;

		// Assemble the number list for this row or column.
		int itemp[] = new int[32];	// should be more than enough
		Color ctemp[] = new Color[32];	// should be more than enough
		int tempLength = makeNumbers( true, itemp, ctemp );

		// Set all of the colors gray to start with
		for ( int i=0; i<m_numbers.length; i++ ) {
			m_bgColors[i] = unset;
		}

		// Calculate the larger of the real or imagined number of blocks.
		int maxLen = Math.max(m_numbers.length, tempLength);

		// Working from left/top to right/bottom...
		for (int i=0; i<maxLen; i++) {

			// Quit looking in this direction when we hit a block of gray
			if (ctemp[i] == unset)	break;

			// Give up if we've gone off either end.
			if ((i >= m_numbers.length) || (i >= tempLength)) {
				for ( int j=0; j<m_numbers.length; j++ )
					m_bgColors[j] = wrong;
				return;
			}

			// Color this block 'wrong' if it's the wrong color or number.
			if		(ctemp[i] != m_fgColors[i])	m_bgColors[i] = wrong;
			else if (itemp[i] != m_numbers[i])	m_bgColors[i] = wrong;

			// If we got this far, then this block is right
			else m_bgColors[i] = correct;
		}

		// Now we work from the other end...
		int i=tempLength;
		int j=m_numbers.length;
		while ( i>0 && j>0 ) {
			i--;
			j--;

			// Quit looking in this direction when we hit a block of gray
			if (ctemp[i] == unset)	break;

			// Color this block 'wrong' if it's the wrong color or number.
			if		(ctemp[i] != m_fgColors[j])	m_bgColors[j] = wrong;
			else if (itemp[i] != m_numbers[j])	m_bgColors[j] = wrong;

			// If we got this far, then this block is right
			else m_bgColors[j] = correct;
		}
	}

	// Assemble a list of numbers, either from the hidden colors,
	// or from the visible ones. 
	public int makeNumbers( boolean visible, int numbers[], Color colors[] ) {

		int numSquares = (m_forRow) ? m_grid.getNumCols():m_grid.getNumRows();
		int n = 0;
		int r = ( m_forRow ) ? m_index : 0;
		int c = ( m_forRow ) ? 0 : m_index;
		Color lastColor = Color.white;
		boolean inBlock = false;

		for ( int i=0; i<numSquares; i++ ) {
			Color thisColor = (visible) ? m_grid.getVisibleColor( r, c )
				: m_grid.getHiddenColor( r, c );

			// we're already in a block...
			if (inBlock) {

				// If this is the end of a block, get ready for the next one.
				if (thisColor == Color.white) {
					n++;
					inBlock = false; 
				}

				// If this is a continuation of a block, increment its size.
				else if (thisColor == lastColor) {
					numbers[n]++; 
				}

				// If this is the start of a new block, start counting
				else  {
					n++;
					inBlock = true;
					numbers[n] = 1;
					colors[n] = thisColor;
				}
			}

			// we're not in a block...
			else {
				// If this is another white square, do nothing.
				if (thisColor == Color.white) {}

				// If this is a new block, start counting.
				if (thisColor != lastColor)	{
					inBlock = true;
					numbers[n] = 1;
					colors[n] = thisColor;
				}

				// If this is a gray square, make sure that there's a zero here
				//else if (n==0 || numbers[n-1] != 0) numbers[n++]=0;
			}

			lastColor = thisColor;
			if ( m_forRow )	c++;
			else			r++;
		}

		// Take care of a possible final block
		if (inBlock)	n++;

		return n;
	}
}


