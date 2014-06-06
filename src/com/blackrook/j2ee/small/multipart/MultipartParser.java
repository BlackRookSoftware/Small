package com.blackrook.j2ee.small.multipart;

import java.io.File;
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
import com.blackrook.j2ee.small.lang.RFCParser;

/**
 * Abstract Multipart parser.
 * @author Matthew Tropiano
 * FIXME Kinda broken. Fix!
 */
public abstract class MultipartParser implements Iterable<Part>
{
	/** Newline. */
	protected final String NEWLINE = System.getProperty("line.separator");
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
	
	/** Byte buffer. */
	private byte[] buffer;
	
	/**
	 * Creates a new multipart parser.
	 */
	public MultipartParser()
	{
		this.random = new Random();
		this.partList = new List<Part>();
		this.charset = "ISO-8859-1";
		this.buffer = new byte[65536]; 
	}

	/**
	 * Returns true if a request is a multiform request, false
	 * otherwise.
	 */
	public static boolean isMultipart(HttpServletRequest request)
	{
		return request.getContentType().startsWith("multipart/");
	}

	/**
	 * Parses the request content.
	 * @param request the servlet request to parse. 
	 * @param outputDir the temporary directory for read files.
	 */
	public void parse(HttpServletRequest request, File outputDir) throws MultipartParserException, UnsupportedEncodingException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
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
		byte[] boundaryBytes = startBoundary.getBytes(charset);

		parseData(request, outputDir, startBoundary, endBoundary, boundaryBytes);
	}
	
	/**
	 * Parses the servlet content and generates.
	 * @param request
	 * @param outputDir
	 * @param startBoundary
	 * @param endBoudary
	 * @param startBoundaryBytes
	 */
	protected abstract void parseData(HttpServletRequest request, File outputDir, String startBoundary, String endBoundary, byte[] startBoundaryBytes)
		throws MultipartParserException, UnsupportedEncodingException;
	
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
						part.name = Common.urlUnescape(value.substring(1, value.length() - 1));
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
						part.fileName = Common.urlUnescape(value.substring(1, value.length() - 1));
				}
				else
					part.fileName = value;
			}
		}
		
	}

	// Parses the content type header.
	protected void parseContentType(String line, Part part)
	{
		String type = line.substring(HEADER_TYPE.length()).trim();
		part.contentType = type;
	}

	/**
	 * Scans and returns the next line.
	 * @param sis the servlet input stream (for request content).
	 * @return the read string.
	 * @throws IOException if the stream could not be read.
	 */
	protected String scanLine(ServletInputStream sis) throws IOException
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

	/**
	 * Scans an input stream until it hits a part boundary.
	 * @param in the input stream.
	 * @param out the output stream.
	 * @param boundaryBytes the boundary as bytes.
	 * @throws IOException if the input stream could not be 
	 * read or the output stream could not be written to.
	 */
	protected void scanDataUntilBoundary(InputStream in, OutputStream out, byte[] boundaryBytes) throws IOException
	{
		byte b = 0;
		int buf = 0;
		int match = 0;
		int nlmatch = 0;
		
		while (match != boundaryBytes.length && (buf = in.read()) != -1)
		{
			b = (byte)(buf & 0x0ff);
			if (nlmatch == 2 || match > 0)
			{
				if (b == boundaryBytes[match])
					match++;
				else
				{
					out.write(NEWLINE_BYTES, 0, nlmatch);
					out.write(boundaryBytes, 0, match);
					if (match > 1)
						Common.noop();
					match = 0;
					nlmatch = 0;
					out.write(b);
				}
			}
			else if (nlmatch > 0)
			{
				if (b == boundaryBytes[match])
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

	/**
	 * Creates a temporary file for read part data.
	 * @param outputDir the temporary output directory.
	 * @return the file created.
	 * @throws IOException if the canonical path could not be read.
	 */
	protected File generateTempFile(String filename, File outputDir) throws IOException
	{
		final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		final String PREFIX = "/MULTIFORM";
		final String SUFFIX = "." + Common.getFileExtension(filename);
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
	 * Returns all of the parts parsed by this parser.
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

}
