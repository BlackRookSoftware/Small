package com.blackrook.j2ee.small.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.blackrook.commons.Common;

/**
 * Parser for multipart form requests.
 * @author Matthew Tropiano
 */
public class MultipartFormDataParser extends MultipartParser
{
	
	/**
	 * Creates a new form-data parser.
	 */
	public MultipartFormDataParser()
	{
		super();
	}

	@Override
	protected void parseData(HttpServletRequest request, File outputDir, String startBoundary, String endBoundary, byte[] startBoundaryBytes)
		throws MultipartParserException, UnsupportedEncodingException
	{
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
			addPart(currentPart);
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
								outFile = generateTempFile(currentPart.fileName, outputDir);
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
										addPart(currentPart);
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
						scanDataUntilBoundary(sis, out, startBoundaryBytes);
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
							addPart(currentPart);
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

}
