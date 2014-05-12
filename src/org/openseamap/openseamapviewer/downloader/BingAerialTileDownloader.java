package org.openseamap.openseamapviewer.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BingAerialTileDownloader extends TileDownloader {

	//private static final String HOST_NAME = "map01.eniro.no/geowebcache/service/tms1.0.0/nautical";
	// see the document eniro Kartenanforderung
	// The browser script requests tiles from the four eniro servers
	// we use them in order 1,2,3,4
	// we assume, that a new tile should be loaded from the next host
	private static final String TAG = "BingAerialTileDownLoader";
	private static final String HOST_NAME1 = "ecn.t1.tiles.virtualearth.net";
	private static final String HOST_NAME2 = "ecn.t2.tiles.virtualearth.net";
	private static final String HOST_NAME3 = "ecn.t3.tiles.virtualearth.net";
	private static final String HOST_NAME4 = "ecn.t4.tiles.virtualearth.net";
	private static final String PROTOCOL = "http";
	private static final byte ZOOM_MAX = 19;
    
	private Context mContext;
	private final StringBuilder stringBuilder;
	private int mHostNumber;
	private final int[] pixels;

	/**
	 * Constructs a new EniroTileDownloader.
	 */
	public BingAerialTileDownloader(Context context) {
		super();
		this.mContext = context;
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
		this.stringBuilder = new StringBuilder();
		mHostNumber = 1;
	}
	

	@Override
	public String getHostName() {
		switch (mHostNumber) {
		
		case 1:
			return HOST_NAME1;
		case 2:
			return HOST_NAME2;
		case 3:
			return HOST_NAME3;
		case 4:
			return HOST_NAME4;
		default:
			return HOST_NAME2;
		}
	}

	@Override
	public String getProtocol() {
		return PROTOCOL;
	}
	
	
	static String computeQuadTree(int zoom, int tilex, int tiley) {
        StringBuilder k = new StringBuilder();
        for (int i = zoom; i > 0; i--) {
            char digit = 48;
            int mask = 1 << (i - 1);
            if ((tilex & mask) != 0) {
                digit += 1;
            }
            if ((tiley & mask) != 0) {
                digit += 2;
            }
            k.append(digit);
        }
        return k.toString();
    }

	@Override
	public String getTilePath(Tile tile) {
		int zoom = tile.zoomLevel;
		int tileX = (int)tile.tileX;
		int tileY = (int)tile.tileY;
		String quadKey = computeQuadTree(zoom,tileX,tileY );
		this.stringBuilder.setLength(0);
		this.stringBuilder.append("/tiles/a");
		this.stringBuilder.append(quadKey);
		this.stringBuilder.append(".jpeg");
		this.stringBuilder.append("?g=1135");
		String myPath = this.stringBuilder.toString();
		//long aTime = System.currentTimeMillis();
		String aTimeStr = "";
		try {
		   //Log.d(TAG,"get tile " + aTimeStr  +" " + tile.tileX + " " + tile.tileY + " zoom "+ tile.zoomLevel );
		  //Logger.d(TAG,"get tile " +aTimeStr + " " +tile.tileX + " " + tile.tileY + "zoom "+ tile.zoomLevel );
		   URL myUrl = new URL(getProtocol(), getHostName(),myPath );
		   String myUrlString ="request " + aTimeStr + " : " + myUrl.toString();
           //Log.d(TAG,"get " + myUrlString);
          // Logger.d(TAG,"get " + myUrlString);
		} catch (MalformedURLException e) {
			Log.d(TAG,"Malformed Url  Exception");
			return "";
		} catch (Exception e ){
			//Logger.d(TAG,"Unkwown Exception " + e.toString());
			return "";
		}
		// if we get a new tile path we set a new tile server for the next request
		// see for this algorithm 
		// org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader--> executeJob
		// getTilePath is called at the third position while constructing the url
		if (mHostNumber > 3){
			mHostNumber = 1;
		}else {
			mHostNumber++;
		}
		return this.stringBuilder.toString();
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}
	/*
	 * @Override
	 */
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		try {
			Tile tile = mapGeneratorJob.tile;
			//Log.d(TAG,"loading tile x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel );
			URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
			String urlText = url.getPath();
			URLConnection con =url.openConnection();
	        con.setConnectTimeout(5000);
	        con.setReadTimeout(5000);
	        con.connect(); 
	        InputStream   inputStream = con.getInputStream();
			//InputStream inputStream = url.openStream();
			Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
			inputStream.close();

			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap == null) {
				return false;
			}

			// copy all pixels from the decoded bitmap to the color array
			decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			return true;
		} catch (UnknownHostException e) {
			//LOG.log(Level.SEVERE, null, e);
			Log.d(TAG,e.toString());
			return false;
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			Log.d(TAG,e.toString());
			return false;
		}
	}
}
