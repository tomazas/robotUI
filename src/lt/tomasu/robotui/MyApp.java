package lt.tomasu.robotui;

import java.io.IOException;

import lt.tomasu.ssh.client.SecureShell;
import lt.tomasu.ssh.client.api.Controller;
import lt.tomasu.ssh.client.api.RobotShell;
import lt.tomasu.ssh.client.api.ShellFactory;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;

public class MyApp extends Application {
	// global class that exists for whole app lifetime use getApplication() from activity
	private ProgressDialog pd;
	
	private RobotShell shell;
	private Controller controller;
	private String ip;
	
	public RobotShell getShell() {
		return shell;
	}
	public void setShell(RobotShell shell) {
		this.shell = shell;
	}
	public Controller getController() {
		return controller;
	}
	public void setController(Controller controller) {
		this.controller = controller;
	}
	
	public String shellExec(String command) throws IOException {
		SecureShell sh = ShellFactory.createSecure();
		sh.setTimeouts(AppConfig.ROBOT_SSH_CONNECT_TIMEOUT, AppConfig.ROBOT_SSH_SOCKET_TIMEOUT);
		sh.connect(ip, AppConfig.ROBOT_SSH_USER, AppConfig.ROBOT_SSH_PWD, AppConfig.ROBOT_SSH_FINGERPRINT);
		String result = sh.exec(command, AppConfig.ROBOT_SSH_CMD_TIMEOUT);
		sh.close();
		
		return result;
	}
	
	public void progressShow(Context context, String title, String details) {
		pd = ProgressDialog.show(context, title, details, true, false, null);
	}
	
	public void progressHide() {
		pd.dismiss();
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
}
