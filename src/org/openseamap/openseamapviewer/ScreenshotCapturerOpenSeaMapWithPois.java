package org.openseamap.openseamapviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.mapsforge.android.maps.PausableThread;
import org.mapsforge.android.maps.overlay.Overlay;
import org.openseamap.openseamapviewer.viewer.OpenSeamapViewerWithPois;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Environment;

public class ScreenshotCapturerOpenSeaMapWithPois extends PausableThread {

	private static final String SCREENSHOT_DIRECTORY = "Pictures";
	private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
	private static final int SCREENSHOT_QUALITY = 90;
	private static final String THREAD_NAME = "ScreenshotCapturer";

	private final OpenSeamapViewerWithPois mMapViewer;
	private CompressFormat compressFormat;
    private int screenshotNumber = 0;
    private Bitmap mBitmap = null;
    private int width = 0;
    private int height = 0;
    private Canvas mCanvas = null;
    
	public ScreenshotCapturerOpenSeaMapWithPois(OpenSeamapViewerWithPois aMapViewer) {
		this.mMapViewer = aMapViewer;
		
	}
	
	/**
	 * 
	 * @param pattern  use a pattern like "000.00"
	 * @param value    the value to convert  45.34523
	 * @return  aString with the value formatted  045.34
	 */
	private String customFormat(String pattern, double value ) {
	      DecimalFormat myFormatter = new DecimalFormat(pattern);
	      String output = myFormatter.format(value);
	      return output;
	  }

	private File assembleFilePath(File directory) {
		StringBuilder strinBuilder = new StringBuilder();
		strinBuilder.append(directory.getAbsolutePath());
		strinBuilder.append(File.separatorChar);
		strinBuilder.append(SCREENSHOT_FILE_NAME);
		strinBuilder.append(customFormat("000",screenshotNumber));
		strinBuilder.append('.');
		strinBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
		return new File(strinBuilder.toString());
	}
	
	private File assembleFileTempPath(File directory) {
		StringBuilder strinBuilder = new StringBuilder();
		strinBuilder.append(directory.getAbsolutePath());
		strinBuilder.append(File.separatorChar);
		strinBuilder.append(SCREENSHOT_FILE_NAME);
		strinBuilder.append('.');
		strinBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
		return new File(strinBuilder.toString());
	}

	@Override
	protected void doWork() {
		try {
			File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY);
			if (!directory.exists() && !directory.mkdirs()) {
				//this.mMapViewer.showToastOnUiThread("Could not create screenshot directory");
				String aStr = this.mMapViewer.getResources().getString(R.string.screenshot_capturer_could_not_create_screenshot_directory);
				this.mMapViewer.showToastOnUiThread(aStr);
				return;
			}

			File outputTempFile = assembleFileTempPath(directory);
			
			if (this.mMapViewer.mapView.takeScreenshot(this.compressFormat, SCREENSHOT_QUALITY, outputTempFile)) {
				// we save the map picture to a temp file
				this.width = this.mMapViewer.mapView.getWidth();
				this.height = this.mMapViewer.mapView.getHeight();
				this.mBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.RGB_565);
				mCanvas = new Canvas(this.mBitmap);
				File inputFile = assembleFileTempPath(directory);
				FileInputStream is = new FileInputStream(inputFile);
				mBitmap = BitmapFactory.decodeStream(is);
				is.close();
				Overlay aOverlay = mMapViewer.mSeamarkItemizedOverlay;
				aOverlay.draw(mCanvas);
				File outputFile = assembleFilePath(directory);
				while (outputFile.exists()) {
					screenshotNumber++;
					outputFile = assembleFilePath(directory);
				}
				FileOutputStream outputStream = new FileOutputStream(outputFile);
				mBitmap.compress(compressFormat, SCREENSHOT_QUALITY, outputStream);
				outputStream.close();
				this.mMapViewer.showToastOnUiThread(outputFile.getAbsolutePath());
			} else {
				//this.mMapViewer.showToastOnUiThread("Screenshot could not be saved");
				String aStr = this.mMapViewer.getResources().getString(R.string.screenshot_capturer_screenshot_could_not_be_saved);
				this.mMapViewer.showToastOnUiThread(aStr);
			}
			
			/*File outputFile = assembleFilePath(directory);
			while (outputFile.exists()) {
				screenshotNumber++;
				outputFile = assembleFilePath(directory);
			}*/
		} catch (IOException e) {
			this.mMapViewer.showToastOnUiThread(e.getMessage());
		}

		this.compressFormat = null;
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected boolean hasWork() {
		return this.compressFormat != null;
	}

	public void captureScreenShot(CompressFormat screenShotFormat) {
		this.compressFormat = screenShotFormat;
		synchronized (this) {
			notify();
		}
	}

}
