//
//
// Grid
//
//
import java.awt.Color;

class Grid 
{
	// nested class to define one square of the grid
	class Square
	{
		// variables
		Color m_hiddenColor;
		Color m_visibleColor;

		// constructor
		public Square() { m_visibleColor = Color.lightGray; }

		// methods
		public void	setHiddenColor (Color newColor) { m_hiddenColor=newColor; }
		public void	setVisibleColor(Color newColor) { m_visibleColor=newColor;}
		public Color	getHiddenColor	()			{ return m_hiddenColor; }
		public Color	getVisibleColor	()			{ return m_visibleColor; }
		public boolean isSolved	() { return (m_hiddenColor==m_visibleColor); }
	}


	// private variables
	int m_numRows;
	int m_numCols;
	Square m_squares[][];

	// constructor
	public Grid( Color colors[][], boolean presolved) {
		m_numRows = colors.length;
		m_numCols = colors[0].length;
		m_squares = new Square[m_numRows][m_numCols];
		for ( int r=0; r<m_numRows; r++ ) {
			for ( int c=0; c<m_numCols; c++ ) {
				m_squares[r][c] = new Square();
				m_squares[r][c].setHiddenColor( colors[r][c] );
				if ( presolved )
					m_squares[r][c].setVisibleColor( colors[r][c] );
			}
		}
	}

	// methods
	public int getNumRows() { return m_numRows; }
	public int getNumCols() { return m_numCols; }

	public void setVisibleColor( int row, int col, Color newColor ) {
		m_squares[row][col].setVisibleColor( newColor );
	}

	public Color getVisibleColor( int row, int col ) {
		return m_squares[row][col].getVisibleColor();
	}

	public Color getHiddenColor( int row, int col ) {
		return m_squares[row][col].getHiddenColor();
	}

	public boolean isSolved() {
		// if any square is not solved then the puzzle is not solved
		for ( int r=0; r<m_numRows; r++ ) {
			for ( int c=0; c<m_numCols; c++ ) {
				if ( !m_squares[r][c].isSolved() )	return false;
			}
		}
		return true;
	}

}

