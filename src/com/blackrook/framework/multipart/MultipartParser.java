package com.blackrook.framework.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Random;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;
import com.blackrook.framework.util.BRUtil;

/**
 * Parser for multipart form requests.
 * @author Matthew Tropiano
 */
public class MultipartParser implements Iterable<Part>
{
	/** Newline bytes. */
	private final String NEWLINE = System.getProperty("line.separator");
	private final byte[] NEWLINE_BYTES = NEWLINE.getBytes();
	
	/** Content type boundary piece. */
	private static final String PIECE_BOUNDARY = "boundary=";
	/** Content type charset piece. */
	private static final String PIECE_CHARSET = "charset=";
	/** Content-Disposition. */
	private static final String HEADER_DISPOSITION = "Content-Disposition:";
	/** Content-Disposition. */
	private static final String HEADER_TYPE = "Content-Type:";

	/** Random number generator. */
	private Random random;
	/** Boundary string. */
	private String boundary;
	/** Charset type. */
	private String charset;
	
	/** List of parsed parts. */
	private List<Part> partList;
	
	/** Byte buffer. */
	private byte[] buffer;
	
	/**
	 * Creates and parses a new request.
	 * @param request the servlet request to parse. 
	 * @param outputDir the temporary directory for read files.
	 */
	public MultipartParser(HttpServletRequest request, File outputDir) throws MultipartParserException, UnsupportedEncodingException
	{
		this.random = new Random();
		this.partList = new List<Part>();
		this.charset = "ISO-8859-1";
		this.buffer = new byte[8192]; 
				
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
		byte[] START_BOUNDARY_BYTES = startBoundary.getBytes(charset);

		final int STATE_HEADER = 1;
		final int STATE_DATA = 2;
		final int STATE_END = 3;
		int state = STATE_HEADER;
		
		File outFile = null; 
		Part currentPart = null;
		
		OutputStream out = null;
		ServletInputStream sis = null;
		
		try {
			sis = request.getInputStream();
			String line = null;

			currentPart = new Part();
			partList.add(currentPart);
			line = scanLine(sis);
			if (!line.equals(startBoundary))
				throw new MultipartParserException("Unexpected beginning of multipart form. Submission is malformed.");

			while (state != STATE_END)
			{
				switch (state)
				{
					// Header Parsing.
					case STATE_HEADER:
						line = scanLine(sis);
						if (line.startsWith(HEADER_DISPOSITION))
							parseDisposition(line, currentPart);
						else if (line.startsWith(HEADER_TYPE))
							parseContentType(line, currentPart);
						else if (line.length() == 0)
						{
							if (currentPart.fileName != null)
							{
								state = STATE_DATA;
								outFile = generateTempFile(outputDir);
								currentPart.file = outFile;
								out = new FileOutputStream(outFile);
								}
							else
							{
								line = scanLine(sis);
								if (line == null)
									throw new MultipartParserException("Unexpected end of multipart form. Submission is malformed.");

								boolean loop = true;
								currentPart.value = line;
								while (loop)
								{
									line = scanLine(sis);
									if (line.equals(startBoundary))
									{
										state = STATE_HEADER;
										currentPart = new Part();
										partList.add(currentPart);
										loop = false;
										}
									else if (line.equals(endBoundary))
									{
										state = STATE_END;
										loop = false;
										}
									else
										currentPart.value += "\n" + line;
									}
								}
							}
						else if (line.startsWith(startBoundary))
							throw new MultipartParserException("Found boundary in header. Submission is malformed.");
						break;
						
					// Data Reading.
					case STATE_DATA:
					{
						scanDataUntilBoundary(sis, out, START_BOUNDARY_BYTES);
						out.close();
						out = null;
						outFile = null;
						line = scanLine(sis);
						if (line.equals("--"))
							state = STATE_END;
						else if (line.length() == 0)
						{
							state = STATE_HEADER;
							currentPart = new Part();
							partList.add(currentPart);
							}
						else
							throw new MultipartParserException("Data terminated with bad boundary. Submission is malformed.");
						}
						break;
					}
				}
			
		} catch (IOException e) {
			throw new MultipartParserException("Could not read request body.", e);
		} finally {
			Common.close(out);
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
	 * Returns all of the parts parsed by this parser.
	 */
	public List<Part> getPartList()
	{
		return partList;
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
						part.name = BRUtil.urlDecode(value.substring(1, value.length() - 1));
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
						part.fileName = BRUtil.urlDecode(value.substring(1, value.length() - 1));
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
	
	// Creates a temp file.
	private File generateTempFile(File outputDir) throws IOException
	{
		final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		final String PREFIX = "/MULTIFORM";
		final String SUFFIX = ".bin";
		char[] out = new char[32];
		for (int i = 0; i < out.length; i++)
			out[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
		
		StringBuffer sb = new StringBuffer();
		sb.append(PREFIX);
		sb.append(out);
		sb.append(SUFFIX);
		return new File(outputDir.getCanonicalFile().getPath() + sb.toString());
		}

	// scans until finds the boundary.
	private String scanLine(ServletInputStream sis) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		int buf = 0;
		
		do {
			buf = sis.readLine(buffer, 0, buffer.length);
			String s = new String(buffer, 0, buf, charset);
			sb.append(s, 0, s.length() - NEWLINE.length());
		} while (buf == buffer.length);
		 
		return sb.toString();
		}
	
	// scans until finds the boundary.
	private void scanDataUntilBoundary(InputStream in, OutputStream out, byte[] startBoundary) throws IOException
	{
		byte b = 0;
		int buf = 0;
		int match = 0;
		int nlmatch = 0;
		
		while (match != startBoundary.length && (buf = in.read()) != -1)
		{
			b = (byte)(buf & 0x0ff);
			if (nlmatch == 2 || match > 0)
			{
				if (b == startBoundary[match])
					match++;
				else
				{
					out.write(NEWLINE_BYTES, 0, nlmatch);
					out.write(startBoundary, 0, match);
					match = 0;
					nlmatch = 0;
					out.write(b);
					}
				}
			else if (nlmatch > 0)
			{
				if (b == startBoundary[match])
					match++;
				else if (b == NEWLINE_BYTES[nlmatch])
					nlmatch++;
				else
				{
					out.write(NEWLINE_BYTES, 0, nlmatch);
					out.write(b);
					nlmatch = 0;
					}
				}
			else if (b == NEWLINE_BYTES[nlmatch])
				nlmatch++;
			else
				out.write(b);
			}
		}
	
	@Override
	public Iterator<Part> iterator()
	{
		return partList.iterator();
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
