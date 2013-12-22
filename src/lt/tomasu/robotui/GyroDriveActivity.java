package lt.tomasu.robotui;

import lt.tomasu.ssh.client.api.Controller;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

class EMA { // exponential moving average
    private float alpha;
    private Float oldValue;
    
    public EMA(float alpha) {
        this.alpha = alpha;
    }

    public float average(float value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        oldValue = oldValue + alpha * (value - oldValue);
        return oldValue;
    }
    
    public float get() {
    	return oldValue;
    }
}

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class GyroDriveActivity extends Activity implements SensorEventListener {
	
	private MyApp app;
	
	private SensorManager sensorManager;	
	private EMA stable = new EMA(0.005f);
	
	private TextView xcoord, ycoord, zcoord, textMotorSpeed; 
	private Drawer canvas;
	private Button forwardBtn, reverseBtn;
	
	private SensorData raw_sd, robo_sd, peak_sd, ctrl_sd;
	private SeekBar speedBar = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setTitle("Gyro Drive");
		
		setContentView(R.layout.activity_gyro_drive);
		
		app = (MyApp)getApplication();
		
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		setupSensors();
        
        xcoord = (TextView)findViewById(R.id.xcoord);
        ycoord = (TextView)findViewById(R.id.ycoord);
        zcoord = (TextView)findViewById(R.id.zcoord);
        
        canvas = (Drawer)findViewById(R.id.surface);
        canvas.setZOrderOnTop(true);  
        canvas.startThread();
        
        textMotorSpeed = (TextView)findViewById(R.id.textMotorSpeed);
        speedBar = (SeekBar)findViewById(R.id.gyroSpeed);
        textMotorSpeed.setText("" + speedBar.getProgress());
        
        speedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            	// nothing
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            	// nothing
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            	textMotorSpeed.setText("" + speedBar.getProgress());
            }
	    });
        
        //
        forwardBtn = (Button)findViewById(R.id.gyroBtn);
        forwardBtn.setOnTouchListener(
			new OnTouchListener() {
	            @Override
	            public boolean onTouch(View v, MotionEvent event) {
	                switch (event.getAction()) {
	                	case MotionEvent.ACTION_DOWN:
	                		new RobotMotorUpdateTask().execute(app.getController(), 0, speedBar.getProgress());
	                		return false;
	                	case MotionEvent.ACTION_UP:
	                		new RobotMotorUpdateTask().execute(app.getController(), 0, 0);
	                		return false;
	                	default:
	                		return false;
	                }
	            }
	        });
		
        reverseBtn = (Button)findViewById(R.id.driveBackBtn);
        reverseBtn.setOnTouchListener(
			new OnTouchListener() {
	            @Override
	            public boolean onTouch(View v, MotionEvent event) {
	                switch (event.getAction()) {
	                	case MotionEvent.ACTION_DOWN:
	                		new RobotMotorUpdateTask().execute(app.getController(), speedBar.getProgress(), 0);
	                		return false;
	                	case MotionEvent.ACTION_UP:
	                		new RobotMotorUpdateTask().execute(app.getController(), 0, 0);
	                		return false;
	                	default:
	                		return false;
	                }
	            }
			});
		
        
        raw_sd = new SensorData(Color.RED);
        robo_sd = new SensorData(Color.BLUE);
        peak_sd = new SensorData(Color.MAGENTA);
        ctrl_sd = new SensorData(Color.GREEN);
        
        canvas.addData(raw_sd);
        canvas.addData(ctrl_sd);
        canvas.addData(robo_sd);
        canvas.addData(peak_sd);
	}
	
	final int LEFT = 0;
	final int RIGHT = 360;
	final int IDLE = 180;
	
	class StableInt {
		private int stability;
		private int value;
		private int candidateValue;
		private long last_time;
		
		public void init(int initialValue, int timeStability) { 
			value = initialValue;
			candidateValue = value;
			stability = timeStability;
			last_time = 0;
		}
		
		public void update(int newValue) {
			if (newValue != value) {
				if (last_time == 0) { // initiate change
					last_time = System.currentTimeMillis();
					candidateValue = newValue;
				} else {
					// timer initiated
					if (newValue != candidateValue) { // re-initiate timer as values differ
						last_time = System.currentTimeMillis();
						candidateValue = newValue;
					} else {
						// values still match, check if stability period ended
						if (System.currentTimeMillis() - last_time >= stability) {
							// change success
							value = candidateValue;
							last_time = 0;
						}
					}
				}
			}
		}
		
		public int get() {
			if (last_time != 0) { // timer initiated
				if (System.currentTimeMillis() - last_time >= stability) { // change success
					value = candidateValue;
					last_time = 0;
				}
			}
			return value;
		}
	
	}
	
	static final int ANGLE_DECR = -1;
	static final int ANGLE_INCR = 1;
	static final int NONE = 0;

	float yaw = 0;
	
	boolean rotating = false;
	float prev_yaw = 0;
	StableInt prev_dir = new StableInt();
	StableInt over_thresh = new StableInt();
	int start_dir = NONE;
	boolean afterpeak = false;
	boolean firstInit = true;
	float peak = 0;
	
	final float SPIKE_FILTER = 15;   // degrees
	final int DIR_STABILITY = 250;   // in ms
	final int SPIKE_STABILITY = 300; // in ms
	
	private int getDriveState() {		
		float delta = yaw - stable.get();
		boolean lowpass = (Math.abs(delta) > SPIKE_FILTER); // filter small spikes
		
		// apply stability for smoothing shaking
		if (firstInit) {
			firstInit = false;
			over_thresh.init(lowpass ? 1 : 0, SPIKE_STABILITY);
		} else {
			over_thresh.update(lowpass ? 1 : 0);
		}
		
		// current rotation direction
		int curr_dir = (yaw - prev_yaw) > 0 ? ANGLE_INCR : ANGLE_DECR;
		prev_yaw = yaw;
		
		// check if we're started rotating
		boolean ok = (over_thresh.get() > 0) ? true : false;
		if (rotating != ok) { // (de-)activation 
			if (!rotating) { // first time init
				start_dir = curr_dir;
				
				if (start_dir == ANGLE_INCR) { 
					//  rotating left
					peak = 0;
					new RobotServoUpdateTask().execute(app.getController(), true, 180);
				} else {
					// rotating right
					peak = 360;
					new RobotServoUpdateTask().execute(app.getController(), true, 0);
				}
				
				afterpeak = false;
				prev_dir.init(curr_dir, DIR_STABILITY);
			}
			rotating = ok;
			
			if (!rotating) { // stop rotation
				new RobotServoUpdateTask().execute(
        				app.getController(), false, AppConfig.SERVO_DEFAULT_VALUE);
			}
		}
		
		if (rotating) { // over the average - still rotating
			prev_dir.update(curr_dir);
			
			
			if (prev_dir.get() == start_dir && !afterpeak) { // rotation is increasing, not falling
				if (start_dir == ANGLE_INCR) {
					peak = Math.max(peak, yaw);
				} else {
					peak = Math.min(peak,  yaw);
				}
			} else {
				// rotation end - falling
				afterpeak = true;
				//return IDLE;
			}
			
			if (start_dir == ANGLE_INCR) {
				return LEFT;
			} else {
				return RIGHT;
			}
			
		}
		
		return IDLE;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		sensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		sensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		setupSensors();
	}
	
	void setupSensors() {
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gyro_drive, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// nothing
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    // nothing
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
        	float[] orientationVals = new float[3];
        	float[] mRotationMatrix = new float[16];
        	
        	SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);
            
            // smooth out noise (to receive in range: 0-360 remove abs and add PI)
            yaw   = (float)Math.toDegrees(Math.abs(orientationVals[0]));// + Math.PI);
            float pitch = (float)Math.toDegrees(Math.abs(orientationVals[1]));//+ Math.PI);
            float roll  = (float)Math.toDegrees(Math.abs(orientationVals[2]));// + Math.PI);
            
            // graphics
            raw_sd.addPt((int)yaw);
            //filtered_sd.addPt((int)filtered.average(yaw));
            ctrl_sd.addPt((int)stable.average(yaw));
            robo_sd.addPt(getDriveState());
            peak_sd.addPt((int) peak);

            xcoord.setText(""+(int)yaw);
			ycoord.setText(""+(int)pitch);
			zcoord.setText(""+(int)roll);
        }
	}
	
	private class RobotMotorUpdateTask extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				if (controller != null) {
					Integer front = (Integer)arg[1];
					Integer rear = (Integer)arg[2];
					controller.setMotorStatus(front, rear);
				}
				return null;
	    	} catch (Exception e) {
	    		return "Motor failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			if (result != null) { // error
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
	    }
	}
	
	// -----------------------------------------------------------------------------
	
	private class RobotServoUpdateTask extends AsyncTask<Object, Integer, String> {
		private Boolean ena;
		private Integer value;
		
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				if (controller != null) {
					ena = (Boolean)arg[1];
					value = (Integer)arg[2];
					controller.setServoStatus(ena, value);
				}
				return null;
	    	} catch (Exception e) {
	    		return "Servo failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			if (result != null) { // no error
				// show error - if any
		    	Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
	    }
	}
}
