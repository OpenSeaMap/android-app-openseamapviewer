package org.openseamap.openseamapviewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openseamap.openseamapviewer.environment.Device;
import org.openseamap.openseamapviewer.environment.Environment2;
import org.openseamap.openseamapviewer.environment.Size;
import org.openseamap.openseamapviewer.viewer.DownloadEniroAerialViewer;
import org.openseamap.openseamapviewer.viewer.DownloadEniroNauticalViewer;
import org.openseamap.openseamapviewer.viewer.DownloadOpenSeamapTileAndSeamarksViewer;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapViewer;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapViewerWithInfoPois;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapWithAerialOverlayViewer;
import org.openseamap.openseamapviewer.viewer.OpenSeamapViewerWithPois;
import org.openseamap.openseamapviewer.viewer.SplitOpenSeaMapWithAerialOverlayAndPoisViewer;
import org.openseamap.openseamapviewer.viewer.SplitOpenSeaMapWithAerialOverlayViewer;
import org.openseamap.openseamapviewer.viewer.SplitScreenViewer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";
	private static final boolean test = false;
	private static final int releasePublic = 1;  
	private static final int releaseSweden = 2;
	private static final int releaseDevelop = 3;
	
	private static final int releaseVersion = 1;
	
	public static final String OPENSEAMAP_STANDARDRENDERERNAME ="openseamaprenderer001.xml";
	public static final String OPENSEAMAP_STANDARDSYMBOLDEFS ="symbols.xml";  // since 13_01_09
	public static final String OPENSEAMAP_STANDARDDIRECTORYNAME = "/openseamapviewer";
	public static final String OPENSEAMAP_TESTMAP ="Testkarte_Wismarbucht";
	public static final String OPENSEAMAP_TESTROUTE= "route_Boltenhagen_Timmendorf";
	
	public static final int seamarkNameNotVisible = 0;
	public static final int seamarkShortNameVisible= 1;
	public static final int seamarkLongNameVisible= 2;
	
	// deal with prefs
	public static final int PREV_APP_VERSION =  57;
	public static final int PREV_APP_BUILD = 01;   // last from  14_01_29
	public static final String PREV_APP_VERSION_STR ="0.57";
	public static final String PREV_APP_BUILD_STR = "01";
	 
	public static final String PREF_ZOOM_FACTOR ="zoom_factor";
	public static final String PREV_LAST_GEOPOINT_LAT ="lastgeopointlat";
	public static final String PREV_LAST_GEOPOINT_LON ="lastgeopointlon";
	public static final String PREV_LAST_MAPFILE_PATH ="lastmapfilepath";
	
	private String mExternalPathName = "";
    private File mExternalStorageDir = null;
	private TextView mMainTextView;
	private  ActivityManager mActivityManager = null;
	
	Button mOpenSeamapViewerButton;
	Button mOpenSeamapViewerWithPoisButton;
	Button mOpenSeamapViewerWithInfoPoisButton;
	Button mOpenSeamapViewerDownloadTilesButton;
	Button mInfoButton;
	Button mEniroNauticalViewerDownloadTilesButton;
	Button mEniroAerialViewerDownloadTilesButton;
	Button mOpenSeaMapWithBingAerialOverlayButton;
	Button mOpenSeaMapWithEniroAerialOverlayButton;
	
	Button mSplitScreenViewerButton1;
	Button mSplitScreenViewerButton2;
	Button mSplitScreenViewerButton3;
	Button mSplitScreenViewerButton4;
	Button mSplitScreenViewerButton5;
	Button mSplitScreenViewerButton6;
	
	
	SharedPreferences prefs; // since Version 0.5 Build 013
	
	
	// deal with Bing or Eniro Aerial tile downloader
	public static final String DOWNLOAD_AERIAL_UNDERLAY ="underlay";
	public static final String DOWNLOAD_AERIAL_TYPE_BING = "bing";
	public static final String DOWNLOAD_AERIAL_TYPE_ENIRO = "eniro";
	
	
	// deal with the maps in the splitView
	public static final String VIEW_LEFT_KEY = "viewLeft";
	public static final String VIEW_RIGHT_KEY = "viewRight";
	public static final String VIEW_MAPTYPE_ENIRO_AERIAL = "eniroaerial";
	public static final String VIEW_MAPTYPE_ENIRO_NAUTICAL = "enironautical";
	public static final String VIEW_MAPTYPE_OPENSEAMAP = "openseamap";
	
	
	private boolean firstStart() {
		  boolean isFirstStart = prefs.getBoolean("firstStart", true);
		  return isFirstStart;  
	  }
	
	  private void doFirstStart() {
		  // int lat = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LAT, 180);
	      // int lon = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LON, 90);
		  int lat = (int)(54.0f *1E6);  // set Poel as initial start location
		  int lon = (int)(11.35f*1E6); 
		  int zoom = 12;
		  prefs.edit()
		    .putBoolean("firstStart", false)
		    .putInt(MainActivity.PREV_LAST_GEOPOINT_LAT, lat)
		    .putInt(MainActivity.PREV_LAST_GEOPOINT_LON,lon)
		    .putInt(MainActivity.PREF_ZOOM_FACTOR, zoom)
		    .commit();
		  String aStr = getResources().getString(R.string.main_activity_location_of_testmap);
		  mMainTextView.append("\n"+aStr);
	  }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main7);
		mExternalStorageDir = Environment2.getCardDirectory();
		mExternalPathName = mExternalStorageDir.getAbsolutePath();
		TextView mainTextView = (TextView) findViewById(R.id.main_textview);
        mMainTextView = mainTextView;
        mActivityManager =(ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        //checkAndDisplayMemory();
        //checkAndDisplayDevices();
        createExternalDirectoryIfNecessary(OPENSEAMAP_STANDARDDIRECTORYNAME);
        copyTestMapIfNecessary() ;	
        prefs = PreferenceManager.getDefaultSharedPreferences(this); 
        int savedVersion = prefs.getInt("VERSION", 0);
		int savedBuild = prefs.getInt("BUILD",0);
		if (savedBuild < PREV_APP_BUILD) {  // update symbols and renderer
		   String symboldefsName = OPENSEAMAP_STANDARDSYMBOLDEFS;
		   boolean ok = copyFileFromAssetToStandardDirectory (symboldefsName); // copy a new symboldefs
		   String renderThemeName = OPENSEAMAP_STANDARDRENDERERNAME;
		   ok = copyFileFromAssetToStandardDirectory (renderThemeName);  // copy the renderer
		}
		copyStandardRendererIfNecessary();     
        copyStandardSymbolDefsIfNecessary();
        String titleStr = getResources().getString(R.string.title_activity_main);
        this.setTitle(titleStr + " " + PREV_APP_VERSION_STR);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.main_layout);
        
        mOpenSeamapViewerButton = (Button)findViewById(R.id.main_button1);
        //mOpenSeamapViewerButton.setText(getResources().getString(R.string.main_activity_openseamapviewer));
        mOpenSeamapViewerButton.setText(getResources().getString(R.string.main_activity_openseamapviewer_with_info_pois));
        mOpenSeamapViewerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, OpenSeaMapViewerWithInfoPois.class);
				//Intent aIntent = new Intent(MainActivity.this, OpenSeaMapViewer.class);
			    startActivity(aIntent);
			}
		});
        
        mOpenSeamapViewerWithPoisButton = (Button) findViewById(R.id.main_button2);
        mOpenSeamapViewerWithPoisButton.setText(getResources().getString(R.string.main_activity_openseamapviewer_with_pois));
        mOpenSeamapViewerWithPoisButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, OpenSeamapViewerWithPois.class);
			    startActivity(aIntent);
			}
		});
       /* mOpenSeamapViewerWithInfoPoisButton = (Button) findViewById(R.id.main_button4);
        mOpenSeamapViewerWithInfoPoisButton.setText(getResources().getString(R.string.main_activity_openseamapviewer_with_info_pois));
        mOpenSeamapViewerWithInfoPoisButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, OpenSeaMapViewerWithInfoPois.class);
			    startActivity(aIntent);
			}
		});*/
        
        
        mOpenSeamapViewerDownloadTilesButton = (Button) findViewById(R.id.main_button3);
        mOpenSeamapViewerDownloadTilesButton.setText(getResources().getString(R.string.main_activity_openseamapviewer_download));
        mOpenSeamapViewerDownloadTilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent(MainActivity.this, DownloadOpenSeamapTileAndSeamarksViewer.class);
				    startActivity(aIntent);
				
			}
		});
        
        mInfoButton = (Button) findViewById(R.id.main_button4);
        mInfoButton.setText(getResources().getString(R.string.main_activity_info));
        mInfoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, InfoActivity.class);
			    startActivity(aIntent);
			    
			}
		});
        
        mEniroNauticalViewerDownloadTilesButton = (Button) findViewById(R.id.main_button5);
        mEniroNauticalViewerDownloadTilesButton.setText(getResources().getString(R.string.main_activity_eniro_nautical_viewer_download));
        mEniroNauticalViewerDownloadTilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent(MainActivity.this, DownloadEniroNauticalViewer.class);
				    startActivity(aIntent);
				
			}
		});
        
        mEniroAerialViewerDownloadTilesButton = (Button) findViewById(R.id.main_button6);
        mEniroAerialViewerDownloadTilesButton.setText(getResources().getString(R.string.main_activity_eniro_aerial_viewer_download));
        mEniroAerialViewerDownloadTilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent(MainActivity.this, DownloadEniroAerialViewer.class);
				    startActivity(aIntent);
				
			}
		});
        
        
        mOpenSeaMapWithBingAerialOverlayButton = (Button) findViewById(R.id.main_button7);
        String info1 = getResources().getString(R.string.main_activity_openseamapviewer_with_bing_aerial_download);
        mOpenSeaMapWithBingAerialOverlayButton.setText(info1);
        mOpenSeaMapWithBingAerialOverlayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent (MainActivity.this, OpenSeaMapWithAerialOverlayViewer.class);
				    aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY, DOWNLOAD_AERIAL_TYPE_BING);
				    startActivity(aIntent);
				
			}
		}); 
        
        mOpenSeaMapWithEniroAerialOverlayButton = (Button) findViewById(R.id.main_button8);
        String info2 = getResources().getString(R.string.main_activity_openseamapviewer_with_eniro_aerial_download);
        mOpenSeaMapWithEniroAerialOverlayButton.setText(info2);
        mOpenSeaMapWithEniroAerialOverlayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent (MainActivity.this, OpenSeaMapWithAerialOverlayViewer.class);
				    aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY,DOWNLOAD_AERIAL_TYPE_ENIRO);
				    startActivity(aIntent);
				
			}
		});
        
        
        mSplitScreenViewerButton1 = (Button) findViewById(R.id.main_button9);
        String info9 = "Split Screen  Aerial (online) / Eniro Nautical "; // getResources().getString(R.string.main_activity_openseamapviewer_with_eniro_aerial_download);
        mSplitScreenViewerButton1.setText(info9);
        mSplitScreenViewerButton1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent (MainActivity.this, SplitScreenViewer.class);
				    aIntent.putExtra(VIEW_LEFT_KEY,VIEW_MAPTYPE_ENIRO_AERIAL);
				    aIntent.putExtra(VIEW_RIGHT_KEY,VIEW_MAPTYPE_ENIRO_NAUTICAL);
				    startActivity(aIntent);
				
			}
		});
        
        mSplitScreenViewerButton2 = (Button) findViewById(R.id.main_button10);
        String info10 = "Split Screen Eniro Aerial /  OpenSeaMap (online)"; // getResources().getString(R.string.main_activity_openseamapviewer_with_eniro_aerial_download);
        mSplitScreenViewerButton2.setText(info10);
        mSplitScreenViewerButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent aIntent = new Intent (MainActivity.this, SplitScreenViewer.class);
			    aIntent.putExtra(VIEW_LEFT_KEY,VIEW_MAPTYPE_ENIRO_AERIAL);
			    aIntent.putExtra(VIEW_RIGHT_KEY,VIEW_MAPTYPE_OPENSEAMAP);
			    startActivity(aIntent);
			}
		});
         
        mSplitScreenViewerButton3 = (Button) findViewById(R.id.main_button11);
        String info11 = "  Split Screen OpenSeaMap with aerial (online) / Eniro Nautical "; // getResources().getString(R.string.main_activity_openseamapviewer_with_eniro_aerial_download);
        mSplitScreenViewerButton3.setText(info11);
        mSplitScreenViewerButton3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent aIntent = new Intent (MainActivity.this, SplitOpenSeaMapWithAerialOverlayViewer.class);
				aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY,DOWNLOAD_AERIAL_TYPE_ENIRO);
			    startActivity(aIntent);
			}
		});
        
        mSplitScreenViewerButton4 = (Button) findViewById(R.id.main_button12);
        String info12 = " Split Screen OpenSeaMap (offline map) with Aerial Underlay / Eniro Nautical "; 
        mSplitScreenViewerButton4.setText(info12);
        mSplitScreenViewerButton4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent aIntent = new Intent (MainActivity.this, SplitOpenSeaMapWithAerialOverlayViewer.class);
				aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY,DOWNLOAD_AERIAL_TYPE_ENIRO);
			    startActivity(aIntent);
			}
		});
        
        mSplitScreenViewerButton5 = (Button) findViewById(R.id.main_button13);
        String info13 = "Split Screen OpenSeaMap offline map with POIs /  Eniro Nautical "; 
        mSplitScreenViewerButton5.setText(info13);
        mSplitScreenViewerButton5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent aIntent = new Intent (MainActivity.this, SplitOpenSeaMapWithAerialOverlayAndPoisViewer.class);
				
				//Intent aIntent = new Intent (MainActivity.this, SplitOpenSeaMapWithAerialOverlayViewer.class);
				//aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY,DOWNLOAD_AERIAL_TYPE_ENIRO);
			    startActivity(aIntent);
			}
		});
        
        mSplitScreenViewerButton6 = (Button) findViewById(R.id.main_button14);
        String info14 = "Split Screen OpenSeaMap offline map  with POIs Aerial Underlay  /  Eniro Nautical "; 
        mSplitScreenViewerButton6.setText(info14);
        mSplitScreenViewerButton6.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent aIntent = new Intent (MainActivity.this, SplitOpenSeaMapWithAerialOverlayAndPoisViewer.class);
				aIntent.putExtra(DOWNLOAD_AERIAL_UNDERLAY,DOWNLOAD_AERIAL_TYPE_ENIRO);
			    startActivity(aIntent);
			}
		});
        //mSplitScreenViewerButton4.setVisibility(View.INVISIBLE);
        /*
        Button aCreateOpenSeamapViewerButton =createOpenSeaMapViewerButton();
        linearLayout.addView(aCreateOpenSeamapViewerButton);
        
        Button aCreateOpenSeamapViewerWithPoisButton =createOpenSeaMapViewerWithPoisButton();
        linearLayout.addView(aCreateOpenSeamapViewerWithPoisButton);
        
        Button aOpenSeamapTileAndSeamarksButton = createOpenSeamapTileAndSeamarksDownloadViewerButton();
		linearLayout.addView(aOpenSeamapTileAndSeamarksButton);
		
		Button aInfoButton = createInfoButton();
		linearLayout.addView(aInfoButton);*/
        
        if (this.firstStart()){
        	doFirstStart();
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		int rows = 7; // we have 7 rows of buttons
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        int standardWidth = width / 2 -5;
        int standardHeight = height/rows -30;
        if (releaseVersion == releasePublic) {
        	rows = 2; 
        	standardWidth = width / 2 - 5;
        	standardHeight = height/rows -30;
        	mOpenSeamapViewerButton.setHeight(standardHeight);
            mOpenSeamapViewerButton.setWidth(standardWidth);
            mOpenSeamapViewerWithPoisButton.setHeight(standardHeight);
            mOpenSeamapViewerWithPoisButton.setWidth(standardWidth);
            mOpenSeamapViewerDownloadTilesButton.setHeight(standardHeight);
            mOpenSeamapViewerDownloadTilesButton.setWidth(standardWidth);
            //mOpenSeamapViewerWithInfoPoisButton.setHeight(standardHeight);
            //mOpenSeamapViewerWithInfoPoisButton.setWidth(standardWidth);
            mInfoButton.setHeight(standardHeight);
            mInfoButton.setWidth(standardWidth);
            mEniroNauticalViewerDownloadTilesButton.setVisibility(View.INVISIBLE);
            mEniroAerialViewerDownloadTilesButton.setVisibility(View.INVISIBLE);
            mOpenSeaMapWithBingAerialOverlayButton.setVisibility(View.INVISIBLE);
            mOpenSeaMapWithEniroAerialOverlayButton.setVisibility(View.INVISIBLE);
            mSplitScreenViewerButton1.setVisibility(View.INVISIBLE);  //EniroAerial- Nautical
            mSplitScreenViewerButton2.setVisibility(View.INVISIBLE);  // EniroAerial - OpenSeaMap online
            mSplitScreenViewerButton3.setVisibility(View.INVISIBLE);  // OpenSeaMap online with Aerial underlay/ Enirio nautical
            mSplitScreenViewerButton4.setVisibility(View.INVISIBLE);  // OpenSeaMap offline with Aerial underlay / Eniro nautical
            mSplitScreenViewerButton5.setVisibility(View.INVISIBLE);  // OpenSeaMap with pois / Eniro nautical
            mSplitScreenViewerButton6.setVisibility(View.INVISIBLE);  // OpenSeaMap with pois with Aerial underlay / Eniro nautical
            
        }
        if (releaseVersion == releaseDevelop) {
        	mOpenSeamapViewerButton.setHeight(standardHeight);
            mOpenSeamapViewerButton.setWidth(standardWidth);
            //mOpenSeamapViewerButton.setBackgroundColor(Color.BLUE);
            mOpenSeamapViewerWithPoisButton.setHeight(standardHeight);
            mOpenSeamapViewerWithPoisButton.setWidth(standardWidth);
            //mOpenSeamapViewerWithPoisButton.setBackgroundColor(Color.BLUE);
            mOpenSeamapViewerDownloadTilesButton.setHeight(standardHeight);
            mOpenSeamapViewerDownloadTilesButton.setWidth(standardWidth);
            //mOpenSeamapViewerDownloadTilesButton.setBackgroundColor(Color.BLUE);
            mInfoButton.setHeight(standardHeight);
            mInfoButton.setWidth(standardWidth);
            
            mEniroNauticalViewerDownloadTilesButton.setHeight(standardHeight);
            mEniroNauticalViewerDownloadTilesButton.setWidth(standardWidth);
            
            mEniroAerialViewerDownloadTilesButton.setHeight(standardHeight);
            mEniroAerialViewerDownloadTilesButton.setWidth(standardWidth);
            
            mOpenSeaMapWithBingAerialOverlayButton.setHeight(standardHeight);
            mOpenSeaMapWithBingAerialOverlayButton.setWidth(standardWidth);
            
            mOpenSeaMapWithEniroAerialOverlayButton.setHeight(standardHeight);
            mOpenSeaMapWithEniroAerialOverlayButton.setWidth(standardWidth);
            
            mSplitScreenViewerButton1.setHeight(standardHeight);  // Eniro Aerial - Nautical
            mSplitScreenViewerButton1.setWidth(standardWidth);    
            
            mSplitScreenViewerButton2.setHeight(standardHeight);  // EniroAerial - OpenSeaMap online
            mSplitScreenViewerButton2.setWidth(standardWidth);
            
            mSplitScreenViewerButton3.setHeight(standardHeight);  // OpenSeaMap online with Aerial underlay/ Enirio nautical
            mSplitScreenViewerButton3.setWidth(standardWidth);
            
            mSplitScreenViewerButton4.setHeight(standardHeight);  // OpenSeaMap offline with Aerial underlay / Eniro nautical
            mSplitScreenViewerButton4.setWidth(standardWidth);
            
            mSplitScreenViewerButton5.setHeight(standardHeight);  // OpenSeaMap with pois / Eniro nautical
            mSplitScreenViewerButton5.setWidth(standardWidth);
            
            mSplitScreenViewerButton6.setHeight(standardHeight);  // OpenSeaMap with pois with Aerial underlay / Eniro nautical
            mSplitScreenViewerButton6.setWidth(standardWidth);
            //mInfoButton.setBackgroundColor(Color.BLUE);
            //mEniroNauticalViewerDownloadTilesButton.setVisibility(View.INVISIBLE);
            //mEniroAerialViewerDownloadTilesButton.setVisibility(View.INVISIBLE);
        }
        
	}
	
	@Override
	protected void onPause() {
		prefs.edit()
     	.putInt("VERSION", PREV_APP_VERSION)
     	.putInt("BUILD", PREV_APP_BUILD)
		.commit();
		super.onPause();
	}
	
	private void checkAndDisplayMemory() {
		int countMemory = mActivityManager.getMemoryClass();
        StringBuffer aBuf = new StringBuffer();
        aBuf.append(getResources().getString(R.string.main_activity_used_memory));
        aBuf.append(countMemory);
        aBuf.append(" Mb \n");
        mMainTextView.append(aBuf.toString());
	}
	
	private Button createInfoButton() {
		Button button = new Button(this);
		//button.setText("OpenSeamapViewer with POIs");
		button.setText(getResources().getString(R.string.main_activity_info));
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, InfoActivity.class);
			    startActivity(aIntent);
			}
		});
		return button;
    }
	
	private Button createOpenSeaMapViewerButton() {
		Button button = new Button(this);
		//button.setText("OpenSeamapViewer with POIs");
		button.setText(getResources().getString(R.string.main_activity_openseamapviewer));
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, OpenSeaMapViewer.class);
			    startActivity(aIntent);
			}
		});
		return button;
    }
	
	private Button createOpenSeaMapViewerWithPoisButton() {
		Button button = new Button(this);
		//button.setText("OpenSeamapViewer with POIs");
		button.setText(getResources().getString(R.string.main_activity_openseamapviewer_with_pois));
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {   
				Intent aIntent = new Intent(MainActivity.this, OpenSeamapViewerWithPois.class);
			    startActivity(aIntent);
			}
		});
		return button;
    }
	
	private Button createOpenSeamapTileAndSeamarksDownloadViewerButton() {
		Button button = new Button(this);
		//button.setText("Download Opensea Tile Map and Seamarks");
		button.setText(getResources().getString(R.string.main_activity_openseamapviewer_download));
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				    Intent aIntent = new Intent(MainActivity.this, DownloadOpenSeamapTileAndSeamarksViewer.class);
				    startActivity(aIntent);
				
			}
		});
		return button;
    }

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}*/
	
	private static void copy( InputStream in , OutputStream out)throws IOException {
		byte[] buffer = new byte[0xFFFF];
		for (int len; (len = in.read(buffer)) !=-1;) {
			out.write(buffer,0,len);
		}
	}
    
   private void copyStandardRendererIfNecessary() {
	   String renderThemeName = OPENSEAMAP_STANDARDRENDERERNAME;
	   String dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + renderThemeName;
	   File aTestFile = new File(dest);
	   if (!aTestFile.exists()){
		   boolean ok = copyFileFromAssetToStandardDirectory (renderThemeName);
	   }
   }
   private void copyStandardSymbolDefsIfNecessary() {
	   String symboldefsName = OPENSEAMAP_STANDARDSYMBOLDEFS;
	   String dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + symboldefsName;
	   File aTestFile = new File(dest);
	   if (!aTestFile.exists()){
		   boolean ok = copyFileFromAssetToStandardDirectory (symboldefsName);
	   }
	  
   }
   
   
   private void checkAndDisplayDevices () {
	    String result = Environment2.getCardState();
		if (result.equals(Environment.MEDIA_MOUNTED)) {
			Device[] devices = Environment2.getDevices(null, false, true); 
			int count = devices.length;
			if (count > -1) {
				for (int index=0;index < count ; index++){
					String devInfoMountPoint = devices[index].getMountPoint();
					Device device = devices[index];
					Size size  = device.getSize();
					long aSize = -1;
					if (size !=null ) aSize = size.guessSize();
					boolean available = device.isAvailable();
					String devInfoSize = Long.toString(aSize);
					String info ="";
					if ( available){
						info = "\nnr: " + Integer.toString(index ) + " Device info: " 
						+ devInfoMountPoint + " size: " + devInfoSize + " is available";
					} else  {
						info = "\nnr: " + Integer.toString(index ) + " Device info: " 
						+ devInfoMountPoint + " not available";
					}
		              
					mMainTextView.append(info);
				}
			}
			File pathToMainDir = Environment2.getCardDirectory();
			String pathToMainDirStr = pathToMainDir.getAbsolutePath();
			mMainTextView.append("\nMain Directory: " + pathToMainDirStr +"\n");
		}
   }
   
   private void createExternalDirectoryIfNecessary(String pDirPath) {
		if (test)
			Log.v(TAG, "createDirectory");
		//String result = Environment.getExternalStorageState(); since 12_11_19 use Environment2
		String result = Environment2.getCardState();
		if (result.equals(Environment.MEDIA_MOUNTED)) {

			//File path = getExternalStorageDir(); since 12_11_19 
			File pathToMainDir = Environment2.getCardDirectory();
			
			// if pDirName contains / we have to analyse the whole path
			String [] dirs = pDirPath.split("/");
			int dirCount = dirs.length;
			String newDirName = "/"; // we must begin with a /
		    //  now we know how many dirs we must create
			for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
				StringBuffer buf = new StringBuffer();
				newDirName = newDirName + dirs[dirIndex]  +"/"; 
				buf.append(newDirName);
				String dirName = buf.toString();
				File file = new File(pathToMainDir, dirName);
				try {
					String filePathStr = file.getAbsolutePath();
					if (file.mkdir()) { // here we need android permission in the manifest
						String aStr = getResources().getString(R.string.main_activity_directory_create);
						//mMainTextView.append("create " +filePathStr);
						mMainTextView.append(aStr +filePathStr);
						if (test)
							Log.v(TAG, "create Directory: " + filePathStr);
						    
					} else {
						if (test)
							Log.v(TAG, "directory exists " + filePathStr);
					}
				} catch (SecurityException se) {
					Log.d(TAG,se.toString());
					if (test)
						Log.v("TAG", "Security exception : Directory not created " + se);
				} catch (Exception e ) {
					Log.d(TAG,e.toString());
					String aStr = getResources().getString(R.string.error);
					//mMainTextView.append("error: " + e.toString());
					mMainTextView.append(aStr + e.toString());
				} // try
			} //for
		}
	}
   
   private void copyTestMapIfNecessary() {
	   String testMapName = OPENSEAMAP_TESTMAP;
	   String dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + testMapName+".map";
	   File aTestFile = new File(dest);
	   if (!aTestFile.exists()){
		   boolean ok = copyFileFromAssetToStandardDirectory (testMapName+".map");
	   }
	   
	    dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + testMapName+"_seamarks.xml";
	    aTestFile = new File(dest);
	   if (!aTestFile.exists()){
		   boolean ok = copyFileFromAssetToStandardDirectory (testMapName+"_seamarks.xml");
	   }
	   
	   dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/"+OPENSEAMAP_TESTROUTE + ".gml";
	    aTestFile = new File(dest);
	   if (!aTestFile.exists()){
		   boolean ok = copyFileFromAssetToStandardDirectory (OPENSEAMAP_TESTROUTE + ".gml");
	   }
	    
   }
   
  
    
    private boolean copyFileFromAssetToStandardDirectory (String fileName) {
		boolean result = false;
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			String dest = mExternalPathName + OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + fileName;
			is = getAssets().open(fileName);
			fos = new FileOutputStream(dest);
			copy(is,fos);
			String aStr = getResources().getString(R.string.main_activity_copy);
			//mMainTextView.append("\n" + "copy : " + fileName); 
			mMainTextView.append("\n" + aStr +fileName);  
			result = true;
		} catch (IOException e){
			if (test) Log.d(TAG,"cant create file "+ e.toString());
			String aStr = getResources().getString(R.string.main_activity_create_std_renderer);
			//mMainTextView.append("\n" + "can't create OpenSeaRenderer "+ e.toString());
			mMainTextView.append("\n" + aStr + e.toString());
		} finally {
			if (is != null)
				try { is.close(); }catch (IOException e){}
			if (fos != null)
				try { fos.close(); } catch (IOException e) {}
		}
		return result;
	}
    
    

}
