package lt.tomasu.robotui;

import lt.tomasu.ssh.client.api.Controller;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class DrivingActivity extends Activity {
	
	private SeekBar speedBar = null;
	private TextView speedBox = null;
	
	private MyApp app;
	private Button fwdBtn;
	private Button revBtn;
	private Button leftBtn;
	private Button rightBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_driving);
		setTitle("Drive Mode");
		setupActionBar();
		
		app = (MyApp)getApplication();

		speedBar = (SeekBar)findViewById(R.id.seekbar_speed);
		
        speedBox = (TextView)findViewById(R.id.motorPwr);
        speedBox.setText("" + speedBar.getProgress());
        
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
            	speedBox.setText("" + speedBar.getProgress());
            }
	    });
        
		fwdBtn = (Button)findViewById(R.id.fwdBtn);
		fwdBtn.setOnTouchListener(
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
		
		revBtn = (Button)findViewById(R.id.revBtn);
		revBtn.setOnTouchListener(
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
		
		leftBtn = (Button)findViewById(R.id.leftBtn);
		leftBtn.setOnTouchListener(
			new OnTouchListener() {
	            @Override
	            public boolean onTouch(View v, MotionEvent event) {
	                switch (event.getAction()) {
	                	case MotionEvent.ACTION_DOWN:
	                		new RobotServoUpdateTask().execute(app.getController(), true, 180);
	                		return false;
	                	case MotionEvent.ACTION_UP:
	                		new RobotServoUpdateTask().execute(
	                				app.getController(), false, AppConfig.SERVO_DEFAULT_VALUE);
	                		return false;
	                	default:
	                		return false;
	                }
	            }
			});
		
		rightBtn = (Button)findViewById(R.id.rightBtn);
		rightBtn.setOnTouchListener(
			new OnTouchListener() {
	            @Override
	            public boolean onTouch(View v, MotionEvent event) {
	                switch (event.getAction()) {
	                	case MotionEvent.ACTION_DOWN:
	                		new RobotServoUpdateTask().execute(app.getController(), true, 0);
	                		return false;
	                	case MotionEvent.ACTION_UP:
	                		new RobotServoUpdateTask().execute(
	                				app.getController(), false, AppConfig.SERVO_DEFAULT_VALUE);
	                		return false;
	                	default:
	                		return false;
	                }
	            }
			});
	}
	
	// -----------------------------------------------------------------------------
	
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
	
	// -----------------------------------------------------------------------------
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.driving, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
