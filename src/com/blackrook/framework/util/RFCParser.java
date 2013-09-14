package com.blackrook.framework.util;

/**
 * Parser for lines that are broken by RFC-standard style text lines.
 * @author Matthew Tropiano
 */
public class RFCParser
{
	/** Characters. */
	private char[] characters;
	/** Current position. */
	private int position;
	
	/**
	 * Creates a new header content parser.
	 * @param data the line to parse.
	 */
	public RFCParser(String data)
	{
		characters = data.trim().toCharArray();
		position = 0;
		}
	
	/**
	 * Returns true if this has tokens left.
	 */
	public boolean hasTokens()
	{
		return position < characters.length;
		}
	
	/**
	 * Returns the next token.
	 */
	public String nextToken()
	{
		final int STATE_INIT = 0;
		final int STATE_TOKEN = 1;
		
		StringBuilder sb = new StringBuilder();
		int state = STATE_INIT;
		boolean good = true;
		
		char c = characters[position];
		while (good && position < characters.length)
		{
			c = characters[position];
			switch (state)
			{
				case STATE_INIT:
					if (!Character.isWhitespace(c))
						state = STATE_TOKEN;
					else
						position++;
					break;
				case STATE_TOKEN:
					if (c == ';')
					{
						position++;
						good = false;
						}
					else
					{
						sb.append(c);
						position++;
						}
					break;
				}
			}
		
		return sb.toString();
		}
}
