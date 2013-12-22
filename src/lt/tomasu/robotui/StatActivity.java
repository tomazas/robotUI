package lt.tomasu.robotui;

import lt.tomasu.ssh.client.AccelerometerData;
import lt.tomasu.ssh.client.GyroData;
import lt.tomasu.ssh.client.MagnetometerData;
import lt.tomasu.ssh.client.SonarStatus;
import lt.tomasu.ssh.client.SystemStatus;
import lt.tomasu.ssh.client.api.Controller;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

public class StatActivity extends Activity {
	
	private MyApp app;
	
	private GraphViewSeries magData, procData;
	private GraphViewSeries accelX, accelY, accelZ;
	private GraphViewSeries gyroX, gyroY, gyroZ;
	private GraphViewSeries sonarFront, sonarBack;
	
	private final Handler mHandler = new Handler();
	private Runnable timer;
	private double lastX = 0;
	
	private TextView systats, ramText, hddText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stat);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setTitle("Statistics");
		
		app = (MyApp)getApplication();
		
		systats = (TextView)findViewById(R.id.systats);
		ramText = (TextView)findViewById(R.id.ramText);
		hddText = (TextView)findViewById(R.id.hddText);
		
		//////////////////////////////////////////////////////////////////////////////
		
		
		// init example series data (dummy data)
		int count = 1;
		GraphViewData[] dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		
		magData = new GraphViewSeries("Magnetometer heading", 
				new GraphViewSeriesStyle(Color.rgb(255, 128, 0), 2), dummydata);
		
		GraphView graph = new LineGraphView(this, "Magnetometer");
		graph.addSeries(magData); // data
		graph.setShowLegend(true);
		graph.setViewPort(0, 200);
		graph.setScrollable(true);
		graph.setScalable(true);
		graph.setLegendAlign(LegendAlign.BOTTOM);
		graph.setLegendWidth(200);
		graph.setManualYAxisBounds(360, 0);
		graph.setViewPort(0, 100);
		graph.setManualYAxis(false);
		graph.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.charts));
		graph.getGraphViewStyle().setNumHorizontalLabels(5);
		LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
		layout.addView(graph);
		
		// accelerometer
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		accelX = new GraphViewSeries("Accel-X", 
				new GraphViewSeriesStyle(Color.rgb(90, 250, 00), 2), dummydata);
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		accelY = new GraphViewSeries("Accel-Y", 
				new GraphViewSeriesStyle(Color.rgb(200, 50, 00), 2), dummydata);
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		accelZ = new GraphViewSeries("Accel-Z", 
				new GraphViewSeriesStyle(Color.rgb(0, 0, 255), 2), dummydata);
		
		graph = new LineGraphView(this, "Accelerometer");
		graph.addSeries(accelX);
		graph.addSeries(accelY);
		graph.addSeries(accelZ);
		graph.setShowLegend(true);
		graph.setViewPort(0, 200);
		graph.setScrollable(true);
		graph.setScalable(true);
		graph.setLegendAlign(LegendAlign.BOTTOM);
		graph.setLegendWidth(200);
		graph.setViewPort(0, 100);
		graph.setManualYAxis(false);
		graph.getGraphViewStyle().setNumHorizontalLabels(5);
		//graph.setManualYAxisBounds(2.56, -2.56);
		graph.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.charts));
		layout = (LinearLayout) findViewById(R.id.graph2);
		layout.addView(graph);
		
		// ultrasound sonar
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		sonarFront = new GraphViewSeries("Front (mm)", 
				new GraphViewSeriesStyle(Color.rgb(90, 250, 00), 2), dummydata);
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		sonarBack = new GraphViewSeries("Back (mm)", 
				new GraphViewSeriesStyle(Color.rgb(200, 50, 00), 2), dummydata);
		
		graph = new LineGraphView(this, "Ultrasound Sonar");
		graph.addSeries(sonarFront);
		graph.addSeries(sonarBack);
		graph.setShowLegend(true);
		graph.setViewPort(0, 200);
		graph.setScrollable(true);
		graph.setScalable(true);
		graph.setLegendAlign(LegendAlign.BOTTOM);
		graph.setLegendWidth(200);
		graph.setViewPort(0, 100);
		graph.setManualYAxis(false);
		graph.getGraphViewStyle().setNumHorizontalLabels(5);
		//graph.setManualYAxisBounds(2.56, -2.56);
		graph.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.charts));
		layout = (LinearLayout) findViewById(R.id.graph3);
		layout.addView(graph);
		
		// processing time
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		procData = new GraphViewSeries("Time (ms)", 
				new GraphViewSeriesStyle(Color.rgb(255, 255, 255), 2), dummydata);
		
		graph = new LineGraphView(this, "ATmega processing time");
		graph.addSeries(procData);
		graph.setShowLegend(true);
		graph.setViewPort(0, 200);
		graph.setScrollable(true);
		graph.setScalable(true);
		graph.setLegendAlign(LegendAlign.BOTTOM);
		graph.setLegendWidth(200);
		graph.setViewPort(0, 100);
		graph.setManualYAxis(false);
		graph.getGraphViewStyle().setNumHorizontalLabels(5);
		//graph.setManualYAxisBounds(2.56, -2.56);
		graph.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.charts));
		layout = (LinearLayout) findViewById(R.id.graph4);
		layout.addView(graph);
		
		// gyroscope
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		gyroX = new GraphViewSeries("Gyro-X", 
				new GraphViewSeriesStyle(Color.rgb(90, 250, 00), 2), dummydata);
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		gyroY = new GraphViewSeries("Gyro-Y", 
				new GraphViewSeriesStyle(Color.rgb(200, 50, 00), 2), dummydata);
		dummydata = new GraphViewData[count];
		for (int i=0; i<count; i++) {
			dummydata[i] = new GraphViewData(i, 0);
		}
		gyroZ = new GraphViewSeries("Gyro-Z", 
				new GraphViewSeriesStyle(Color.rgb(0, 0, 255), 2), dummydata);
		
		graph = new LineGraphView(this, "Gyroscope");
		graph.addSeries(gyroX);
		graph.addSeries(gyroY);
		graph.addSeries(gyroZ);
		graph.setShowLegend(true);
		graph.setViewPort(0, 100);
		graph.setScrollable(true);
		graph.setScalable(true);
		graph.setLegendAlign(LegendAlign.BOTTOM);
		graph.setLegendWidth(200);
		graph.setViewPort(0, 100);
		graph.setManualYAxis(false);
		graph.getGraphViewStyle().setNumHorizontalLabels(5);
		//graph.setManualYAxisBounds(2.56, -2.56);
		graph.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.charts));
		layout = (LinearLayout) findViewById(R.id.graph5);
		layout.addView(graph);
		
		new SystemStatsTask().execute();
	}
	
	@Override
	protected void onPause() {
		mHandler.removeCallbacks(timer);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		timer = new Runnable() {
			@Override
			public void run() {
				new StatTask().execute();
			}
		};
		mHandler.postDelayed(timer, 1000);
	}
	
	// -------------------------------------------------------------------
	
	private class SysSnapshot {
    	public String error;
    	public MagnetometerData mag;
    	public GyroData gyro;
    	public SonarStatus sonar;
    	public AccelerometerData accel;
    	public SystemStatus sys;
    }
	
	private class StatTask extends AsyncTask<Object, Integer, SysSnapshot> {
		@Override
		protected SysSnapshot doInBackground(Object... arg) {
			SysSnapshot rs = new SysSnapshot();
			try {
				Controller c = app.getController();
				if (c != null) {
			        rs.mag = c.getMagnetometerData();
			        rs.gyro = c.getGyroData();
			        rs.sonar = c.getSonarStatus();
			        rs.accel = c.getAccelerometerData();
			        rs.sys = c.getSystemStatus();
				}
	    	} catch (Exception e) {
	    		rs.error = "System stat failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
			return rs;
		}
		
		@Override
	    protected void onPostExecute(SysSnapshot result) {
			try {
				if (result.error == null) { // error
					lastX += 5;
					magData.appendData(new GraphViewData(lastX, result.mag.getHeadingAngle()), true, 100);
	
					accelX.appendData(new GraphViewData(lastX, result.accel.getVoltX()), true, 100);
					accelY.appendData(new GraphViewData(lastX, result.accel.getVoltY()), true, 100);
					accelZ.appendData(new GraphViewData(lastX, result.accel.getVoltZ()), true, 100);
					
					sonarFront.appendData(new GraphViewData(lastX, result.sonar.getFrontDistance()), true, 100);
					sonarBack.appendData(new GraphViewData(lastX, result.sonar.getRearDistance()), true, 100);
					
					procData.appendData(new GraphViewData(lastX, result.sys.getProcessTimeMillis()), true, 100);
					
					gyroX.appendData(new GraphViewData(lastX, result.gyro.getPitch()), true, 100);
					gyroY.appendData(new GraphViewData(lastX, result.gyro.getYaw()), true, 100);
					gyroZ.appendData(new GraphViewData(lastX, result.gyro.getRoll()), true, 100);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			mHandler.postDelayed(timer, 1000); // query again later
	    }
	}
	
	// -------------------------------------------------------------------
	
	private class StatContainer {
		public String error;
		public String uptime;
		public long memFreekB;
		public long memTotalkB;
		public long hddUsed;
		public long hddTotal;
	}
	
	private class SystemStatsTask extends AsyncTask<Object, Integer, StatContainer> {
		@Override
		protected void onPreExecute() {
			app.progressShow(StatActivity.this, "Checking status", null);
		}
		@Override
		protected StatContainer doInBackground(Object... arg) {
			StatContainer sc = new StatContainer();
			try {
				sc.uptime = app.shellExec("uptime");
				
				String mem = app.shellExec("cat /proc/meminfo | head -n 2");
				if (mem != null && !mem.isEmpty()) {
					String memlines[] = mem.split("\n");
					Log.d("R", memlines[0]);
					Log.d("R", memlines[1]);
					
					try {
						String total[] = memlines[0].trim().split(" ");
						sc.memTotalkB = Long.parseLong(total[total.length-2]);
					
						String free[] = memlines[1].trim().split(" ");
						sc.memFreekB = Long.parseLong(free[free.length-2]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				
				String hdd = app.shellExec("df | head -n 2");
				if (hdd != null && !hdd.isEmpty()) {
					String hddlines[] = hdd.split("\n");
					Log.d("R", hddlines[1]);
					
					try {
						String chunks[] = hddlines[1].trim().split(" ");
						sc.hddUsed = Long.parseLong(chunks[chunks.length-7]);
						sc.hddTotal = Long.parseLong(chunks[chunks.length-8]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
	    	} catch (Exception e) {
	    		sc.error = "System stat failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
			return sc;
		}
		
		@Override
	    protected void onPostExecute(StatContainer result) {
			app.progressHide();
			
			if (result.error != null) { // error
				Toast.makeText(getApplicationContext(), result.error, Toast.LENGTH_LONG).show();
			} else {
				systats.setText(result.uptime);
				
				long memproc = (result.memTotalkB-result.memFreekB)*100/(result.memTotalkB+1);
				ramText.setText("" + result.memFreekB + "/" + result.memTotalkB + " (" + memproc + "%)");
				
				long hddproc = result.hddUsed * 100 / (result.hddTotal+1);
				hddText.setText("" + result.hddUsed + "/" + result.hddTotal + " (" + hddproc + "%)");
			}
	    }
	}
	
	public void onRefresh(View view) {
		new SystemStatsTask().execute();
	}
	
	// -------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stat, menu);
		return true;
	}
}
