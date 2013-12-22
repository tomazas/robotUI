package lt.tomasu.robotui;

public class AppConfig {
	public static final int DEFAULT_PORT = 1337;
	
	public final static int SERVO_RESET_TIMEOUT = 3000;
	public final static int MOTOR_RESET_TIMEOUT = 1000;
	
	public final static int CONNECT_TIMEOUT = 3000;
	public final static int SOCKET_TIMEOUT = 5000;
	public final static int QUERY_TIMER_PERIOD = 1000; // in ms
	public final static int SERVO_DEFAULT_VALUE = 90;  // in degrees
	public final static int MOTOR_DEFAULT_VALUE = 0;
	public final static long MOTOR_WDT_TIMER_PERIOD = 1000;
	
	public static final String ROBOT_SSH_USER = "root";
	public static final String ROBOT_SSH_PWD = "root";
	public static final String ROBOT_SSH_FINGERPRINT = "d1:eb:4a:f9:0a:93:bc:88:53:48:e3:83:55:a0:dc:d0";
	public static final int ROBOT_SSH_CMD_TIMEOUT = 5000;
	public static final int ROBOT_SSH_SOCKET_TIMEOUT = 5000;
	public static final int ROBOT_SSH_CONNECT_TIMEOUT = 10000;
	public static final String ROBOT_KILL_CMD = "shutdown -h now";
}
