package lt.tomasu.robotui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;

public class SensorData {
	
	private final int MAX_PTS = 1000;
	private int vals[] = new int[MAX_PTS];
	
	private Paint paint = new Paint();
	
	public SensorData(int color) {
		paint.setColor(color);
		paint.setStrokeWidth(1.5f);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.SQUARE);
		paint.setStyle(Style.FILL);
	}

	public void addPt(int value) {
		for (int i = 1; i < vals.length; i++) {
			vals[i - 1] = vals[i];
		}
		vals[vals.length - 1] = value;
	}
	
	public void draw(Canvas canvas) {
		float h = canvas.getHeight();
		
		// draw graph
		float dx = canvas.getWidth() / (float) vals.length;
		float fy = h / 360.0f;

		float prevy = h;
		for (int i = 1; i < vals.length; i++) {
			float cury = h - fy * vals[i];
			float curx = dx * i;
			canvas.drawLine(curx-dx, prevy, curx, cury, paint);
			prevy = cury;
		}
	}
}
