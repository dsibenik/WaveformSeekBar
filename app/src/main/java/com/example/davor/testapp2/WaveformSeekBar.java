package com.example.davor.testapp2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.library1.WavFile;
import com.library1.WavFileException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Created by davor on 7/4/14.
 */
public class WaveformSeekBar extends SeekBar {
    public static final int READ_FRAME_COUNT = 100;
    private Vector<Float> yAxis = new Vector<Float>();
    private int numOfPoints = 1000;
    private boolean firstDraw = true;

    public WaveformSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    private InputStream inputStream;

    public void setInputStream(InputStream intStream) {
        this.inputStream = intStream;
        invalidate();
    }


    //calculating yaxis points
    private class AsyncTask1 extends AsyncTask<Void, Void, Void> {

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

        numOfPoints = this.getMeasuredWidth();

        float clickedX;
        int closestPoint = 0;
        float xAxis = 0;

        //getting x-axis of the clicked point
        clickedX = (float) (this.getMeasuredWidth() * ((double) getProgress() / getMax()));

        //path paint settings
        Paint paintPath = new Paint();
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setAntiAlias(true);



        if (firstDraw == true) {

            firstDraw = false;
            //calcualting yAxis points
            new AsyncTask1().execute();

        } else {
            //drawing graphs depending on the clicked point (clickedX)

            //closest int - possible optimisations floor or casting to int, but isn't as accurate
            for (int j = 0; j <= numOfPoints; j += 1)
                if (Math.abs(j - clickedX) < Math.abs(closestPoint - clickedX)) closestPoint = j;

            //draw from 0 to clickedX
            int i = drawPath( 0, closestPoint, 0xffffffff, paintPath, canvas);

            //draw from clickedX to end
            drawPath( i, numOfPoints, 0x88ffffff, paintPath, canvas);

        }

    }   //end of onDraw


    //draw a path from start to end using yAxis array
    private int drawPath(int start, int end, int color, Paint paintPath, Canvas canvas){
        Path path = new Path();
        int viewHeightHalf = this.getMeasuredHeight() / 2;

        for ( ; start <= end; start += 1) {
            if ( start >= yAxis.size()) break;

            if (yAxis.get(start) != 0) {
                path.moveTo(start, viewHeightHalf - (yAxis.get(start)));
                path.lineTo(start, viewHeightHalf + (yAxis.get(start)));

            } else {          // making it visible (thicker line)
                path.moveTo(start, (float) (viewHeightHalf * 1.01));
                path.lineTo(start, (float) (viewHeightHalf * 0.99));
            }
        }

        path.close();
        paintPath.setColor(color);
        canvas.drawPath(path, paintPath);
        path.reset();

        return end;
    }


    private void readWavFile() throws IOException, WavFileException {

        // Open the wav file specified as the first argument
        WavFile wavFile = WavFile.openWavFile(inputStream);

        // Get the number of audio channels in the wav file
        int numChannels = wavFile.getNumChannels();

        // Create a buffer of 100 frames
        double[] buffer = new double[READ_FRAME_COUNT * numChannels];

        int framesRead;
        int samplesPerPixel = (int) (wavFile.getNumFrames() * numChannels) / (numOfPoints);


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

                for (int s = 0; s < framesRead * numChannels; s+=1) {
                    if (buffer[s] > maxLoc) maxLoc = buffer[s];


                    //get max out of every 256
                    if (overallCounter % 256 == 0) {
                        avrg += Math.abs(maxLoc);
                        maxLoc = Double.MIN_VALUE;
                        pointsInAvrg++;
                    }

                    //average of maximums from samplesPerPixel
                    if (overallCounter % samplesPerPixel == 0) {

                        if( pointsInAvrg == 0 )
                            yAxis.add( (float) 0 );

                        else {
                            yPoint = (float)(avrg / pointsInAvrg);
                            yAxis.add( yPoint );
                            if ( yPoint > max) max = (yPoint);
                            pointsInAvrg = 0;
                            avrg = 0;
                        }
                    }

                    overallCounter++;
                }
            }

        } while (framesRead != 0);

        //ratio relative to the screensize and padding
        double ratio = ((this.getMeasuredHeight() / 2) - getPaddingTop() - getPaddingBottom()/max);

        //normalizing the values relative to the ratio
        for (overallCounter = 0; overallCounter < yAxis.size(); overallCounter++)
            yAxis.set( overallCounter, (float) ( yAxis.get(overallCounter) * ratio) );

        // Close the wavFile
        wavFile.close();
    }

}


