package com.blackrook.framework.multiform;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.blackrook.commons.Common;

/**
 * Parser for multipart form requests.
 * @author Matthew Tropiano
 * TODO: Finish.
 */
public class MultipartParser
{
	/** Content type boundary piece. */
	private static final String PIECE_BOUNDARY = "boundary=";
	/** Content-Disposition. */
	private static final String HEADER_DISPOSITION = "Content-Disposition:";
	/** Content-Disposition. */
	private static final String HEADER_TYPE = "Content-Type:";
	
	/** Boundary string. */
	private String boundary;
	/** Encoding type. */
	private String encoding;
	
	/**
	 * Creates and parses a new request.
	 * @param request the servlet request to parse. 
	 */
	public MultipartParser(HttpServletRequest request) throws MultipartParserException
	{
		encoding = "ISO-8859-1";
		String contentType = request.getContentType();

		HeaderParser parser = new HeaderParser(contentType);
		while (parser.hasTokens())
		{
			String piece = parser.nextToken();
			if (piece.startsWith(PIECE_BOUNDARY))
				boundary = piece.substring(PIECE_BOUNDARY.length());
			}
		
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(request.getInputStream());
			
			
			
		} catch (IOException e) {
			throw new MultipartParserException("Could not read request body.", e);
		} finally {
			Common.close(in);
			}
		}

	/**
	 * Returns true if a request is a multiform request, false
	 * otherwise.
	 */
	public static boolean isMultipart(HttpServletRequest request)
	{
		return request.getContentType().toLowerCase().startsWith("multipart/");
		}
	
	/**
	 * Multipart part.
	 */
	private class Part
	{
		/** Part name. */
		private String name;
		/** Part filename, if file. */
		private String fileName;
		/** Part content type. */
		private String contentType;
		/** Part length. */
		private int length;
		
		/** Data. */
		private byte[] data;
		
		Part(InputStream in) throws IOException
		{
			String line = null;
			}
		}
	
	/**
	 * Input reader for scanning parts.  
	 */
	private class InputReader
	{
		/** Input stream to read from. */
		private InputStream in;
		
		InputReader(InputStream in)
		{
			
			}
		
		}
	
	/**
	 * Header parser.
	 */
	private static class HeaderParser
	{
		/** Characters. */
		private char[] characters;
		/** Current position. */
		private int position;
		
		/**
		 * Creates a new header content parser.
		 * @param data the line to parse.
		 */
		HeaderParser(String data)
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
	
	
}
