
//=============================================================================
// Class for display
//=============================================================================
import java.awt.Color;

public class ColorInfo
{
	String	m_name;
	Color	m_bg;
	Color	m_fg;

	// constructor
	public ColorInfo(String name, Color bg)
	{
		m_name = name;
		m_bg = bg;

		if		( m_bg == Color.white )		m_fg = Color.black;
		else if ( m_bg == Color.lightGray )	m_fg = Color.black;
		else								m_fg = Color.lightGray;
	}

	// accessors
	public String name() { return m_name; }
	public Color bg() { return m_bg; }
	public Color fg() { return m_fg; }
}
