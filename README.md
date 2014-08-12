WaveformSeekBar
===============

Android seekbar which gives you a visual representation (waveform) of your wav file.


to use you can just include dependency 
    compile ('com.github.dsibenik:WaveformSeekBar:0.+@aar'


in your xml file, do something like this:

xmlns:waveformSeekBar="http://schemas.android.com/apk/res-auto"

            <com.android.custom.widget.waveformseekbar.WaveformSeekBar
                waveformSeekBar:activeLineColor="#FFFFFF"
                waveformSeekBar:inactiveLineColor="#000000"
                waveformSeekBar:inactiveLineColorAlpha="60"

                android:id="@+id/seekbar"
                android:layout_width="fill_parent"
                android:layout_height="150dp"
                android:layout_marginTop="10dp"/>

                
and set the audio track you wish to display using 
            seekbar.setAudio(InputStream);
            
(if you're having trouble, see example.java)

everything else is just like using a normal seekbar. enjoy!
