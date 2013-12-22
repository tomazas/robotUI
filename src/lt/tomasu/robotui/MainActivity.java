package lt.tomasu.robotui;

import java.util.Timer;
import java.util.TimerTask;

import lt.tomasu.ssh.client.AccelerometerData;
import lt.tomasu.ssh.client.GyroData;
import lt.tomasu.ssh.client.MagnetometerData;
import lt.tomasu.ssh.client.MotorStatus;
import lt.tomasu.ssh.client.ServoStatus;
import lt.tomasu.ssh.client.SonarStatus;
import lt.tomasu.ssh.client.SystemStatus;
import lt.tomasu.ssh.client.api.Controller;
import lt.tomasu.ssh.client.api.ControllerFactory;
import lt.tomasu.ssh.client.api.RobotShell;
import lt.tomasu.ssh.client.api.ShellFactory;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private MyApp app;
	
	private boolean connected = false;
	
	private Timer statusTimer;
	private Handler handler;
	
	private Button disconnectBtn;
	private Button connectBtn;
	private Button statsBtn;
	private Button driveBtn;
	private Button killBtn;
	private Button gyroBtn;
	
	private CheckBox servoChk;
	private CheckBox magChk;      
	private CheckBox gyroChk;    
	private CheckBox sonarChk;   
	private CheckBox accelChk;
	private CheckBox killChk;
	
	private TextView respBox;
	private TextView rttBox;
	private TextView connBox;
	
	private EditText ipbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        setContentView(R.layout.activity_main);
        setTitle("RobotUI");
        
        findViewById(R.id.main_activity).requestFocus(); // remove focus from editbox 
        
        app = (MyApp)getApplication();
        
        disconnectBtn = (Button)findViewById(R.id.disconnectBtn);
    	connectBtn = (Button)findViewById(R.id.connectBtn);
    	statsBtn = (Button)findViewById(R.id.statsBtn);
    	driveBtn = (Button)findViewById(R.id.driveBtn);
    	killBtn = (Button)findViewById(R.id.killBtn);
    	gyroBtn = (Button)findViewById(R.id.gyroBtn); 
    	
    	servoChk = (CheckBox)findViewById(R.id.servoChk);       
        magChk = (CheckBox)findViewById(R.id.magChk);      
        gyroChk = (CheckBox)findViewById(R.id.gyroChk);    
        sonarChk = (CheckBox)findViewById(R.id.sonarChk);   
        accelChk = (CheckBox)findViewById(R.id.accelChk);
        killChk = (CheckBox)findViewById(R.id.killChk);
        
        respBox = (TextView)findViewById(R.id.respBox);
        rttBox = (TextView)findViewById(R.id.rttBox);
        connBox = (TextView)findViewById(R.id.connBox);
        
        ipbox = (EditText)findViewById(R.id.ipBox);
        
        handler = new Handler();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onOpenDrivingActivity(View view) {
    	Intent intent = new Intent(this, DrivingActivity.class);
    	startActivity(intent);
    }
    
    public void onOpenStatisticsActivity(View view) {
    	Intent intent = new Intent(this, StatActivity.class);
    	startActivity(intent);
    }
    
    public void onOpenGyroDrive(View view) {
    	Intent intent = new Intent(this, GyroDriveActivity.class);
    	startActivity(intent);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	disableStatusTimer();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (connected) {
    		startStatusTimer();
    	}
    }
    
   // -----------------------------------------------------------------------------
    
    private class StatusContainer {
    	public String error;
    	@SuppressWarnings("unused")
		public MotorStatus motor;
    	public ServoStatus servo;
    	public MagnetometerData mag;
    	public GyroData gyro;
    	public SonarStatus sonar;
    	public AccelerometerData accel;
    	public SystemStatus sys;
    }
    
    private class RobotInitialStatusTask extends AsyncTask<Controller, Integer, StatusContainer> {
		@Override
		protected StatusContainer doInBackground(Controller... arg) {
			StatusContainer rs = new StatusContainer();
			try {
				// execute network queries on different thread!
				Controller controller = arg[0];
				rs.motor = controller.getMotorStatus();
				rs.servo = controller.getServoStatus();
		        rs.mag = controller.getMagnetometerData();
		        rs.gyro = controller.getGyroData();
		        rs.sonar = controller.getSonarStatus();
		        rs.accel = controller.getAccelerometerData();
		        rs.sys = controller.getSystemStatus();
	    	} catch (Exception e) {
	    		rs.error = "Status failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
			return rs;
		}
		
	    protected void onPostExecute(StatusContainer result) {
	    	app.progressHide();
	    	
	    	// disable/enable buttons
	    	if (result.error == null) { // all good
		        servoChk.setChecked(result.servo.isEnabled());
		        magChk.setChecked(result.mag.isEnabled());
		        gyroChk.setChecked(result.gyro.isEnabled());
		        sonarChk.setChecked(result.sonar.isEnabled());
		        accelChk.setChecked(result.accel.isEnabled());
		        killChk.setChecked(false);
		        
		        respBox.setText(result.sys.getProcessTimeMillis() + " us");
		        
		        // enable buttons
	    		connBox.setText("CONNECTED");
	    		connected = true;
	    		
	    		disconnectBtn.setEnabled(true);
	    		gyroBtn.setEnabled(true);
	    		statsBtn.setEnabled(true);
	    		driveBtn.setEnabled(true);
	    		connectBtn.setEnabled(false);
	    		killBtn.setEnabled(false);
	    		
	    		//servoChk.setEnabled(true); // disable, no need to turn it on/off
		        magChk.setEnabled(true);
		        gyroChk.setEnabled(true);
		        sonarChk.setEnabled(true);
		        accelChk.setEnabled(true);
		        killChk.setEnabled(true);
		        
	    		// we need periodical info, so run timer
	    		disableStatusTimer();
	    		startStatusTimer();

		        Toast.makeText(getApplicationContext(), "Status load complete", Toast.LENGTH_LONG).show();
	    	} else {
	    		connectBtn.setEnabled(true); // allow to re-connect again
	    		app.setShell(null);
	    		app.setController(null);
	    		
	    		// show error - if any
		    	Toast.makeText(getApplicationContext(), result.error, Toast.LENGTH_LONG).show();
	    	}
	    }
	}
    
    // -----------------------------------------------------------------------------
    
    private class SysContainer {
    	public long rtt;
    	public SystemStatus status;
    }
    
    private class RobotQuerySystemStatusTask extends AsyncTask<Controller, Integer, SysContainer> {
		@Override
		protected SysContainer doInBackground(Controller... arg) {
			SysContainer sc = new SysContainer();
			try {
				Controller controller = arg[0];
				if (controller != null) {
					long startT = System.nanoTime();
					sc.status = controller.getSystemStatus();
					sc.rtt = System.nanoTime()-startT;
				}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
			return sc;
		}
		
		@Override
	    protected void onPostExecute(SysContainer result) {
	    	if (result.status != null) { // valid result received, show response time in textview
    	        respBox.setText(result.status.getProcessTimeMillis() + " us");
    	        rttBox.setText(result.rtt/1000000 + " ms"); // convert from nano to ms
	    	} else {
	    		// error
	    		respBox.setText("- us");
    	        rttBox.setText("- ms");
	    	}
	    }
	}
    
    // -----------------------------------------------------------------------------
    
    private void disableStatusTimer() {
    	// ensure old timer is stopped
		if (statusTimer != null) {
			statusTimer.cancel();
			statusTimer = null;
		}
    }
    
    private void startStatusTimer() {
    	// create a periodic query timer
		statusTimer = new Timer(true);
		statusTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// runnable will be executed in main thread
				handler.post(new Runnable() {
			        public void run() {
			        	// run network actions in other thread
						new RobotQuerySystemStatusTask().execute(app.getController());
	                }
	            });
				
			}
		}, 0, AppConfig.QUERY_TIMER_PERIOD);
    }
   
    // -----------------------------------------------------------------------------
    
	private class RobotLoginTask extends AsyncTask<Object, Integer, String> {
		@Override
	    protected void onPreExecute() {
			// disable buttons so user can't launch other connect command
			connectBtn.setEnabled(false);
			app.progressShow(MainActivity.this, "Connecting", null);
		}
		
		@Override
		protected String doInBackground(Object... arg) {
			try {
				connected = false;
				String ip = (String)arg[1];
				int port = (Integer)arg[2];
				app.setIp(ip);
				
				// connect to robot controller server
				RobotShell sh = (RobotShell)arg[0];
	    		sh.login(ip, port, (Integer)arg[3], (Integer)arg[4]);
	    		return null; // no error
	    	} catch (Exception e) {
	    		return "Connect failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}

		@Override
	    protected void onPostExecute(String result) {
	    	if (result == null) { 
	    		// success - no error
	    		result = "Connection successful";
	    		
	    		// query initial robot status
	    		new RobotInitialStatusTask().execute(app.getController());
	    	} else {
	    		// failure
	    		app.setShell(null);
	    		app.setController(null);
	    		app.progressHide();
	    		
	    		connectBtn.setEnabled(true); // allow to re-connect again
	    	}
	    	
	    	Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}

    /**
     * @param view
     */
    public void onConnectRobot(View view) {
    	app.setShell(ShellFactory.create());
    	app.setController(ControllerFactory.create(app.getShell()));
    	
    	String ipaddr[] = ipbox.getText().toString().trim().split(":");
    	
    	// parse port value
    	int port = AppConfig.DEFAULT_PORT;
    	try {
    		if (ipaddr.length > 1) { // port is optional
    			port = Integer.parseInt(ipaddr[1]);
    		}
    	} catch (NumberFormatException e) { 
    		Toast.makeText(getApplicationContext(), "Invalid port number, using default: " + port, Toast.LENGTH_LONG).show();
    	}
    	
    	if (ipaddr.length > 0 && !ipaddr[0].isEmpty()) {
    		// run network actions in other thread
        	new RobotLoginTask().execute(app.getShell(), ipaddr[0], port, 
        			AppConfig.CONNECT_TIMEOUT, AppConfig.SOCKET_TIMEOUT);
    	} else {
    		Toast.makeText(getApplicationContext(), "IP address not valid", Toast.LENGTH_LONG).show();
    	}
    }
    
    // -----------------------------------------------------------------------------
    
    void disableButtons() {
    	// disable all buttons before executing disconnect so user can't do anything!
		disconnectBtn.setEnabled(false);
		gyroBtn.setEnabled(false);
		statsBtn.setEnabled(false);
		driveBtn.setEnabled(false);
		killBtn.setEnabled(false);
		
		//servoChk.setEnabled(false); // disabled, no need to turn it on/off 
        magChk.setEnabled(false);
        gyroChk.setEnabled(false);
        sonarChk.setEnabled(false);
        accelChk.setEnabled(false);
        killChk.setEnabled(false);
    }
    
    private class RobotDisconnectTask extends AsyncTask<Object, Integer, String> {
    	
    	@Override
    	protected void onPreExecute() {
    		disableButtons();
    		app.progressShow(MainActivity.this, "Disconnecting", null);
    	}
    	
		@Override
		protected String doInBackground(Object... arg) {
			try {
				// ensure robot is stopped before disconnect
				Controller c = (Controller)arg[0];
				c.setServoStatus(false, AppConfig.SERVO_DEFAULT_VALUE);
				c.setMotorStatus(AppConfig.MOTOR_DEFAULT_VALUE, AppConfig.MOTOR_DEFAULT_VALUE);
				
				// disconnect
				RobotShell sh = (RobotShell)arg[1];
				if (sh != null) {
					sh.logout();
				}
				return null;
	    	} catch (Exception e) {
	    		return "Disconnect failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			// enable buttons
			app.progressHide();
	    	connBox.setText("DISCONNECTED");
			connectBtn.setEnabled(true);
			
			if (result == null) {	
				Toast.makeText(getApplicationContext(), "Disconnect successful", Toast.LENGTH_LONG).show();			
			} else {
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
			
			disableStatusTimer();
			
			app.setShell(null);
			app.setController(null);
	    }
	}
    
    public void onDisconnectRobot(View view) {
    	new RobotDisconnectTask().execute(app.getController(), app.getShell());
    }
    
    // -----------------------------------------------------------------------------
    
    private class RobotShutdownTask extends AsyncTask<Object, Integer, String> {
    	
    	@Override
    	protected void onPreExecute() {
    		disableButtons();
    		app.progressShow(MainActivity.this, "Shutting down", null);
    	}
    	
		@Override
		protected String doInBackground(Object... arg) {
			try {
				String ip = (String)arg[0];
				app.setIp(ip);
				
				app.shellExec(AppConfig.ROBOT_KILL_CMD);
				return null;
	    	} catch (Exception e) {
	    		return "Shutdown failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			// enable buttons
			app.progressHide();
	    	connBox.setText("SHUTDOWN");
			
			if (result == null) {	
				Toast.makeText(getApplicationContext(), "Shutdown started", Toast.LENGTH_LONG).show();			
			} else {
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
			
			disableStatusTimer();
			
			app.setShell(null);
			app.setController(null);
	    }
	}
    
    public void onKill(View view) {
    	// create out AlterDialog
        Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Robot hardware will be shutdown, are you sure?");
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String ipaddr[] = ipbox.getText().toString().trim().split(":");
				
				if (ipaddr.length > 0 && !ipaddr[0].isEmpty()) {
					new RobotShutdownTask().execute(ipaddr[0]);
				} else {
					Toast.makeText(getApplicationContext(), "IP address not valid", Toast.LENGTH_LONG).show();
				}
			}
		});
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    public void onShutdownFlip(View view) {
    	killBtn.setEnabled(killChk.isChecked());
    }
    
    // -----------------------------------------------------------------------------
    
    private class RobotSonarToggle extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				Boolean value = (Boolean)arg[1];
				controller.toggleSonar(value);
				return "Sonar " + (value ? "enabled" : "disabled");
	    	} catch (Exception e) {
	    		return "Sonar failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			app.progressHide();
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}
    
    public void sonarFlip(View view) {
    	if (app.getController() != null) {
    		app.progressShow(MainActivity.this, "Updating", null);
    		new RobotSonarToggle().execute(app.getController(), sonarChk.isChecked());
    	}
    }
    
    // -----------------------------------------------------------------------------
    
    private class RobotMagToggle extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				Boolean value = (Boolean)arg[1];
				controller.toggleMagnetometer(value);
				return "Magnetometer " + (value ? "enabled" : "disabled");
	    	} catch (Exception e) {
	    		return "Magnetometer failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			app.progressHide();
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}
    
    public void magFlip(View view) {
		if (app.getController() != null) {
			app.progressShow(MainActivity.this, "Updating", null);
			new RobotMagToggle().execute(app.getController(), magChk.isChecked());
		}
    }
    
    // -----------------------------------------------------------------------------
    
    private class RobotGyroToggle extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				Boolean value = (Boolean)arg[1];
				controller.toggleGyro(value);
				return "Gyro " + (value ? "enabled" : "disabled");
	    	} catch (Exception e) {
	    		return "Gyro failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			app.progressHide();
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}
    
    public void gyroFlip(View view) {
		if (app.getController() != null) {
			app.progressShow(MainActivity.this, "Updating", null);
			new RobotGyroToggle().execute(app.getController(), gyroChk.isChecked());
		}
    }
    
    // -----------------------------------------------------------------------------
    
    private class RobotAccelToggle extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				Boolean value = (Boolean)arg[1];
				controller.toggleAccelerometer(value);
				return "Accelerometer " + (value ? "enabled" : "disabled");
	    	} catch (Exception e) {
	    		return "Accelerometer failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			app.progressHide();
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}
    
    public void accelFlip(View view) {
		if (app.getController() != null) {
			app.progressShow(MainActivity.this, "Updating", null);
			new RobotAccelToggle().execute(app.getController(), accelChk.isChecked());
		}
    }
    
    // -----------------------------------------------------------------------------
    // NOTE: disabled, no need to turn it on/off
    
    /*private class RobotServoToggle extends AsyncTask<Object, Integer, String> {
		@Override
		protected String doInBackground(Object... arg) {
			try {
				Controller controller = (Controller)arg[0];
				Boolean value = (Boolean)arg[1];
				controller.setServoStatus(value, SERVO_DEFAULT_VALUE);
				return "Servo " + (value ? "enabled" : "disabled");
	    	} catch (Exception e) {
	    		return "Servo failure: " + e.getClass().getSimpleName() + " " + e.getMessage();
	    	}
		}
		
		@Override
	    protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
	    }
	}*/
    
    public void servoFlip(View view) {
		/*if (app.getController() != null) {
			new RobotServoToggle().execute(app.getController(), servoChk.isChecked());
		}*/
    }
    
}
