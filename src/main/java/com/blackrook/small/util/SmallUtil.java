/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.ServerContainer;

import com.blackrook.small.SmallConfiguration;
import com.blackrook.small.SmallConstants;
import com.blackrook.small.SmallEnvironment;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.roles.MIMETypeDriver;
import com.blackrook.small.struct.Utils;

/**
 * Utility library for common or useful functions.
 * @author Matthew Tropiano
 */
public final class SmallUtil
{
	/** MIME type for JSON */
	public static final String CONTENT_MIME_TYPE_JSON = "application/json";
	/** MIME type for XML */
	public static final String CONTENT_MIME_TYPE_XML = "application/xml";

	/** Map of entity names to characters. */
	public static final HashMap<String, Character> ENTITY_NAME_MAP = new HashMap<String, Character>()
	{
		private static final long serialVersionUID = 6040588289725369336L;
		{
			put("nbsp", '\u00A0');
			put("iexcl", '\u00A1');
			put("cent", '\u00A2');
			put("pound", '\u00A3');
			put("curren", '\u00A4');
			put("yen", '\u00A5');
			put("brvbar", '\u00A6');
			put("sect", '\u00A7');
			put("uml", '\u00A8');
			put("copy", '\u00A9');
			put("ordf", '\u00AA');
			put("laquo", '\u00AB');
			put("not", '\u00AC');
			put("shy", '\u00AD');
			put("reg", '\u00AE');
			put("macr", '\u00AF');
			put("deg", '\u00B0');
			put("plusmn", '\u00B1');
			put("sup2", '\u00B2');
			put("sup3", '\u00B3');
			put("acute", '\u00B4');
			put("micro", '\u00B5');
			put("para", '\u00B6');
			put("middot", '\u00B7');
			put("cedil", '\u00B8');
			put("sup1", '\u00B9');
			put("ordm", '\u00BA');
			put("raquo", '\u00BB');
			put("frac14", '\u00BC');
			put("frac12", '\u00BD');
			put("frac34", '\u00BE');
			put("iquest", '\u00BF');
			put("Agrave", '\u00C0');
			put("Aacute", '\u00C1');
			put("Acirc", '\u00C2');
			put("Atilde", '\u00C3');
			put("Auml", '\u00C4');
			put("Aring", '\u00C5');
			put("AElig", '\u00C6');
			put("Ccedil", '\u00C7');
			put("Egrave", '\u00C8');
			put("Eacute", '\u00C9');
			put("Ecirc", '\u00CA');
			put("Euml", '\u00CB');
			put("Igrave", '\u00CC');
			put("Iacute", '\u00CD');
			put("Icirc", '\u00CE');
			put("Iuml", '\u00CF');
			put("ETH", '\u00D0');
			put("Ntilde", '\u00D1');
			put("Ograve", '\u00D2');
			put("Oacute", '\u00D3');
			put("Ocirc", '\u00D4');
			put("Otilde", '\u00D5');
			put("Ouml", '\u00D6');
			put("times", '\u00D7');
			put("Oslash", '\u00D8');
			put("Ugrave", '\u00D9');
			put("Uacute", '\u00DA');
			put("Ucirc", '\u00DB');
			put("Uuml", '\u00DC');
			put("Yacute", '\u00DD');
			put("THORN", '\u00DE');
			put("szlig", '\u00DF');
			put("agrave", '\u00E0');
			put("aacute", '\u00E1');
			put("acirc", '\u00E2');
			put("atilde", '\u00E3');
			put("auml", '\u00E4');
			put("aring", '\u00E5');
			put("aelig", '\u00E6');
			put("ccedil", '\u00E7');
			put("egrave", '\u00E8');
			put("eacute", '\u00E9');
			put("ecirc", '\u00EA');
			put("euml", '\u00EB');
			put("igrave", '\u00EC');
			put("iacute", '\u00ED');
			put("icirc", '\u00EE');
			put("iuml", '\u00EF');
			put("eth", '\u00F0');
			put("ntilde", '\u00F1');
			put("ograve", '\u00F2');
			put("oacute", '\u00F3');
			put("ocirc", '\u00F4');
			put("otilde", '\u00F5');
			put("ouml", '\u00F6');
			put("divide", '\u00F7');
			put("oslash", '\u00F8');
			put("ugrave", '\u00F9');
			put("uacute", '\u00FA');
			put("ucirc", '\u00FB');
			put("uuml", '\u00FC');
			put("yacute", '\u00FD');
			put("thorn", '\u00FE');
			put("yuml", '\u00FF');
			put("fnof", '\u0192');
			put("Alpha", '\u0391');
			put("Beta", '\u0392');
			put("Gamma", '\u0393');
			put("Delta", '\u0394');
			put("Epsilon", '\u0395');
			put("Zeta", '\u0396');
			put("Eta", '\u0397');
			put("Theta", '\u0398');
			put("Iota", '\u0399');
			put("Kappa", '\u039A');
			put("Lambda", '\u039B');
			put("Mu", '\u039C');
			put("Nu", '\u039D');
			put("Xi", '\u039E');
			put("Omicron", '\u039F');
			put("Pi", '\u03A0');
			put("Rho", '\u03A1');
			put("Sigma", '\u03A3');
			put("Tau", '\u03A4');
			put("Upsilon", '\u03A5');
			put("Phi", '\u03A6');
			put("Chi", '\u03A7');
			put("Psi", '\u03A8');
			put("Omega", '\u03A9');
			put("alpha", '\u03B1');
			put("beta", '\u03B2');
			put("gamma", '\u03B3');
			put("delta", '\u03B4');
			put("epsilon", '\u03B5');
			put("zeta", '\u03B6');
			put("eta", '\u03B7');
			put("theta", '\u03B8');
			put("iota", '\u03B9');
			put("kappa", '\u03BA');
			put("lambda", '\u03BB');
			put("mu", '\u03BC');
			put("nu", '\u03BD');
			put("xi", '\u03BE');
			put("omicron", '\u03BF');
			put("pi", '\u03C0');
			put("rho", '\u03C1');
			put("sigmaf", '\u03C2');
			put("sigma", '\u03C3');
			put("tau", '\u03C4');
			put("upsilon", '\u03C5');
			put("phi", '\u03C6');
			put("chi", '\u03C7');
			put("psi", '\u03C8');
			put("omega", '\u03C9');
			put("thetasym", '\u03D1');
			put("upsih", '\u03D2');
			put("piv", '\u03D6');
			put("bull", '\u2022');
			put("hellip", '\u2026');
			put("prime", '\u2032');
			put("Prime", '\u2033');
			put("oline", '\u203E');
			put("frasl", '\u2044');
			put("weierp", '\u2118');
			put("image", '\u2111');
			put("real", '\u211C');
			put("trade", '\u2122');
			put("alefsym", '\u2135');
			put("larr", '\u2190');
			put("uarr", '\u2191');
			put("rarr", '\u2192');
			put("darr", '\u2193');
			put("harr", '\u2194');
			put("crarr", '\u21B5');
			put("lArr", '\u21D0');
			put("uArr", '\u21D1');
			put("rArr", '\u21D2');
			put("dArr", '\u21D3');
			put("hArr", '\u21D4');
			put("forall", '\u2200');
			put("part", '\u2202');
			put("exist", '\u2203');
			put("empty", '\u2205');
			put("nabla", '\u2207');
			put("isin", '\u2208');
			put("notin", '\u2209');
			put("ni", '\u220B');
			put("prod", '\u220F');
			put("sum", '\u2211');
			put("minus", '\u2212');
			put("lowast", '\u2217');
			put("radic", '\u221A');
			put("prop", '\u221D');
			put("infin", '\u221E');
			put("ang", '\u2220');
			put("and", '\u2227');
			put("or", '\u2228');
			put("cap", '\u2229');
			put("cup", '\u222A');
			put("int", '\u222B');
			put("there4", '\u2234');
			put("sim", '\u223C');
			put("cong", '\u2245');
			put("asymp", '\u2248');
			put("ne", '\u2260');
			put("equiv", '\u2261');
			put("le", '\u2264');
			put("ge", '\u2265');
			put("sub", '\u2282');
			put("sup", '\u2283');
			put("nsub", '\u2284');
			put("sube", '\u2286');
			put("supe", '\u2287');
			put("oplus", '\u2295');
			put("otimes", '\u2297');
			put("perp", '\u22A5');
			put("sdot", '\u22C5');
			put("lceil", '\u2308');
			put("rceil", '\u2309');
			put("lfloor", '\u230A');
			put("rfloor", '\u230B');
			put("lang", '\u2329');
			put("rang", '\u232A');
			put("loz", '\u25CA');
			put("spades", '\u2660');
			put("clubs", '\u2663');
			put("hearts", '\u2665');
			put("diams", '\u2666');
			put("quot", '\u0022');
			put("amp", '\u0026');
			put("lt", '\u003C');
			put("gt", '\u003E');
			put("OElig", '\u0152');
			put("oelig", '\u0153');
			put("Scaron", '\u0160');
			put("scaron", '\u0161');
			put("Yuml", '\u0178');
			put("circ", '\u02C6');
			put("tilde", '\u02DC');
			put("ensp", '\u2002');
			put("emsp", '\u2003');
			put("thinsp", '\u2009');
			put("zwnj", '\u200C');
			put("zwj", '\u200D');
			put("lrm", '\u200E');
			put("rlm", '\u200F');
			put("ndash", '\u2013');
			put("mdash", '\u2014');
			put("lsquo", '\u2018');
			put("rsquo", '\u2019');
			put("sbquo", '\u201A');
			put("ldquo", '\u201C');
			put("rdquo", '\u201D');
			put("bdquo", '\u201E');
			put("dagger", '\u2020');
			put("Dagger", '\u2021');
			put("permil", '\u2030');
			put("lsaquo", '\u2039');
			put("rsaquo", '\u203A');
			put("euro", '\u20AC');
		}
	};
	
	/** Map of characters to entity names. */
	public static final HashMap<Character, String> UNICODE_ENTITY_MAP = new HashMap<Character, String>()
	{
		private static final long serialVersionUID = -8209253780919474200L;
		{
			put('\u00f6',"ouml");
			put('\u03a3',"Sigma");
			put('\u00b0',"deg");
			put('\u00f4',"ocirc");
			put('\u0392',"Beta");
			put('\u00e5',"aring");
			put('\u2660',"spades");
			put('\u03b9',"iota");
			put('\u039b',"Lambda");
			put('\u03a0',"Pi");
			put('\u201a',"sbquo");
			put('\u00d6',"Ouml");
			put('\u03c3',"sigma");
			put('\u2286',"sube");
			put('\u232a',"rang");
			put('\u2044',"frasl");
			put('\u0399',"Iota");
			put('\u00d1',"Ntilde");
			put('\u2261',"equiv");
			put('\u03bb',"lambda");
			put('\u00bb',"raquo");
			put('\u222a',"cup");
			put('\u00fc',"uuml");
			put('\u2282',"sub");
			put('\u00a7',"sect");
			put('\u00b4',"acute");
			put('\u00a1',"iexcl");
			put('\u2211',"sum");
			put('\u2283',"sup");
			put('\u00eb',"euml");
			put('\u201c',"ldquo");
			put('\u21d0',"lArr");
			put('\u00f1',"ntilde");
			put('\u03d2',"upsih");
			put('\u00dc',"Uuml");
			put('\u2033',"Prime");
			put('\u00c0',"Agrave");
			put('\u00cd',"Iacute");
			put('\u00e3',"atilde");
			put('\u2248',"asymp");
			put('\u00cb',"Euml");
			put('\u2212',"minus");
			put('\u00dd',"Yacute");
			put('\u2190',"larr");
			put('\u2020',"dagger");
			put('\u2309',"rceil");
			put('\u2663',"clubs");
			put('\u0398',"Theta");
			put('\u2032',"prime");
			put('\u00c7',"Ccedil");
			put('\u03a7',"Chi");
			put('\u00ed',"iacute");
			put('\u03c5',"upsilon");
			put('\u2295',"oplus");
			put('\u00a3',"pound");
			put('\u21d2',"rArr");
			put('\u00ae',"reg");
			put('\u00fd',"yacute");
			put('\u03b5',"epsilon");
			put('\u03b8',"theta");
			put('\u0192',"fnof");
			put('\u00c1',"Aacute");
			put('\u00cc',"Igrave");
			put('\u03c7',"chi");
			put('\u03a8',"Psi");
			put('\u03bf',"omicron");
			put('\u00d3',"Oacute");
			put('\u2192',"rarr");
			put('\u00df',"szlig");
			put('\u00ad',"shy");
			put('\u00b5',"micro");
			put('\u00ec',"igrave");
			put('\u2207',"nabla");
			put('\u2135',"alefsym");
			put('\u03c8',"psi");
			put('\u221e',"infin");
			put('\u2002',"ensp");
			put('\u00f3',"oacute");
			put('\u200d',"zwj");
			put('\u00ac',"not");
			put('\u00e4',"auml");
			put('\u00a8',"uml");
			put('\u223c',"sim");
			put('\u00c2',"Acirc");
			put('\u03b6',"zeta");
			put('\u03a1',"Rho");
			put('\u21d4',"hArr");
			put('\u00ca',"Ecirc");
			put('\u2026',"hellip");
			put('\u2265',"ge");
			put('\u00c8',"Egrave");
			put('\u00ce',"Icirc");
			put('\u230a',"lfloor");
			put('\u00a5',"yen");
			put('\u203e',"oline");
			put('\u00b8',"cedil");
			put('\u00d2',"Ograve");
			put('\u2019',"rsquo");
			put('\u00db',"Ucirc");
			put('\u003e',"gt");
			put('\u20ac',"euro");
			put('\u00da',"Uacute");
			put('\u03be',"xi");
			put('\u00c4',"Auml");
			put('\u22a5',"perp");
			put('\u0152',"OElig");
			put('\u0153',"oelig");
			put('\u2203',"exist");
			put('\u00e2',"acirc");
			put('\u0396',"Zeta");
			put('\u03c1',"rho");
			put('\u2194',"harr");
			put('\u00ea',"ecirc");
			put('\u00ee',"icirc");
			put('\u2013',"ndash");
			put('\u00e9',"eacute");
			put('\u00af',"macr");
			put('\u2003',"emsp");
			put('\u0022',"quot");
			put('\u00f2',"ograve");
			put('\u00fb',"ucirc");
			put('\u00a0',"nbsp");
			put('\u00fa',"uacute");
			put('\u039e',"Xi");
			put('\u2284',"nsub");
			put('\u2022',"bull");
			put('\u2665',"hearts");
			put('\u02c6',"circ");
			put('\u00c9',"Eacute");
			put('\u03c2',"sigmaf");
			put('\u2666',"diams");
			put('\u2245',"cong");
			put('\u230b',"rfloor");
			put('\u039a',"Kappa");
			put('\u2039',"lsaquo");
			put('\u00d9',"Ugrave");
			put('\u00be',"frac34");
			put('\u00ab',"laquo");
			put('\u2209',"notin");
			put('\u039f',"Omicron");
			put('\u0391',"Alpha");
			put('\u00e8',"egrave");
			put('\u03a9',"Omega");
			put('\u00b1',"plusmn");
			put('\u00d7',"times");
			put('\u201e',"bdquo");
			put('\u0395',"Epsilon");
			put('\u2009',"thinsp");
			put('\u03ba',"kappa");
			put('\u2205',"empty");
			put('\u00f9',"ugrave");
			put('\u03a6',"Phi");
			put('\u2217',"lowast");
			put('\u220f',"prod");
			put('\u25ca',"loz");
			put('\u02dc',"tilde");
			put('\u201d',"rdquo");
			put('\u03a5',"Upsilon");
			put('\u2111',"image");
			put('\u21d3',"dArr");
			put('\u03b1',"alpha");
			put('\u03c9',"omega");
			put('\u221d',"prop");
			put('\u2122',"trade");
			put('\u03d1',"thetasym");
			put('\u00bc',"frac14");
			put('\u00bd',"frac12");
			put('\u03c6',"phi");
			put('\u2308',"lceil");
			put('\u00d8',"Oslash");
			put('\u0397',"Eta");
			put('\u200f',"rlm");
			put('\u00a9',"copy");
			put('\u203a',"rsaquo");
			put('\u00d0',"ETH");
			put('\u2264',"le");
			put('\u00e1',"aacute");
			put('\u2193',"darr");
			put('\u2014',"mdash");
			put('\u00f7',"divide");
			put('\u0393',"Gamma");
			put('\u03a4',"Tau");
			put('\u003c',"lt");
			put('\u00f8',"oslash");
			put('\u03b7',"eta");
			put('\u21b5',"crarr");
			put('\u00d5',"Otilde");
			put('\u0394',"Delta");
			put('\u00f0',"eth");
			put('\u03d6',"piv");
			put('\u00e7',"ccedil");
			put('\u211c',"real");
			put('\u00b9',"sup1");
			put('\u00b2',"sup2");
			put('\u00a4',"curren");
			put('\u00aa',"ordf");
			put('\u00b3',"sup3");
			put('\u03b3',"gamma");
			put('\u200e',"lrm");
			put('\u00a2',"cent");
			put('\u03bc',"mu");
			put('\u03c4',"tau");
			put('\u2021',"Dagger");
			put('\u00ba',"ordm");
			put('\u21d1',"uArr");
			put('\u2234',"there4");
			put('\u00c3',"Atilde");
			put('\u00f5',"otilde");
			put('\u2260',"ne");
			put('\u03b4',"delta");
			put('\u200c',"zwnj");
			put('\u00ff',"yuml");
			put('\u220b',"ni");
			put('\u00e0',"agrave");
			put('\u0160',"Scaron");
			put('\u0026',"amp");
			put('\u03bd',"nu");
			put('\u039c',"Mu");
			put('\u2200',"forall");
			put('\u2297',"otimes");
			put('\u00ef',"iuml");
			put('\u2191',"uarr");
			put('\u2208',"isin");
			put('\u2229',"cap");
			put('\u00a6',"brvbar");
			put('\u00de',"THORN");
			put('\u2227',"and");
			put('\u2287',"supe");
			put('\u2118',"weierp");
			put('\u2220',"ang");
			put('\u0178',"Yuml");
			put('\u00b6',"para");
			put('\u0161',"scaron");
			put('\u2228',"or");
			put('\u2018',"lsquo");
			put('\u00c6',"AElig");
			put('\u00d4',"Ocirc");
			put('\u00e6',"aelig");
			put('\u00bf',"iquest");
			put('\u039d',"Nu");
			put('\u00cf',"Iuml");
			put('\u2030',"permil");
			put('\u2329',"lang");
			put('\u221a',"radic");
			put('\u222b',"int");
			put('\u03b2',"beta");
			put('\u00c5',"Aring");
			put('\u2202',"part");
			put('\u22c5',"sdot");
			put('\u00fe',"thorn");
			put('\u03c0',"pi");
			put('\u00b7',"middot");
		}
	};
	
	/** Lag simulator seed. */
	private static Random randomLagSimulator = new Random();

	private SmallUtil() {}
	
	/**
	 * Checks if a MIME-Type is JSON-formatted.
	 * @param type the content type.
	 * @return true if so, false if not.
	 */
	public static boolean isJSON(String type)
	{
		return type != null && type.startsWith("application/json");
	}

	/**
	 * Checks if a MIME-Type is XML-formatted.
	 * @param type the content type.
	 * @return true if so, false if not.
	 */
	public static boolean isXML(String type)
	{
		return type != null && (
			type.startsWith("application/xml")
			|| type.startsWith("text/xml")
		);
	}

	/**
	 * Converts a String to an HTML-safe string.
	 * @param input the input string to convert.
	 * @return the converted string.
	 */
	public static String convertToHTMLEntities(String input)
	{
		StringBuilder sb = new StringBuilder();
		char[] chars = input.toCharArray();
		
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			
			if (c < 0x0020 || c >= 0x007f)
				sb.append(String.format("&#x%04x;", c));
			else if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '"')
				sb.append("&quot;");
			else if (c == '\'')
				sb.append("&apos;");
			else
				sb.append(c);
		}
		
		return sb.toString();
	}
	
	/**
	 * Converts a String with HTML entities in it to one without.
	 * @param input the input string to convert.
	 * @return the converted string.
	 */
	public static String convertFromHTMLEntities(String input)
	{
		StringBuilder sb = new StringBuilder();
		StringBuilder entity = new StringBuilder();
		char[] chars = input.toCharArray();
		
		final int STATE_STRING = 0;
		final int STATE_ENTITY = 1;
		int state = STATE_STRING;
		
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			
			switch (state)
			{
				case STATE_STRING:
					if (c == '&')
					{
						entity.delete(0, entity.length());
						state = STATE_ENTITY;
					}
					else
						sb.append(c);
					break;
				case STATE_ENTITY:
					if (c == ';')
					{
						String e = entity.toString();
						if (e.startsWith("#x"))
						{
							int n = Integer.parseInt(e.substring(2), 16);
							sb.append((char)n);
						}
						else if (e.startsWith("#"))
						{
							int n = Integer.parseInt(e.substring(1), 10);
							sb.append((char)n);
						}
						else if (ENTITY_NAME_MAP.containsKey(e))
							sb.append(ENTITY_NAME_MAP.get(e));
						else
							sb.append(e);
						
						state = STATE_STRING;
					}
					else
						entity.append(c);
					break;
			}
		}
		
		if (state == STATE_ENTITY)
			sb.append('&').append(entity.toString());
		
		return sb.toString();
	}

	/**
	 * Trims slashes from the ends.
	 * @param str the input string.
	 * @return the input string with slashes removed from both ends.
	 * @see #removeBeginningSlash(String)
	 * @see #removeEndingSlash(String)
	 */
	public static String trimSlashes(String str)
	{
		return removeBeginningSlash(removeEndingSlash(str));
	}
	
	/**
	 * Convenience for <code>addBeginningSlash(removeEndingSlash(str))</code>.
	 * @param str the input string.
	 * @return the resulting string with a beginning slash and no ending slash.
	 */
	public static String pathify(String str)
	{
		return removeEndingSlash(addBeginningSlash(str));
	}
	
	/**
	 * Adds a beginning slash to the string, if no beginning slash exists.
	 * @param str the string.
	 * @return the resulting string with a beginning slash.
	 */
	public static String addBeginningSlash(String str)
	{
		if (Utils.isEmpty(str))
			return "/";
		
		return str.charAt(0) == '/' ? str : '/' + str;
	}
	
	/**
	 * Removes the beginning slashes, if any, from a string.
	 * @param str the string.
	 * @return the resulting string without a beginning slash.
	 */
	public static String removeBeginningSlash(String str)
	{
		if (Utils.isEmpty(str))
			return str;
		
		int i = 0;
		while (i < str.length() && str.charAt(i) == '/')
			i++;
		return i > 0 ? str.substring(i) : str;
	}
	
	/**
	 * Removes the ending slashes, if any, from a string.
	 * @param str the string.
	 * @return the resulting string without an ending slash.
	 */
	public static String removeEndingSlash(String str)
	{
		if (Utils.isEmpty(str))
			return str;
		
		int i = str.length();
		while (i > 0 && str.charAt(i - 1) == '/')
			i--;
		return i > 0 ? str.substring(0, i) : str;
	}
	
	/**
	 * Removes the path extension, or rather, the part after the last dot in a path.
	 * @param path the path string.
	 * @return the resulting string without an extension.
	 */
	public static String removeExtension(String path)
	{
		if (Utils.isEmpty(path))
			return path;
		
		int i = path.length();
		while (i > 0 && path.charAt(i - 1) != '.' && path.charAt(i - 1) != '/')
			i--;
		
		if (i == 0)
			return path;
		else if (path.charAt(i - 1) == '.')
			return path.substring(0, i - 1);
		else
			return path;
	}
	
	
	/**
	 * Convenience method that calls <code>session.getAttribute(attribName)</code>. 
	 * @param session the session.
	 * @param attribName the attribute name.
	 * @return true if it exists, false otherwise.
	 */
	public static boolean getAttributeExist(HttpSession session, String attribName)
	{
		return session.getAttribute(attribName) != null;
	}

	/**
	 * Convenience method that gets the Small Application Environment.
	 * @param context the servlet context.
	 * @return the Small environment.
	 */
	public static SmallEnvironment getEnvironment(ServletContext context)
	{
		return (SmallEnvironment)context.getAttribute(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ATTRIBUTE);
	}

	/**
	 * Convenience method that gets the Small Configuration stub used to start this application.
	 * @param context the servlet context.
	 * @return the Small environment.
	 */
	public static SmallConfiguration getConfiguration(ServletContext context)
	{
		return (SmallConfiguration)context.getAttribute(SmallConstants.SMALL_APPLICATION_CONFIGURATION_ATTRIBUTE);
	}

	/**
	 * Gets the MIME type of a file (uses the provided {@link MIMETypeDriver}).
	 * @param context the servlet context.
	 * @param filename the file name to use to figure out a MIME type.
	 * @return the MIME type, or <code>application/octet-stream</code>.
	 */
	public static String getMIMEType(ServletContext context, String filename)
	{
		return getEnvironment(context).getMIMETypeDriver().getMIMEType(Utils.getFileExtension(filename));
	}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param <T> the object type.
	 * @return a typecast object on the application scope.
	 * @throws IllegalArgumentException if the class provided in an anonymous class or array without a component type.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz)
	{
		String className = clazz.getCanonicalName(); 
		if ((className = clazz.getCanonicalName()) == null)
			throw new IllegalArgumentException("Class provided has no type!");
		return getApplicationBean(context, clazz, "$$"+className, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param <T> the object type.
	 * @return a typecast object on the application scope.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz, String name)
	{
		return getApplicationBean(context, clazz, name, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the application's servlet context (via {@link Class#newInstance()}) if it doesn't exist.
	 * @param <T> the object type.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz, String name, boolean create)
	{
		Object obj = context.getAttribute(name);
		if (obj == null && create)
		{
			try {
				synchronized (context)
				{
					if ((obj = context.getAttribute(name)) == null)
					{
						obj = clazz.getDeclaredConstructor().newInstance();
						context.setAttribute(name, obj);
					}
				}
			} catch (Exception e) {
				throw new SmallFrameworkException(e);
			}
		}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
	}

	/**
	 * Gets a file that is on the application path. 
	 * @param context the servlet context to use. 
	 * @param relativePath the path to the file to get via a path relative to the application root.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public static File getApplicationFile(ServletContext context, String relativePath)
	{
		File inFile = new File(getApplicationFilePath(context, relativePath));
		return inFile.exists() ? inFile : null;
	}

	/**
	 * Attempts to fetch the Websocket Server Container from a servlet context.
	 * @param context the servlet context to use. 
	 * @return the server container, or null if it wasn't found, usually because it wasn't configured or it is unsupported.
	 */
	public static ServerContainer getWebsocketServerContainer(ServletContext context)
	{
		return (ServerContainer)context.getAttribute("javax.websocket.server.ServerContainer");
	}
	
	/**
	 * Gets a file path that is on the application path.
	 * @param context the servlet context to use. 
	 * @param relativePath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public static String getApplicationFilePath(ServletContext context, String relativePath)
	{
		return context.getRealPath("/") + "/" + SmallUtil.removeBeginningSlash(relativePath);
	}

	/**
	 * Opens an input stream to a resource using a path relative to the application context path. 
	 * Outside users should not be able to access this!
	 * @param context the servlet context to use. 
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 * @throws IOException if the stream cannot be opened.
	 */
	public static InputStream getApplicationFileAsStream(ServletContext context, String path) throws IOException
	{
		File inFile = getApplicationFile(context, path);
		return inFile != null ? new FileInputStream(inFile) : null;
	}

	/**
	 * Convenience method that calls <code>context.getAttribute(attribName)</code>.
	 * @param context the servlet context.
	 * @param attribName the attribute name.
	 * @return true if it exists, false otherwise.
	 */
	public static boolean getAttributeExist(ServletContext context, String attribName)
	{
		return context.getAttribute(attribName) != null;
	}

	/**
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public static void includeView(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).include(request, response);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Surreptitiously forwards the request to a view.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public static void sendToView(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Pauses the current thread for up to <code>maxMillis</code>
	 * milliseconds, used for simulating lag.
	 * For debugging and testing only!
	 * @param maxMillis the maximum amount of milliseconds to wait.
	 */
	public static void simulateLag(long maxMillis)
	{
		long millis = randomLagSimulator.nextLong() % (maxMillis <= 1 ? 1 : maxMillis);
		try {Thread.sleep(millis);} catch (InterruptedException e) {}
	}

}
