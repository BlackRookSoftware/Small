/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.multipart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.blackrook.small.exception.request.MultipartParserException;
import com.blackrook.small.parser.RFCParser;
import com.blackrook.small.struct.Utils;

/**
 * Abstract Multipart parser.
 * @author Matthew Tropiano
 */
public abstract class MultipartParser implements Iterable<Part>
{
	/** Newline. */
	protected final String NEWLINE = "\r\n";
	/** Newline as bytes. */
	protected final byte[] NEWLINE_BYTES = NEWLINE.getBytes();
	
	/** Content type boundary piece. */
	protected static final String PIECE_BOUNDARY = "boundary=";
	/** Content type charset piece. */
	protected static final String PIECE_CHARSET = "charset=";
	/** Content-Disposition. */
	protected static final String HEADER_DISPOSITION = "Content-Disposition:";
	/** Content-Disposition. */
	protected static final String HEADER_TYPE = "Content-Type:";

	/** Random number generator. */
	private Random random;
	/** Boundary string. */
	private String boundary;
	/** Charset type. */
	private String charset;
	
	/** List of parsed parts. */
	private List<Part> partList;
	
	/**
	 * Creates a new multipart parser.
	 */
	public MultipartParser()
	{
		this.random = new Random();
		this.partList = new ArrayList<Part>();
		this.charset = "ISO-8859-1";
	}

	/**
	 * Checks if the request is a multipart form.
	 * @param request the servlet request.
	 * @return true if a request is a multipart request, false otherwise.
	 */
	public static boolean isMultipart(HttpServletRequest request)
	{
		return !Utils.isEmpty(request.getContentType()) && request.getContentType().startsWith("multipart/");
	}

	/**
	 * Parses the request content.
	 * @param request the servlet request to parse. 
	 * @param outputDir the temporary directory for read files.
	 * @throws MultipartParserException if a parsing error occurs in parsing the content.
	 * @throws IOException if a read error occurs.
	 */
	public void parse(HttpServletRequest request, File outputDir) throws MultipartParserException, IOException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
		while (parser.hasTokens())
		{
			String piece = parser.nextToken();
			if (piece.startsWith(PIECE_BOUNDARY))
				boundary = unquote(piece.substring(PIECE_BOUNDARY.length()));
			else if (piece.startsWith(PIECE_CHARSET))
				charset = unquote(piece.substring(PIECE_CHARSET.length()));
		}
		
		String startBoundary = "--" + boundary;
		String endBoundary = startBoundary + "--";
		byte[] boundaryBytes = startBoundary.getBytes(charset);

		parseData(request.getInputStream(), outputDir, startBoundary, endBoundary, boundaryBytes);
	}

	/**
	 * Parses the servlet content and generates parts.
	 * @param inStream the input servlet stream.
	 * @param outputDir the output directory
	 * @param startBoundary the boundary string for each part. 
	 * @param endBoundary  the last boundary string.
	 * @param startBoundaryBytes the boundary string for each part as bytes in the correct encoding.
	 * @throws MultipartParserException if something is malformed in the request body.
	 * @throws UnsupportedEncodingException if the part encoding is not supported.
	 * @throws IOException if a read error occurs.
	 */
	protected abstract void parseData(ServletInputStream inStream, File outputDir, String startBoundary, String endBoundary, byte[] startBoundaryBytes)
		throws MultipartParserException, UnsupportedEncodingException, IOException;
	
	// Parses the disposition header.
	protected void parseDisposition(String line, Part part) throws MultipartParserException
	{
		RFCParser hp = new RFCParser(line.substring(HEADER_DISPOSITION.length()));
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
						part.setName(Utils.urlUnescape(value.substring(1, value.length() - 1)));
				}
				else
					part.setName(value);
			}
			else if (token.startsWith("filename="))
			{
				String value = token.substring("filename=".length());
				if (value.startsWith("\""))
				{
					if (!value.endsWith("\""))
						throw new MultipartParserException("Missing closing quote in header disposition.");
					else
						part.setFileName(Utils.urlUnescape(value.substring(1, value.length() - 1)));
				}
				else
					part.setFileName(value);
			}
		}
		
	}

	// Parses the content type header.
	protected void parseContentType(String line, Part part)
	{
		String type = line.substring(HEADER_TYPE.length()).trim();
		part.setContentType(type);
	}

	/**
	 * Scans and returns the next line.
	 * @param in the servlet input stream (for request content).
	 * @return the read string.
	 * @throws IOException if the stream could not be read.
	 */
	protected String scanLine(InputStream in) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int buf = 0;
		boolean match = false;
		
		while (true)
		{
			buf = in.read();
			if (buf >= 0)
			{
				if (!match)
				{
					if (buf == 0x0d) // \r
						match = true;
					else
						bos.write(buf);
				}
				else
				{
					if (buf == 0x0a) // \n
						break;
					else
					{
						bos.write(0x0d);
						bos.write(buf);
					}
				}
			}
		}
		
		String outstr = new String(bos.toByteArray(), charset);
		return outstr.substring(0, outstr.length());
	}

	/**
	 * Scans an input stream into an output stream until it hits a part boundary.
	 * @param in the input stream.
	 * @param out the output stream.
	 * @param boundaryBytes the boundary as bytes.
	 * @return the amount of bytes read.
	 * @throws IOException if the input stream could not be read or the output stream could not be written to.
	 */
	protected int scanDataUntilBoundary(InputStream in, OutputStream out, byte[] boundaryBytes) throws IOException
	{
		byte b = 0;
		int buf = 0;
		int match = 0;
		int count = 0;
		
		while (match != boundaryBytes.length && (buf = in.read()) != -1)
		{
			count++;
			b = (byte)(buf & 0x0ff);
			if (match > 0)
			{
				if (b == boundaryBytes[match])
					match++;
				else
				{
					out.write(boundaryBytes, 0, match);
					match = 0;
					out.write(b);
				}
			}
			else if (b == boundaryBytes[0])
			{
				match++;
			}
			else
				out.write(b);
		}
		
		return count;
	}

	/**
	 * Creates a temporary file for read part data.
	 * @param filename the Part's file name (extension is pulled from this).
	 * @param outputDir the temporary output directory.
	 * @return the file created.
	 * @throws IOException if the canonical path could not be read.
	 */
	protected File generateTempFile(String filename, File outputDir) throws IOException
	{
		final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		final String PREFIX = "/MULTIFORM";
		final String SUFFIX = "." + Utils.getFileExtension(filename);
		char[] out = new char[32];
		for (int i = 0; i < out.length; i++)
			out[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
		
		StringBuffer sb = new StringBuffer();
		sb.append(PREFIX);
		sb.append(out);
		sb.append(SUFFIX);
		return new File(outputDir.getCanonicalFile().getPath() + sb.toString());
	}
	
	/**
	 * Adds a part to the multipart parser.
	 * @param part the part to add.
	 */
	protected void addPart(Part part)
	{
		partList.add(part);
	}

	/**
	 * @return all of the parts parsed by this parser.
	 */
	public List<Part> getPartList()
	{
		return partList;
	}

	@Override
	public Iterator<Part> iterator()
	{
		return partList.iterator();
	}

	private static String unquote(String value)
	{
		if (value.charAt(0) == '"')
			return value.substring(1, value.length() - 1);
		return value;
	}
	
}
