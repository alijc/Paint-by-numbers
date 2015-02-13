//****************************************************************************
// PlayArea.java:	GUI for game
//****************************************************************************

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PlayArea extends JPanel
{
	// sizes
	private int	m_numGridRows,	m_numGridCols;
	private int	m_numKeyRows,	m_numKeyCols;
	private int	m_numTotalRows,	m_numTotalCols;

	// non-GUI objects
	private PBN13		m_applet;
	private Grid		m_grid;
	private Keyarray	m_rowKeys;
	private Keyarray	m_colKeys;

	private int			m_numNeededColors;
	private ColorInfo[] m_neededColors;

	// Shortcut keys
	private	char left[] = {'a','s','d','f','g'};
	private	char right[] = {';','l','k','j','h'};

	// GUI objects
	private JLabel		m_status;
	private ColorPanel	m_colorPanel;
	private PlayGrid	m_playGrid;
	private Color		m_currentColor;

	private final Color	m_background = Color.white;

	// Constructor
	public PlayArea(PBN13 applet)
	{
		// Get the stuff we need from the applet
		m_applet  = applet;
		m_grid    = applet.grid();
		m_rowKeys = applet.rowKeys();
		m_colKeys = applet.colKeys();
		m_numNeededColors = applet.numNeededColors();
		m_neededColors = applet.neededColors();

		// Fill in the permanent sizes
		m_numGridRows = m_grid.getNumRows();
		m_numGridCols = m_grid.getNumCols();
		m_numKeyRows  = m_colKeys.getMaxSize();
		m_numKeyCols  = m_rowKeys.getMaxSize();

		m_numTotalRows = m_numGridRows + m_numKeyRows;
		m_numTotalCols = m_numGridCols + m_numKeyCols;

		// Make the objects we need for the GUI
		String hint = new String("Shortcuts:");
		for (int i=0; i<m_numNeededColors; i++) {
			hint += "  "+m_neededColors[i].name()+": "+left[i]+" or "+right[i];
		}
		m_status = new JLabel(hint);

		m_colorPanel = new ColorPanel();
		m_playGrid = new PlayGrid();

		// ...and lay them out.
		setBackground( m_background );
		setLayout( new BorderLayout() );
		add( m_status, BorderLayout.NORTH);
		add( m_colorPanel, BorderLayout.WEST);
		add( m_playGrid, BorderLayout.CENTER );

		// Start out using white
		m_colorPanel.cButton[1].doClick();

		//m_colorPanel.bBlack.requestFocusInWindow(); works in 1.4
		//m_colorPanel.bBlack.requestFocus();
	}

	// accessors
	public void showStatus(String msg) { m_status.setText(msg); }
	public String filename() { return m_applet.filename(); }

	public Color getColor() { return m_currentColor; }
	public void setColor(Color newColor) { m_currentColor = newColor; }


	//=========================================
	// Internal class for the Key listener
	//=========================================
	class MyKeyListener implements KeyListener { 
		public void keyTyped(KeyEvent e) {

 			char c = e.getKeyChar();
			for (int i=0; i<m_numNeededColors; i++) {
				if ( c == left[i]  ||  c == right[i] ) {
					m_colorPanel.cButton[i].doClick();
				}
			}
		} 
		public void keyPressed(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }
	}


	//=====================================
	// Internal class for the color buttons
	//=====================================
	private class ColorPanel extends JPanel 
	{
		private RadioListener m_listener;
		private ButtonGroup   m_group;

		private ColorButton[] cButton = new ColorButton[m_numNeededColors];
		private ColorPatch colorPatch;

		public ColorPanel()
		{
			m_listener = new RadioListener();
			m_group = new ButtonGroup();

 			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			setBackground(Color.gray);
			setBorder(BorderFactory.createLineBorder(Color.black));

			JLabel chooseLabel = new JLabel("Choose:");
			chooseLabel.setForeground(Color.black);

			JLabel spacer = new JLabel("");
			//usingLabel.setForeground(Color.black);
			spacer.setMaximumSize(new Dimension(Short.MAX_VALUE,40));

			colorPatch = new ColorPatch();

			add(chooseLabel);
			for (int i=0; i<m_numNeededColors; i++) {
				cButton[i] = new ColorButton(m_neededColors[i]);
				add(cButton[i]);
			}

			add(spacer);
			add(colorPatch);
		}

		//=========================================
		// Internal class for a single color button
		//=========================================
		private class ColorButton extends JButton
		{
			public ColorButton(ColorInfo color)
			{
				setText(color.name());
				setBackground(color.bg());
				setForeground(color.fg());

				setMaximumSize(new Dimension(Short.MAX_VALUE,50)); 

				m_group.add(this);
				setActionCommand(color.name());
				addActionListener(m_listener);
				addKeyListener(new MyKeyListener());
			}
		}

		//===================================
		// Internal class for the color patch
		//===================================
		private class ColorPatch extends JLabel
		{
			public ColorPatch()
			{
				setOpaque(true);
				setMaximumSize(new Dimension(Short.MAX_VALUE,100)); 

				//setBorder(BorderFactory.createLineBorder(Color.gray));
				//setBorder(BorderFactory.createLoweredBevelBorder());
				setBorder(BorderFactory.createTitledBorder("using:"));

				//setHorizontalTextPosition(SwingConstants.CENTER);
			}
		}


		//=========================================
		// Internal class for the Radio listener
		//=========================================
		class RadioListener implements ActionListener { 
			public void actionPerformed(ActionEvent e) {
				String colorName = e.getActionCommand();

				for (int i=0; i<m_numNeededColors; i++) {
					if (colorName == m_neededColors[i].name()) {
						Color bg = m_neededColors[i].bg();
						Color fg = m_neededColors[i].fg();
						m_colorPanel.colorPatch.setBackground(bg);
						m_colorPanel.colorPatch.setForeground(fg);
						m_colorPanel.colorPatch.setText(colorName);
						setColor(bg);
					}
				}
			}
		}
	}


	//=====================================
	// Internal class for showing the grid.
	//=====================================
	private class PlayGrid extends JPanel implements MouseListener
	{
		// GUI objects
		private PictureSquare	m_squares[][];
		private JLabel			m_rowLabels[][];
		private JLabel			m_colLabels[][];

		// Stuff to remember between mouseDown and mouseUp
		boolean	m_down;
		boolean	m_dragged;
		int		m_downRow,		m_downColumn;
		int		m_currentRow,	m_currentColumn;

		private final Color	m_background = Color.gray;

		// Constructor
		public PlayGrid()
		{
			setBackground( m_background );
			setLayout( new GridLayout(m_numTotalRows,m_numTotalCols,1,1) );

			m_squares	= new PictureSquare[m_numGridRows][m_numGridCols];
			m_rowLabels = new JLabel[m_numGridRows][m_numKeyCols];
			m_colLabels = new JLabel[m_numGridCols][m_numKeyRows];

			for ( int r=0; r<m_numTotalRows; r++ ) {
				for ( int c=0; c<m_numTotalCols; c++ ) {

					// the squares that make up the picture
					if ( r < m_numGridRows  &&  c < m_numGridCols ) {
						PictureSquare square = new PictureSquare(r,c);
						m_squares[r][c] = square;
						add(square);
					}

					// the numbers to the right of the grid
					else if ( r < m_numGridRows && 
							  (c - m_numGridCols) < m_rowKeys.getSize(r) ) {
						int col = c - m_numGridCols;
						m_rowLabels[r][col] = makeKeyLabel(m_rowKeys, r, col);
					}

					// the numbers below the grid
					else if ( c < m_numGridCols && 
							  (r - m_numGridRows) < m_colKeys.getSize(c) ) {
						int row = r - m_numGridRows;
						m_colLabels[c][row] = makeKeyLabel(m_colKeys, c, row);
					}

					// leftover space (that we have to put something into 
					// to keep the GridLayout happy)
					else add(new JLabel());
				}
			}
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Dimension d	= getSize();

			int inc = 4;
			int xinc = d.width / m_numTotalCols;
			int yinc = d.height / m_numTotalRows;

			// draw a darker line on every 4th row and column
			g.setColor( getBackground().darker().darker() );
			for ( int r=0; r<=m_numGridRows; r+=inc ) {
				g.drawLine(0, ((r*yinc)-1), d.width, ((r*yinc)-1));
			}
			for ( int c=0; c<=m_numGridCols; c+=inc ) {
				g.drawLine(((c*xinc)-1), 0, ((c*xinc)-1), d.height);
			}

			// If we're dragging, draw a rubberband
			if ( m_down ) {
				Rectangle rect = buttonsSelected();
				g.setColor( Color.yellow );
				g.drawRect( ((rect.x*xinc)-1), ((rect.y*yinc)-1), 
							(rect.width*xinc), (rect.height*yinc) );
			}

		}


		// Public methods:
		//================
		public void myMouseDown( PictureSquare s )
		{
			m_down  = true;
			m_downRow	 = s.row();
			m_downColumn = s.col();

			repaint(); // to show the rubberband
		}

		public void myMouseDragged( PictureSquare s )
		{
			if ( m_down ) {
				m_dragged = true;
				m_currentRow = s.row();
				m_currentColumn = s.col();
				repaint(); // to show the rubberband
			}
		}

		public void myMouseUp(  )
		{
			Rectangle rect = buttonsSelected();
			m_down = false;
			m_dragged = false;

			// color the selected buttons
			for ( int r=rect.y; r<(rect.y+rect.height); r++ ) {
				for ( int c=rect.x; c<(rect.x+rect.width); c++ ) {
					m_grid.setVisibleColor( r, c, m_currentColor );
					m_squares[r][c].setBackground( m_currentColor );
				}
			}

			// recolor any keys that have changed
			for ( int r=rect.y; r<(rect.y+rect.height); r++ ) {
				m_rowKeys.setColors( r );
				for ( int c=0; c<m_rowKeys.getSize(r); c++ ) {
					m_rowLabels[r][c].setBackground(m_rowKeys.getBgColor(r,c));
				}
			}
			for ( int c=rect.x; c<(rect.x+rect.width); c++ ) {
				m_colKeys.setColors( c );
				for ( int r=0; r<m_colKeys.getSize(c); r++ ) {
					m_colLabels[c][r].setBackground(m_colKeys.getBgColor(c,r));
				}
			}

			// check to see if we've solved the puzzle
			if ( m_grid.isSolved() ) {
				showStatus("Congratulations, you have drawn " + filename());
			}
		}

		// Private methods
		private JLabel makeKeyLabel( Keyarray ka, int i, int j )
		{
			JLabel l = new JLabel( ""+ka.getNumber(i,j), JLabel.CENTER );
			l.setOpaque(true);
			l.setBackground( ka.getBgColor(i,j) );
			l.setForeground( ka.getFgColor(i,j) );
			add(l);
			return l;
		}

		private Rectangle buttonsSelected()
		{
			if ( m_dragged )
				return new Rectangle( Math.min(m_currentColumn, m_downColumn),
									  Math.min(m_currentRow, m_downRow),
									  Math.abs(m_currentColumn-m_downColumn)+1,
									  Math.abs(m_currentRow - m_downRow) + 1 );
			else
				return new Rectangle( m_downColumn, m_downRow, 1, 1 );
		}


		//======================================================
		// Internal class for the squares of the picture
		// (This has to be a button to make it listen to keys.)
		//======================================================
		private class PictureSquare extends JButton
			implements MouseListener, MouseMotionListener
		{
			private final int m_row;
			private final int m_col;

			// constructor
			public PictureSquare( int r, int c ) 
			{
				m_row = r;
				m_col = c;
				setOpaque(true);
				setBackground(m_grid.getVisibleColor( m_row, m_col ));

				// Make it look not like a button
				setBorderPainted(false);

				addMouseListener(this);
				addMouseMotionListener(this);
				addKeyListener(new MyKeyListener());
			}

			public int row() { return m_row; }
			public int col() { return m_col; }

			// The PictureSquare has to know:
			// if the mouse is pressed in it (to start a selection),
			// if the mouse is dragged through it (to extend the selection),
			// and if the mouse is released in it (to finish the selection).
			// (mouseExited or mouseEntered would be better to detect a
			// dragging, except that they aren't fired when the button is
			// held down.)

			// These methods are required by MouseListener.
			public void mouseClicked(MouseEvent e){;}
			public void mouseExited(MouseEvent e){;}
			public void mouseEntered(MouseEvent e) {;}

			public void mousePressed(MouseEvent e)
			{
				myMouseDown( this );
			}

			public void mouseReleased(MouseEvent e)
			{
				myMouseUp();
			}

			// These methods are required by MouseMotionListener.
			public void mouseMoved(MouseEvent e){;}

			public void mouseDragged(MouseEvent e)
			{
				myMouseDragged( getSquare(e) );
			}

			// Convenience function for figuring out the square under the mouse
			private PictureSquare getSquare(MouseEvent e)
			{
				int x = e.getX() + e.getComponent().getX();
				int y = e.getY() + e.getComponent().getY();
				return (PictureSquare)getParent().getComponentAt(x,y);
			}

		}

		// The PlayGrid only has to know if the mouse is released in it.
		// This is because if the user drags the mouse to the trough
		// between PictureSquares before releasing it, then none of the
		// PictureSquares will detect the release.

		// These methods are required by MouseListener.
		public void mouseClicked(MouseEvent e){;}
		public void mouseExited(MouseEvent e){;}
		public void mouseEntered(MouseEvent e) {;}
		public void mousePressed(MouseEvent e) {;}

		public void mouseReleased(MouseEvent e)
		{
			myMouseUp();
		}

	}

}
