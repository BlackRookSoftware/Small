/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.parser;

/**
 * Parser for lines that are broken by RFC-standard style text lines (most HTTP headers).
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
	 * @return true if this has tokens left.
	 */
	public boolean hasTokens()
	{
		return position < characters.length;
	}
	
	/**
	 * @return the next token.
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
