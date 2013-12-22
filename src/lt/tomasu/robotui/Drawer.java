package lt.tomasu.robotui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Drawer extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	

	private Paint bgpaint = new Paint();
	
	private Thread thread; 
	private boolean running = true;
	
	private List<SensorData> data = new ArrayList<SensorData>();

	public Drawer(Context context) {
		super(context);
		initialize();
	}

	public Drawer(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public Drawer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {
		getHolder().addCallback(this);
		getHolder().setFormat(PixelFormat.TRANSPARENT);
		setFocusable(false);

		bgpaint.setColor(Color.WHITE);
	}
	
	public void addData(SensorData sd) {
		data.add(sd);
	}
	
	public void onResume() {
		startThread();
	}

	public void onPause() {
		stopThread();
	}

	public void startThread() {
		// restart painter thread
		running = true;
		thread = new Thread(this);
		thread.start();
	}
	
	private void stopThread() {
		boolean retry = true;
		running = false;
		while (retry) {
			try {
				// wait for thread to finish
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// clear canvas
		//int w = canvas.getWidth();
		//int h = canvas.getHeight();
		
		canvas.drawColor( 0, PorterDuff.Mode.CLEAR);
		//bgpaint.setAlpha(0);
		//canvas.drawRect(0, 0, w, h, bgpaint);
		
		for (SensorData sd: data) {
			sd.draw(canvas);
		}
	}


	@Override
	public void run() {
		Canvas canvas = null;
		SurfaceHolder holder = getHolder();
		
		while (running) {
			synchronized (holder) {
				canvas = holder.lockCanvas(null);
				if (canvas != null) {
					onDraw(canvas);
					holder.unlockCanvasAndPost(canvas);
				}
			}
			
			try {
				Thread.sleep(1000/30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

}