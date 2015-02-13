//****************************************************************************
// PBN.java:	Applet
//****************************************************************************

import java.awt.BorderLayout;
import java.awt.Color;
import java.net.URL;
import java.util.*;
import javax.swing.*;


//=============================================================================
// Main Class for applet PBN
//=============================================================================
public class PBN13 extends JApplet
{

	// Members for applet parameters (with default values)
	private String	m_filename	= "";
	private boolean	m_solved	= false;

	// Parameter names.
	private final String PARAM_filename	= "filename";
	private final String PARAM_solved	= "solved";

	// member objects
	private Picture		m_picture;
	private Grid		m_grid;
	private Keyarray	m_rowKeys;
	private Keyarray	m_colKeys;
	private ColorInfo	m_neededColors[];
	private int			m_numColors;

	// successful initialization flag
	private Exception	m_exception;

	// accessors
	public String		filename()		{ return m_filename; }
	public Grid			grid()			{ return m_grid; }
	public Keyarray		rowKeys()		{ return m_rowKeys; }
	public Keyarray		colKeys()		{ return m_colKeys; }
	public ColorInfo[]	neededColors()	{ return m_neededColors; }
	public int			numNeededColors() { return m_numColors; };

	// PBN Class Constructor
	//-------------------------------------------------------------------------
	public PBN13()
	{
		// initialization done in the init method
	}

	// APPLET INFO:
	//-------------------------------------------------------------------------
	public String getAppletInfo()
	{
		return
			"Name:   Paint By Numbers for java1.3\r\n" +
			"Author: Ali Corbin\r\n" +
			"Date:   Sept 2003\r\n";
	}

	// PBN Parameter Information:
	public String[][] getParameterInfo()
	{
		String[][] info =
			{// "Name",			"Type",		"Description"
				{ PARAM_filename, "String",	"name of the file to load" },
				{ PARAM_solved,	"boolean",	"should it start out solved?" },
			};
		return info;		
	}

	// The init() method is called when an applet is first loaded or reloaded.
	//-------------------------------------------------------------------------
	public void init()
	{
		// Read the input parameters into the member variables.
		readParams();

		// Create the objects that we'll need for the guts of the program.
		makeGuts();

		// Create the objects that we'll need for the GUI.
		makeGUI();
	}

	// Place additional applet clean up code here.  destroy() is called when
	// when your applet is terminating and being unloaded.
	//-------------------------------------------------------------------------
	public void destroy()
	{
		// nothing to do with me!
	}

	// Private methods:
	//================

	// Read the input parameters into the member variables.
	private void readParams()
	{
		String param;

		// name of the file to load
		param = getParameter(PARAM_filename);
		if (param != null)	  m_filename = param;

		// should the puzzle start out solved?
		param = getParameter(PARAM_solved);	
		if (param != null)	  m_solved = Boolean.valueOf(param).booleanValue();
	}

	// Create the objects that we'll need for the guts of the program.
	private void makeGuts()
	{
		m_picture	= new Picture();
		URL base = getDocumentBase();
		Color colors[][];
		try {
			colors = m_picture.create(base, m_filename);
		}
		catch ( Exception e ) { m_exception = e; return; }

		m_grid		= new Grid( colors, m_solved );
		m_rowKeys	= new Keyarray(m_grid, true);
		m_colKeys	= new Keyarray(m_grid, false);

		// Calculate which (and how many) colors are needed.
		Hashtable ht = new Hashtable();
		for ( int r=0; r<m_grid.getNumRows(); r++ ) {
			for ( int c=0; c<m_grid.getNumCols(); c++ ) {
				Color color = m_grid.getHiddenColor( r, c );
				if ( !ht.contains(color) )
					ht.put(color,color);
			}
		}

		m_neededColors = new ColorInfo[ht.size() + 1];
		m_numColors = 0;

		// unguessed color, always needed
		m_neededColors[m_numColors++] = new ColorInfo("Gray", Color.lightGray);

		// background color, always needed
		m_neededColors[m_numColors++] = new ColorInfo("White", Color.white);

		// Other possible colors, in this order...
		if ( ht.contains(Color.black) )
			m_neededColors[m_numColors++] = new ColorInfo("Black",Color.black);

		if ( ht.contains(Color.red) )
			m_neededColors[m_numColors++] = new ColorInfo("Red", Color.red);

		if ( ht.contains(Color.green) )
			m_neededColors[m_numColors++] = new ColorInfo("Green",Color.green);

		if ( ht.contains(Color.blue) )
			m_neededColors[m_numColors++] = new ColorInfo("Blue", Color.blue);
	}

	// Create the objects that we'll need for the GUI.
	private void makeGUI()
	{
		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add( new PlayArea(this), "Center" );
	}

}
