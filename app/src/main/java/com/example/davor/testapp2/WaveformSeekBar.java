package com.example.davor.testapp2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import com.library1.WavFileTmp;
import com.library1.WavFileException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Created by davor on 7/4/14.
 */
public class WaveformSeekBar extends SeekBar {
    private static final String LOG_TAG = WaveformSeekBar.class.getName();
    public static final int READ_FRAME_COUNT = 100;
    private int graphCol, labelCol;
    private String graphLabel;
    private int audioLenght = 60;
    private Vector<Float> yAxis = new Vector<Float>();
    private int numOfPoints = 1000;
    private boolean firstDraw = true;

    public WaveformSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);


        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WaveformSeekBar,
                0, 0);

        try {
            //mShowText = a.getBoolean(R.styleable.GraphView_showText, false);
            //mTextPos = a.getInteger(R.styleable.GraphView_labelPosition, 0);
            graphCol = a.getInteger(R.styleable.WaveformSeekBar_graphColor, 0);
            labelCol = a.getInteger(R.styleable.WaveformSeekBar_labelColor, 0);
            graphLabel = a.getString(R.styleable.WaveformSeekBar_graphLabel);

        } finally {
            a.recycle();
        }
    }


//    private File file;
//
//    public void setFile(File file) {
//        this.file = file;
//        invalidate();
//    }


    private Context context;
    public void getContext1(Context context){
        this.context = context;
    }
//    private void setFile1() {
//        file = new File(context.getExternalFilesDir(null), "audio.wav");
//        invalidate();
//    }


    private class AsyncTask1 extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void[] params) {
            try {
                readWavFile(context);
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

        numOfPoints = this.getMeasuredWidth();

        float clickedX = 0;
        float closestpoint = 0;
        float x;
        int i = 0;
        int viewWidthHalf = this.getMeasuredWidth() / 2;
        int viewHeightHalf = this.getMeasuredHeight() / 2;

        clickedX = (float) (this.getMeasuredWidth() * ((double) getProgress() / getMax()));

        Paint paintPath = new Paint();
        Path path = new Path();
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setAntiAlias(true);

        //Paint graphPaint = new Paint();
        //graphPaint.setStyle(Paint.Style.FILL);
        //graphPaint.setColor(Color.parseColor("#B9D770")); //green
        //graphPaint.setColor(0xff000000); //black
        //graphPaint.setColor(0xcc00ffff); //semitransp black
        //graphPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        //canvas.drawRect(0, 0, 2 * viewWidthHalf, 2 * viewHeightHalf, graphPaint);

//        graphPaint.setColor(labelCol);
//        graphPaint.setTextAlign(Paint.Align.CENTER);
//        graphPaint.setTextSize(50);
//        canvas.drawText( ((double)getProgress()/getMax())+"", viewWidthHalf, viewHeightHalf, graphPaint );


        if (firstDraw == true) {
            firstDraw = false;

            new AsyncTask1().execute();

        } else {
            float k = clickedX;
            for (float j = 0; j <= numOfPoints; j += 1)
                if (Math.abs(j - k) < Math.abs(closestpoint - k)) closestpoint = j;

            for (x = 0, i = 0; x <= closestpoint; x += 1, i++) {
                if (i >= yAxis.size()) break;

                if (yAxis.get(i) != 0) {
                    path.moveTo(x, viewHeightHalf - (yAxis.get(i)));
                    path.lineTo(x, viewHeightHalf + (yAxis.get(i)));
                } else {
                    path.moveTo(x, (float) (viewHeightHalf * 1.01));
                    path.lineTo(x, (float) (viewHeightHalf * 0.99));
                }
            }
            path.close();
            paintPath.setColor(0xffffffff);
            canvas.drawPath(path, paintPath);
            path.reset();
        }

        if (i != 0) i--;
        for (x = closestpoint; x <= numOfPoints; x += 1, i++) {
            if (i >= yAxis.size()) break;

            if (yAxis.get(i) != 0) {
                path.moveTo(x, viewHeightHalf - (yAxis.get(i)));
                path.lineTo(x, viewHeightHalf + (yAxis.get(i)));
            } else {
                path.moveTo(x, (float) (viewHeightHalf * 1.01));
                path.lineTo(x, (float) (viewHeightHalf * 0.99));
            }
        }
        path.close();
        paintPath.setColor(0x88ffffff);
        canvas.drawPath(path, paintPath);
        path.reset();
    }   //End of OnDraw


    private void readWavFile(Context contextMain) throws IOException, WavFileException {

        //String file = "res/raw/test.txt";
        //InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
        FileInputStream in;
        in = contextMain.getAssets().open("audio.wav");
        contextMain.getAssets().
        File file2 = new File("assets/audio.wav");
        File testfile = new File(contextMain.getAssets().);
        //in = contextMain.getClass().getClassLoader().getResources("/src/main/assets/audio.wav"); //dobijem input stream

        long startms = System.currentTimeMillis();

        // Open the wav file specified as the first argument
        WavFileTmp wavFile = WavFileTmp.openWavFile(audioFile);

        // Get the number of audio channels in the wav file
        int numChannels = wavFile.getNumChannels();

        // Create a buffer of 100 frames
        double[] buffer = new double[READ_FRAME_COUNT * numChannels];

        int framesRead;
        int samplesPerPixel = (int) (wavFile.getNumFrames() * numChannels) / (numOfPoints);


        double avrg = 0;
        int k = 0;
        int j = 0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        double minLoc = Double.MAX_VALUE;
        double maxLoc = Double.MIN_VALUE;

        do {

            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, READ_FRAME_COUNT);


            // Loop through frames and look for minimum and maximum value
            if (framesRead != 1) {

                for (int s = 0; s < framesRead * numChannels; s++) {

                    if (buffer[s] > maxLoc) maxLoc = buffer[s];
                    if (buffer[s] < minLoc) minLoc = buffer[s]; //does nothing


                    //get max out of every 256
                    if (k % 256 == 0) {
                        avrg += Math.abs(maxLoc);
                        maxLoc = Double.MIN_VALUE;
                        j++;

                    }

                    //average of maximums from samplesPerPixel
                    if (k % samplesPerPixel == 0) {
                        yAxis.add((float) (avrg / j));
                        if ( (avrg / j) > max) max =  (avrg / j);

                        j = 0;
                        avrg = 0;
                    }

                    k++;
                }
            }

        } while (framesRead != 0);

        Log.e(" min i max su ", min + " // " + max);

        double ratio = ((this.getMeasuredHeight() / 2) - getPaddingTop() - getPaddingBottom()/max);

        for (k = 0; k < (yAxis.size() - 1); k++) {
            yAxis.set(k, (float) (yAxis.get(k) * ratio));
        }
        Log.e(LOG_TAG, "" + (System.currentTimeMillis() - startms) + " .. " + 0);

        // Close the wavFile
        wavFile.close();

    }

}


