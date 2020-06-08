/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.multipart;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletInputStream;

import com.blackrook.small.struct.Utils;

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
	protected void parseData(ServletInputStream sis, File outputDir, String startBoundary, String endBoundary, byte[] startBoundaryBytes)
		throws MultipartParserException, UnsupportedEncodingException
	{
		final int STATE_HEADER = 1;
		final int STATE_DATA = 2;
		final int STATE_END = 3;
		int state = STATE_HEADER;
		
		File outFile = null; 
		Part currentPart = null;

		OutputStream out = null;
		
		try (InputStream fin = new BufferedInputStream(sis, 65536)) 
		{
			String line = null;

			currentPart = new Part();
			addPart(currentPart);
			line = scanLine(fin);
			if (!line.equals(startBoundary))
				throw new MultipartParserException("Unexpected beginning of multipart form. Submission is malformed.");

			while (state != STATE_END)
			{
				switch (state)
				{
					// Header Parsing.
					case STATE_HEADER:
						
						line = scanLine(fin);
						
						if (line.startsWith(HEADER_DISPOSITION))
							parseDisposition(line, currentPart);
						else if (line.startsWith(HEADER_TYPE))
							parseContentType(line, currentPart);
						else if (line.length() == 0)
						{
							if (currentPart.getFileName() != null)
							{
								state = STATE_DATA;
								outFile = generateTempFile(currentPart.getFileName(), outputDir);
								currentPart.setFile(outFile);
								out = new BufferedOutputStream(new FileOutputStream(outFile), 65536);
							}
							else
							{
								line = scanLine(fin);
								if (line == null)
									throw new MultipartParserException("Unexpected end of multipart form. Submission is malformed.");

								boolean loop = true;
								currentPart.setValue(line);
								while (loop)
								{
									line = scanLine(fin);
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
										currentPart.setValue(currentPart.getValue() + "\r\n" + line);
								}
							}
						}
						else if (line.startsWith(startBoundary))
							throw new MultipartParserException("Found boundary in header. Submission is malformed.");
						break;
						
					// Data Reading.
					case STATE_DATA:
					{
						scanDataUntilBoundary(fin, out, startBoundaryBytes);
						out.close();
						out = null;
						outFile = null;
						line = scanLine(fin);
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
			Utils.close(out);
		}
		
	}

}
