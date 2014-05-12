package org.openseamap.openseamapviewer.filefilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public final class ValidRouteGmlFile implements ValidFileFilter {
	private FileOpenResult fileOpenResult;
	@Override
	public boolean accept(File file) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			/*RenderThemeHandler renderThemeHandler = new RenderThemeHandler();
			XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			xmlReader.setContentHandler(renderThemeHandler);
			xmlReader.parse(new InputSource(inputStream));*/
			this.fileOpenResult = FileOpenResult.SUCCESS;
		/*} catch (ParserConfigurationException e) {
			this.fileOpenResult = new FileOpenResult(e.getMessage());
		} catch (SAXException e) {
			this.fileOpenResult = new FileOpenResult(e.getMessage());*/
		} catch (IOException e) {
			this.fileOpenResult = new FileOpenResult(e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				this.fileOpenResult = new FileOpenResult(e.getMessage());
			}
		}
		return this.fileOpenResult.isSuccess();
	}

	@Override
	public FileOpenResult getFileOpenResult() {
		return this.fileOpenResult;
	}
}
