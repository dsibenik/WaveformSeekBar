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
        float closestPoint = 0;
        float xAxis = 0;
        int viewHeightHalf = this.getMeasuredHeight() / 2;

        //getting x-axis of the clicked point
        clickedX = (float) (this.getMeasuredWidth() * ((double) getProgress() / getMax()));

        //path paint settings
        Paint paintPath = new Paint();
        Path path = new Path();
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setAntiAlias(true);



        if (firstDraw == true) {

            firstDraw = false;
            //calcualting yAxis points
            new AsyncTask1().execute();

        } else {
            //drawing graphs depending on the clicked point (clickedX)

            
            //closest int - possible optimisations floor or casting to int, but isn't as accurate
            for (float j = 0; j <= numOfPoints; j += 1)
                if (Math.abs(j - clickedX) < Math.abs(closestPoint - clickedX)) closestPoint = j;

            int i = 0;
            
            //drawing the first part of the graph in white
            for ( i = 0; xAxis <= closestPoint; xAxis += 1, i++) {
                if (i >= yAxis.size()) break;

                if (yAxis.get(i) != 0) {
                    path.moveTo(xAxis, viewHeightHalf - (yAxis.get(i)));
                    path.lineTo(xAxis, viewHeightHalf + (yAxis.get(i)));
                } else {
                    path.moveTo(xAxis, (float) (viewHeightHalf * 1.01));
                    path.lineTo(xAxis, (float) (viewHeightHalf * 0.99));
                }
            }
            drawPath(path, 0xffffffff, paintPath, canvas);

            //drawing the rest of the graph in transparent white, continuing with the i variable
            for (xAxis = closestPoint; xAxis <= numOfPoints; xAxis += 1, i++) {
                if (i >= yAxis.size()) break;

                if (yAxis.get(i) != 0) {
                    path.moveTo(xAxis, viewHeightHalf - (yAxis.get(i)));
                    path.lineTo(xAxis, viewHeightHalf + (yAxis.get(i)));
                } else {
                    path.moveTo(xAxis, (float) (viewHeightHalf * 1.01));
                    path.lineTo(xAxis, (float) (viewHeightHalf * 0.99));
                }
            }
            drawPath(path, 0x88ffffff, paintPath, canvas);
            
        }

    }   //End of OnDraw


    //draw a path
    private void drawPath(Path path, int color, Paint paintPath, Canvas canvas){
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
        int samplesPerPixel = (int) (wavFile.getNumFrames() * numChannels) / (numOfPoints);


        double avrg = 0;
        int overallCounter = 0;
        int pointsInAvrg = 0;
        double max = Double.MIN_VALUE;
        double maxLoc = Double.MIN_VALUE;

        do {

            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, READ_FRAME_COUNT);


            // Loop through frames and look for minimum and maximum value
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
                        if( pointsInAvrg == 0 ) yAxis.add( (float) 0 );
                        else {
                            yAxis.add( (float)(avrg / pointsInAvrg) );
                            if ((avrg / pointsInAvrg) > max) max = (avrg / pointsInAvrg);
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
            yAxis.set(overallCounter, (float) ( yAxis.get(overallCounter) * ratio) );

        // Close the wavFile
        wavFile.close();
    }

}


