package com.blackrook.framework.multiform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.DataList;
import com.blackrook.commons.list.List;
import com.blackrook.framework.util.BRUtil;

/**
 * Parser for multipart form requests.
 * @author Matthew Tropiano
 * TODO: Finish: decode field names better. Read data better.
 */
public class MultipartParser implements Iterable<MultipartParser.Part>
{
	/** Content type boundary piece. */
	private static final String PIECE_BOUNDARY = "boundary=";
	/** Content type charset piece. */
	private static final String PIECE_CHARSET = "charset=";
	/** Content-Disposition. */
	private static final String HEADER_DISPOSITION = "Content-Disposition:";
	/** Content-Disposition. */
	private static final String HEADER_TYPE = "Content-Type:";
	
	/** Boundary string. */
	private String boundary;
	/** Charset type. */
	private String charset;
	
	/** List of parsed parts. */
	private List<Part> partList;
	
	/**
	 * Creates and parses a new request.
	 * @param request the servlet request to parse. 
	 */
	public MultipartParser(HttpServletRequest request) throws MultipartParserException, UnsupportedEncodingException
	{
		partList = new List<MultipartParser.Part>();
		charset = "ISO-8859-1";
		String contentType = request.getContentType();
		
		HeaderParser parser = new HeaderParser(contentType);
		while (parser.hasTokens())
		{
			String piece = parser.nextToken();
			if (piece.startsWith(PIECE_BOUNDARY))
				boundary = piece.substring(PIECE_BOUNDARY.length());
			else if (piece.startsWith(PIECE_CHARSET))
				charset = piece.substring(PIECE_CHARSET.length());
			}
		
		String startBoundary = "--" + boundary;
		String endBoundary = startBoundary + "--";
		byte[] NEWLINE_BYTES = System.getProperty("line.separator").getBytes(charset);
		
		final int STATE_START = 0;
		final int STATE_HEADER = 1;
		final int STATE_DATA = 2;
		final int STATE_END = 3;
		int state = STATE_START;
		DataList data = new DataList();
		
		Part currentPart = null;
		
		InputStream in = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in = request.getInputStream(), charset));
			
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				switch (state)
				{
					// Start Parsing.
					case STATE_START:
						currentPart = new Part();
						if (line.equals(startBoundary))
							state = STATE_HEADER;
						break;

					// Header Parsing.
					case STATE_HEADER:
						if (line.startsWith(HEADER_DISPOSITION))
							parseDisposition(line, currentPart);
						else if (line.startsWith(HEADER_TYPE))
							parseContentType(line, currentPart);
						else if (line.length() == 0)
						{
							state = STATE_DATA;
							data.clear();
							}
						else if (line.equals(startBoundary) || line.equals(endBoundary))
							throw new MultipartParserException("Found boundary in header. Submission is malformed.");
						break;
						
					// Data Reading.
					case STATE_DATA:
						if (line.equals(startBoundary))
						{
							currentPart.data = data.toByteArray();
							partList.add(currentPart);
							
							currentPart = new Part();
							state = STATE_HEADER;
							}
						else if (line.equals(endBoundary))
						{
							currentPart.data = data.toByteArray();
							partList.add(currentPart);
							state = STATE_END;
							}
						else
						{
							// if we previously read data, and didn't find the boundary, must have been a newline.
							// if (data.size() > 0) data.append(NEWLINE_BYTES);
							data.append(line.getBytes(charset));
							}
						break;
					}
				
				//System.out.println("[["+line+"]]");
				}
			
		} catch (IOException e) {
			throw new MultipartParserException("Could not read request body.", e);
		} finally {
			Common.close(in);
			}
		}
	
	// Parses the disposition header.
	private void parseDisposition(String line, Part part) throws MultipartParserException
	{
		HeaderParser hp = new HeaderParser(line.substring(HEADER_DISPOSITION.length()));
		while (hp.hasTokens())
		{
			String token = hp.nextToken();
			if (token.startsWith("name="))
			{
				String value = token.substring("name=".length());
				if (value.startsWith("\""))
				{
					if (!value.endsWith("\""))
						throw new MultipartParserException("Missing closing quote in header disposition.");
					else
						part.name = BRUtil.convertFromHTMLEntities(value.substring(1, value.length() - 1));
					}
				else
					part.name = value;
				}
			else if (token.startsWith("filename="))
			{
				String value = token.substring("filename=".length());
				if (value.startsWith("\""))
				{
					if (!value.endsWith("\""))
						throw new MultipartParserException("Missing closing quote in header disposition.");
					else
						part.fileName = BRUtil.convertFromHTMLEntities(value.substring(1, value.length() - 1));
					}
				else
					part.fileName = value;
				}
			}
		
		}

	// Parses the content type header.
	private void parseContentType(String line, Part part)
	{
		String type = line.substring(HEADER_TYPE.length()).trim();
		part.contentType = type;
		}

	/**
	 * Returns true if a request is a multiform request, false
	 * otherwise.
	 */
	public static boolean isMultipart(HttpServletRequest request)
	{
		return request.getContentType().toLowerCase().startsWith("multipart/");
		}

	@Override
	public Iterator<Part> iterator()
	{
		return partList.iterator();
		}
	
	/**
	 * Multipart part.
	 */
	public class Part
	{
		/** Part name. */
		private String name;
		/** Part filename, if file. */
		private String fileName;
		/** Part content type. */
		private String contentType;
		/** Data. */
		private byte[] data;
		
		private Part()
		{
			}
		
		public String getName()
		{
			return name;
			}

		public boolean isFile()
		{
			return fileName != null;
			}

		public String getFileName()
		{
			return fileName;
			}

		public String getContentType()
		{
			return contentType;
			}

		public int getLength()
		{
			return data.length;
			}

		public byte[] getData()
		{
			return data;
			}

		public String getText()
		{
			try {
				return new String(data, charset);
			} catch (UnsupportedEncodingException e) {
				return null;
				}
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

	/**
	 * Special scanner for multipart form data.
	 */
	private class Scanner
	{
		private StringBuffer charBuffer;
		private DataList dataBuffer;
		private InputStreamReader reader;
		
		Scanner(InputStream in) throws UnsupportedEncodingException
		{
			this.reader = new InputStreamReader(in, charset);
			charBuffer = new StringBuffer();
			dataBuffer = new DataList();
			}
		
		
		
		
		}
	
}
