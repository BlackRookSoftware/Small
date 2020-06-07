package com.blackrook.small.roles;

import java.util.HashMap;
import java.util.Objects;

/**
 * Default MIME-Type driver.
 * @author Matthew Tropiano
 */
public class DefaultMIMETypeDriver implements MIMETypeDriver
{
	/** Internal map. */
	private HashMap<String, String> mapping;
	
	public DefaultMIMETypeDriver()
	{
		this.mapping = new HashMap<>(240, 1f);
		setMIMEType("323", "text/h323");
		setMIMEType("3g2", "video/3gpp2");
		setMIMEType("3gp", "video/3gpp");
		setMIMEType("7z", "application/x-7z-compressed");
		setMIMEType("aac", "audio/aac");
		setMIMEType("abw", "application/x-abiword");
		setMIMEType("acx", "application/internet-property-stream");
		setMIMEType("ai", "application/postscript");
		setMIMEType("aif", "audio/x-aiff");
		setMIMEType("aifc", "audio/x-aiff");
		setMIMEType("aiff", "audio/x-aiff");
		setMIMEType("arc", "application/x-freearc");
		setMIMEType("asf", "video/x-ms-asf");
		setMIMEType("asr", "video/x-ms-asf");
		setMIMEType("asx", "video/x-ms-asf");
		setMIMEType("au", "audio/basic");
		setMIMEType("avi", "video/x-msvideo");
		setMIMEType("axs", "application/olescript");
		setMIMEType("azw", "application/vnd.amazon.ebook");
		setMIMEType("bas", "text/plain");
		setMIMEType("bcpio", "application/x-bcpio");
		setMIMEType("bin", "application/octet-stream");
		setMIMEType("bmp", "image/bmp");
		setMIMEType("bz", "application/x-bzip");
		setMIMEType("bz2", "application/x-bzip2");
		setMIMEType("c", "text/plain");
		setMIMEType("cat", "application/vnd.ms-pkiseccat");
		setMIMEType("cdf", "application/x-cdf");
		setMIMEType("cdf", "application/x-netcdf");
		setMIMEType("cer", "application/x-x509-ca-cert");
		setMIMEType("class", "application/octet-stream");
		setMIMEType("clp", "application/x-msclip");
		setMIMEType("cmx", "image/x-cmx");
		setMIMEType("cod", "image/cis-cod");
		setMIMEType("cpio", "application/x-cpio");
		setMIMEType("crd", "application/x-mscardfile");
		setMIMEType("crl", "application/pkix-crl");
		setMIMEType("crt", "application/x-x509-ca-cert");
		setMIMEType("csh", "application/x-csh");
		setMIMEType("css", "text/css");
		setMIMEType("csv", "text/csv");
		setMIMEType("dcr", "application/x-director");
		setMIMEType("der", "application/x-x509-ca-cert");
		setMIMEType("dir", "application/x-director");
		setMIMEType("dll", "application/x-msdownload");
		setMIMEType("dms", "application/octet-stream");
		setMIMEType("doc", "application/msword");
		setMIMEType("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		setMIMEType("dot", "application/msword");
		setMIMEType("dvi", "application/x-dvi");
		setMIMEType("dxr", "application/x-director");
		setMIMEType("eot", "application/vnd.ms-fontobject");
		setMIMEType("eps", "application/postscript");
		setMIMEType("epub", "application/epub+zip");
		setMIMEType("etx", "text/x-setext");
		setMIMEType("evy", "application/envoy");
		setMIMEType("exe", "application/octet-stream");
		setMIMEType("fif", "application/fractals");
		setMIMEType("flr", "x-world/x-vrml");
		setMIMEType("gif", "image/gif");
		setMIMEType("gtar", "application/x-gtar");
		setMIMEType("gz", "application/x-gzip");
		setMIMEType("h", "text/plain");
		setMIMEType("hdf", "application/x-hdf");
		setMIMEType("hlp", "application/winhlp");
		setMIMEType("hqx", "application/mac-binhex40");
		setMIMEType("hta", "application/hta");
		setMIMEType("htc", "text/x-component");
		setMIMEType("htm", "text/html");
		setMIMEType("html", "text/html");
		setMIMEType("htt", "text/webviewhtml");
		setMIMEType("ico", "image/x-icon");
		setMIMEType("ief", "image/ief");
		setMIMEType("iii", "application/x-iphone");
		setMIMEType("ins", "application/x-internet-signup");
		setMIMEType("isp", "application/x-internet-signup");
		setMIMEType("jfif", "image/pipeg");
		setMIMEType("jpe", "image/jpeg");
		setMIMEType("jpeg", "image/jpeg");
		setMIMEType("jpg", "image/jpeg");
		setMIMEType("js", "application/x-javascript");
		setMIMEType("json", "application/json");
		setMIMEType("jsonld", "application/ld+json");
		setMIMEType("latex", "application/x-latex");
		setMIMEType("lha", "application/octet-stream");
		setMIMEType("lsf", "video/x-la-asf");
		setMIMEType("lsx", "video/x-la-asf");
		setMIMEType("lzh", "application/octet-stream");
		setMIMEType("m13", "application/x-msmediaview");
		setMIMEType("m14", "application/x-msmediaview");
		setMIMEType("m3u", "audio/x-mpegurl");
		setMIMEType("man", "application/x-troff-man");
		setMIMEType("mdb", "application/x-msaccess");
		setMIMEType("me", "application/x-troff-me");
		setMIMEType("mht", "message/rfc822");
		setMIMEType("mhtml", "message/rfc822");
		setMIMEType("mid", "audio/midi");
		setMIMEType("midi", "audio/midi");
		setMIMEType("mjs", "text/javascript");
		setMIMEType("mny", "application/x-msmoney");
		setMIMEType("mov", "video/quicktime");
		setMIMEType("movie", "video/x-sgi-movie");
		setMIMEType("mp2", "video/mpeg");
		setMIMEType("mp3", "audio/mpeg");
		setMIMEType("mpa", "video/mpeg");
		setMIMEType("mpe", "video/mpeg");
		setMIMEType("mpeg", "video/mpeg");
		setMIMEType("mpg", "video/mpeg");
		setMIMEType("mpkg", "application/vnd.apple.installer+xml");
		setMIMEType("mpp", "application/vnd.ms-project");
		setMIMEType("mpv2", "video/mpeg");
		setMIMEType("ms", "application/x-troff-ms");
		setMIMEType("msg", "application/vnd.ms-outlook");
		setMIMEType("mvb", "application/x-msmediaview");
		setMIMEType("nc", "application/x-netcdf");
		setMIMEType("nws", "message/rfc822");
		setMIMEType("oda", "application/oda");
		setMIMEType("odp", "application/vnd.oasis.opendocument.presentation");
		setMIMEType("ods", "application/vnd.oasis.opendocument.spreadsheet");
		setMIMEType("odt", "application/vnd.oasis.opendocument.text");
		setMIMEType("oga", "audio/ogg");
		setMIMEType("ogg", "application/ogg");
		setMIMEType("ogv", "video/ogg");
		setMIMEType("ogx", "application/ogg");
		setMIMEType("opus", "audio/opus");
		setMIMEType("otf", "font/otf");
		setMIMEType("p10", "application/pkcs10");
		setMIMEType("p12", "application/x-pkcs12");
		setMIMEType("p7b", "application/x-pkcs7-certificates");
		setMIMEType("p7c", "application/x-pkcs7-mime");
		setMIMEType("p7m", "application/x-pkcs7-mime");
		setMIMEType("p7r", "application/x-pkcs7-certreqresp");
		setMIMEType("p7s", "application/x-pkcs7-signature");
		setMIMEType("pbm", "image/x-portable-bitmap");
		setMIMEType("pdf", "application/pdf");
		setMIMEType("pfx", "application/x-pkcs12");
		setMIMEType("pgm", "image/x-portable-graymap");
		setMIMEType("php", "application/x-httpd-php");
		setMIMEType("pk3", "application/zip");
		setMIMEType("pk7", "application/x-7z-compressed");
		setMIMEType("pko", "application/ynd.ms-pkipko");
		setMIMEType("pma", "application/x-perfmon");
		setMIMEType("pmc", "application/x-perfmon");
		setMIMEType("pml", "application/x-perfmon");
		setMIMEType("pmr", "application/x-perfmon");
		setMIMEType("pmw", "application/x-perfmon");
		setMIMEType("png", "image/png");
		setMIMEType("pnm", "image/x-portable-anymap");
		setMIMEType("pot", "application/vnd.ms-powerpoint");
		setMIMEType("ppm", "image/x-portable-pixmap");
		setMIMEType("pps", "application/vnd.ms-powerpoint");
		setMIMEType("ppt", "application/vnd.ms-powerpoint");
		setMIMEType("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		setMIMEType("prf", "application/pics-rules");
		setMIMEType("ps", "application/postscript");
		setMIMEType("pub", "application/x-mspublisher");
		setMIMEType("qt", "video/quicktime");
		setMIMEType("ra", "audio/x-pn-realaudio");
		setMIMEType("rar", "application/vnd.rar");
		setMIMEType("ram", "audio/x-pn-realaudio");
		setMIMEType("ras", "image/x-cmu-raster");
		setMIMEType("rgb", "image/x-rgb");
		setMIMEType("rmi", "audio/mid");
		setMIMEType("roff", "application/x-troff");
		setMIMEType("rtf", "application/rtf");
		setMIMEType("rtx", "text/richtext");
		setMIMEType("scd", "application/x-msschedule");
		setMIMEType("sct", "text/scriptlet");
		setMIMEType("setpay", "application/set-payment-initiation");
		setMIMEType("setreg", "application/set-registration-initiation");
		setMIMEType("sh", "application/x-sh");
		setMIMEType("shar", "application/x-shar");
		setMIMEType("sit", "application/x-stuffit");
		setMIMEType("snd", "audio/basic");
		setMIMEType("spc", "application/x-pkcs7-certificates");
		setMIMEType("spl", "application/futuresplash");
		setMIMEType("src", "application/x-wais-source");
		setMIMEType("sst", "application/vnd.ms-pkicertstore");
		setMIMEType("stl", "application/vnd.ms-pkistl");
		setMIMEType("stm", "text/html");
		setMIMEType("sv4cpio", "application/x-sv4cpio");
		setMIMEType("sv4crc", "application/x-sv4crc");
		setMIMEType("svg", "image/svg+xml");
		setMIMEType("swf", "application/x-shockwave-flash");
		setMIMEType("t", "application/x-troff");
		setMIMEType("tar", "application/x-tar");
		setMIMEType("tcl", "application/x-tcl");
		setMIMEType("tex", "application/x-tex");
		setMIMEType("texi", "application/x-texinfo");
		setMIMEType("texinfo", "application/x-texinfo");
		setMIMEType("tgz", "application/x-compressed");
		setMIMEType("tif", "image/tiff");
		setMIMEType("tiff", "image/tiff");
		setMIMEType("tr", "application/x-troff");
		setMIMEType("trm", "application/x-msterminal");
		setMIMEType("ts", "video/mp2t");
		setMIMEType("tsv", "text/tab-separated-values");
		setMIMEType("ttf", "font/ttf");
		setMIMEType("txt", "text/plain");
		setMIMEType("uls", "text/iuls");
		setMIMEType("ustar", "application/x-ustar");
		setMIMEType("vcf", "text/x-vcard");
		setMIMEType("vrml", "x-world/x-vrml");
		setMIMEType("vsd", "application/vnd.visio");
		setMIMEType("wad", "application/x-doom");
		setMIMEType("wav", "audio/x-wav");
		setMIMEType("wcm", "application/vnd.ms-works");
		setMIMEType("wdb", "application/vnd.ms-works");
		setMIMEType("weba", "audio/webm");
		setMIMEType("webm", "video/webm");
		setMIMEType("webp", "image/webp");
		setMIMEType("woff", "font/woff");
		setMIMEType("woff2", "font/woff2");
		setMIMEType("wks", "application/vnd.ms-works");
		setMIMEType("wmf", "application/x-msmetafile");
		setMIMEType("wps", "application/vnd.ms-works");
		setMIMEType("wri", "application/x-mswrite");
		setMIMEType("wrl", "x-world/x-vrml");
		setMIMEType("wrz", "x-world/x-vrml");
		setMIMEType("xaf", "x-world/x-vrml");
		setMIMEType("xbm", "image/x-xbitmap");
		setMIMEType("xhtml", "application/xhtml+xml");
		setMIMEType("xla", "application/vnd.ms-excel");
		setMIMEType("xlc", "application/vnd.ms-excel");
		setMIMEType("xlm", "application/vnd.ms-excel");
		setMIMEType("xls", "application/vnd.ms-excel");
		setMIMEType("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		setMIMEType("xlt", "application/vnd.ms-excel");
		setMIMEType("xlw", "application/vnd.ms-excel");
		setMIMEType("xml", "application/xml");
		setMIMEType("xof", "x-world/x-vrml");
		setMIMEType("xpm", "image/x-xpixmap");
		setMIMEType("xul", "application/vnd.mozilla.xul+xml");
		setMIMEType("xwd", "image/x-xwindowdump");
		setMIMEType("z", "application/x-compress");
		setMIMEType("zip", "application/zip");
	}
	
	/**
	 * Sets the corresponding type of an extension.
	 * @param extension the file extension.
	 * @param mimeType the corresponding type.
	 * @throws NullPointerException if extension or mimeType is null.
	 */
	public void setMIMEType(String extension, String mimeType)
	{
		mapping.put(Objects.requireNonNull(extension), Objects.requireNonNull(mimeType));
	}
	
	@Override
	public String getMIMEType(String extension)
	{
		return extension == null ? null : mapping.get(extension.toLowerCase());
	}

}
