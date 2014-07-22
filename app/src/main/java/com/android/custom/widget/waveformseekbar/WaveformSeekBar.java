package com.android.custom.widget.waveformseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import com.example.davor.testapp2.R;
import com.android.custom.widget.waveformseekbar.wav.WavFile;
import com.android.custom.widget.waveformseekbar.wav.WavFileException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Created by dsibenik on 7/4/14.
 */
public class WaveformSeekBar extends SeekBar {

    public static final int READ_FRAME_COUNT = 100;

    private InputStream inputStream;
    private Vector<Float> yAxis = new Vector<Float>();
    private boolean firstDraw = true;
    private int activeColor;
    private int inactiveColor;
    private int transparencyAtrb;

    public WaveformSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
    }

    public WaveformSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WaveformSeekBar,
                0, 0);

        try {
            activeColor = a.getInteger(R.styleable.WaveformSeekBar_activeLineColor, 0);
            inactiveColor = a.getInteger(R.styleable.WaveformSeekBar_inactiveLineColor, 0);
            transparencyAtrb = a.getInteger(R.styleable.WaveformSeekBar_inactiveLineColorAlpha, 0);

            if( transparencyAtrb > 100 ){
                transparencyAtrb = 255;
            }else {
                transparencyAtrb = (int) ((transparencyAtrb/100.0)*(255));
            }

        } finally {
            a.recycle();
        }

        if( inactiveColor == 0 && transparencyAtrb == 0 ) {
            inactiveColor = adjustAlpha(activeColor, 200);
        } else if ( inactiveColor == 0 && transparencyAtrb != 0) {
            inactiveColor = adjustAlpha(activeColor, transparencyAtrb);
        } else if ( inactiveColor != 0 && transparencyAtrb != 0) {
            inactiveColor = adjustAlpha(inactiveColor, transparencyAtrb);
        }
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }


    public void setAudio(InputStream intStream) {
        this.inputStream = intStream;
        invalidate();
    }

    private class CalculateYAxisPoints extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void[] params) {
            try {
                readWavFile();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (WavFileException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //getting x-axis of the clicked point
        float clickedX = (float) (getMeasuredWidth() * ((double) getProgress() / getMax()));

        if (firstDraw) {
            firstDraw = false;
            new CalculateYAxisPoints().execute();
        } else {
            Paint paintPath = new Paint();
            paintPath.setStyle(Paint.Style.STROKE);
            paintPath.setAntiAlias(true);

            //drawing graphs depending on the clicked point (clickedX)
            int closestPoint = 0;

            //closest int - possible optimisations floor or casting to int, but isn't as accurate
            for (int j = 0; j <= getMeasuredWidth(); j += 1) {
                if (Math.abs(j - clickedX) < Math.abs(closestPoint - clickedX)) closestPoint = j;
            }

            //draw from 0 to clickedX
            drawPath(0, closestPoint, activeColor, paintPath, canvas);

            //draw from clickedX to end
            drawPath(closestPoint, getMeasuredWidth(), inactiveColor, paintPath, canvas);

        }

    }   //end of onDraw


    /**
     * Draw a path from start to end using yAxis array.
     *
     * @param start
     * @param end
     * @param color
     * @param paintPath
     * @param canvas
     */
    private void drawPath(int start, int end, int color, Paint paintPath, Canvas canvas) {

        Path path = new Path();
        int viewHeightHalf = this.getMeasuredHeight() / 2;

        for (; start <= end; start += 1) {
            if (start >= yAxis.size()) break;

            if ( Math.floor( yAxis.get(start) ) >= 1 ) {
                path.moveTo(start, viewHeightHalf - (yAxis.get(start)));
                path.lineTo(start, viewHeightHalf + (yAxis.get(start)));

            } else {          // making it visible if very low value (thicker line)
                path.moveTo(start, (float) (viewHeightHalf * 1.01));
                path.lineTo(start, (float) (viewHeightHalf * 0.99));
            }
        }

        path.close();
        paintPath.setColor(color);
        canvas.drawPath(path, paintPath);
        path.reset();
    }


    private void readWavFile() throws IOException, WavFileException {

        // Open the wav file specified as the first argument
        WavFile wavFile = WavFile.openWavFile(inputStream);

        // Get the number of audio channels in the wav file
        int numChannels = wavFile.getNumChannels();

        // Create a buffer of 100 frames
        double[] buffer = new double[READ_FRAME_COUNT * numChannels];

        int framesRead;
        int samplesPerPixel = (int) (wavFile.getNumFrames() * numChannels) / (getMeasuredWidth());

        double avrg = 0;
        int overallCounter = 0;
        int pointsInAvrg = 0;
        float yPoint = 0;                    //a single value on y-axis
        double max = Double.MIN_VALUE;       //global max
        double maxLoc = Double.MIN_VALUE;    //local max

        do {
            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, READ_FRAME_COUNT);

            // Loop through frames and store yAxis values
            if (framesRead != 1) {
                for (int s = 0; s < framesRead * numChannels; s += 1) {
                    if (buffer[s] > maxLoc) maxLoc = buffer[s];

                    //get max out of every 256
                    if (overallCounter % 256 == 0) {
                        avrg += Math.abs(maxLoc);
                        maxLoc = Double.MIN_VALUE;
                        pointsInAvrg++;
                    }
                    //average of maximums from samplesPerPixel
                    if (overallCounter % samplesPerPixel == 0) {

                        if (pointsInAvrg == 0)
                            yAxis.add((float) 0);
                        else {
                            yPoint = (float) (avrg / pointsInAvrg);
                            yAxis.add(yPoint);
                            if (yPoint > max) max = (yPoint);
                            pointsInAvrg = 0;
                            avrg = 0;
                        }
                    }
                    overallCounter++;
                }
            }

        } while (framesRead != 0);

        //ratio relative to the screensize and padding
        double ratio = ((this.getMeasuredHeight() / 2) - getPaddingTop() - getPaddingBottom() / max);

        //normalizing the values relative to the ratio
        for (overallCounter = 0; overallCounter < yAxis.size(); overallCounter++) {
            yAxis.set(overallCounter, (float) (yAxis.get(overallCounter) * ratio));
        }

        // Close the wavFile
        wavFile.close();
    }

}


