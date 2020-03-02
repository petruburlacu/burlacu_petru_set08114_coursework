package peterbach.coursework;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener { //implement View On click listener to make the images clickable

    private Toolbar toolbar;
    private Chronometer chronometer;
    //SeekBar Reference: http://abhiandroid.com/ui/seekbar
    private SeekBar seekBar;

    private ImageView myImageViewAudioRecord;
    private ImageView myImageViewAudioPlay;
    private ImageView myImageViewAudioStop;

    private LinearLayout linearLayoutMainFrame;
    private LinearLayout linearLayoutAudioPlay;

    /*
	https://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en
	** initializing a default code for RECORD_AUDIO_REQUEST_CODE equal to 123 to fix error (link above for reference)
	*/
    private int RECORD_AUDIO_REQUEST_CODE =123;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String myAudioFileName = null; //declares the string empty
    private int previousAudioProgress = 0;
    private Handler mediaHandler = new Handler();
    private boolean isAudioPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myMainView();

        /*
        checks if the version is >= that 23 (Marshmellow)
        So, wont even try if it is lower
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioRecordPermission();
        }

        switch (Build.VERSION.SDK_INT) {

        }

        Intent intent = new Intent(this, AppService.class);
        startService(intent); //AppService intent
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.item_list) {
            Intent intent = new Intent(this, ListRecordingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onClick(View view) {

        if( view == myImageViewAudioRecord ) {
            prepareAudioRecording();
            startAudioRecording();
        }else if( view == myImageViewAudioStop ) {
            prepareAudioStop();
            stopAudioRecording();
        }else if( view == myImageViewAudioPlay ) {
            if( !isAudioPlaying && myAudioFileName != null ) {
                isAudioPlaying = true;
                startAudioPlaying();
            }else {
                isAudioPlaying = false;
                stopAudioPlaying();
            }
        }

    }

    // Should only be called on the given API level (M) or higher.
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void audioRecordPermission() {
        //https://developer.android.com/training/permissions/requesting.html
        /*
        avoids checking the build version
        library: ContextCompat.checkSelfPerm ission() available only in Marshmallow +
        CHECKS the permissions because the user can always change permissions in settings!

        */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            /*
            The permission is NOT already granted
            Shows the standard permission request dialog UI
            */
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);

        }
    }

    //request from requestPermissions
    //https://developer.android.com/training/permissions/requesting.html
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void resultPermission(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        //checks for original requeSt
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED){

                Toast.makeText(this, "PERMISSION GRANTER", Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(this, "Please give permissions", Toast.LENGTH_SHORT).show();
                finishAffinity(); //removes activity
            }
        }

    }

    /** View Main **/
    private void myMainView() {

        //connects xml with main activity
        linearLayoutMainFrame = (LinearLayout) findViewById(R.id.linearLayoutMainFrame);

        myImageViewAudioRecord = (ImageView) findViewById(R.id.myImageViewAudioRecord);
        myImageViewAudioStop = (ImageView) findViewById(R.id.myImageViewAudioStop);
        myImageViewAudioPlay = (ImageView) findViewById(R.id.myImageViewAudioPlay);
        //onClick listeners
        myImageViewAudioRecord.setOnClickListener(this);
        myImageViewAudioStop.setOnClickListener(this);
        myImageViewAudioPlay.setOnClickListener(this);

        chronometer = (Chronometer) findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        linearLayoutAudioPlay = (LinearLayout) findViewById(R.id.linearLayoutAudioPlay);

        seekBar = (SeekBar) findViewById(R.id.seekBar);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Audio Recording Application");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        //reference
        setSupportActionBar(toolbar);

    }

    /** RECORDING METHODS **/
    private void startAudioRecording() {

        //https://stackoverflow.com/questions/5453708/android-how-to-use-environment-getexternalstoragedirectory
        File path = android.os.Environment.getExternalStorageDirectory();
        File file = new File(path.getAbsolutePath() + "/AudioRecordings/Audios");
        //in app recorder >> make one for lock screen as well
        //using MediaRecorder for audio recording
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //audio source
        //https://developer.android.com/guide/topics/media/media-formats.html
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //most used format

        if (!file.exists()) {
            file.mkdirs(); // will create the specified directory path in its entirety to avoid crash
            /*
            https://docs.oracle.com/javase/6/docs/api/java/io/File.html#mkdirs%28%29
            JAVADOCS for mkdirs()
            Creates the directory named by this abstract pathname,
            including any necessary but nonexistent parent directories.
            Note that if this operation fails it may have succeeded in creating some of the necessary parent directories.
            */
        }

        //creating a storage directory for our phone
        myAudioFileName =  path.getAbsolutePath() + "/AudioRecordings/Audios/" + String.valueOf(System.currentTimeMillis() + ".mp3"); //saving path

        Log.d("myAudioFileName",myAudioFileName);

        mediaRecorder.setOutputFile(myAudioFileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //encodes the file
        //find reference for this

        // exceptions .. Try/Catch for main app MediaRecorder
        //reference for most common Media Recorder Audio Try and Catches
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }

        previousAudioProgress = 0;
        seekBar.setProgress(0);

        stopAudioPlaying();
        //imageView becomes stop Button

        //starts the time (chronometer)
        chronometer.setBase(SystemClock.elapsedRealtime()); //change chronometer name
        chronometer.start();
        //http://abhiandroid.com/ui/chronometer
    }

    private void prepareAudioStop() {

        TransitionManager.beginDelayedTransition(linearLayoutMainFrame);
        myImageViewAudioRecord.setVisibility(View.VISIBLE);
        myImageViewAudioStop.setVisibility(View.GONE);
        linearLayoutAudioPlay.setVisibility(View.VISIBLE);

    }



    private void prepareAudioRecording() {

        TransitionManager.beginDelayedTransition(linearLayoutMainFrame);
        myImageViewAudioRecord.setVisibility(View.GONE);
        myImageViewAudioStop.setVisibility(View.VISIBLE);
        linearLayoutAudioPlay.setVisibility(View.GONE);

    }

    private void stopAudioPlaying() {

        try{
            mediaPlayer.release();
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        mediaPlayer = null;
        //showing the play button
        myImageViewAudioPlay.setImageResource(R.drawable.play);
        chronometer.stop();
    }


    private void stopAudioRecording() {

        try{
            mediaRecorder.stop();
            mediaRecorder.release();
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        mediaRecorder = null;
        //starting the chronometer
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        //showing the play button
        Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show();
    }


    private void startAudioPlaying() {

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(myAudioFileName);
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (IOException exception) {
            Log.e("LOG_TAG", "prepare() failed");
        }
        //making the imageView pause button
        myImageViewAudioPlay.setImageResource(R.drawable.pause);

        seekBar.setProgress(previousAudioProgress);
        mediaPlayer.seekTo(previousAudioProgress);
        seekBar.setMax(mediaPlayer.getDuration());

        seekBarPosition();

        chronometer.start();


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { //comments here
            @Override
            public void onCompletion(MediaPlayer mP) {

                myImageViewAudioPlay.setImageResource(R.drawable.play);
                isAudioPlaying = false;
                chronometer.stop();

            }
        });



        //https://stackoverflow.com/questions/8956218/android-seekbar-setonseekbarchangelistener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //comments
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if( mediaPlayer != null && fromUser ) { //change here
                    mediaPlayer.seekTo(progress);
                    chronometer.setBase(SystemClock.elapsedRealtime() - mediaPlayer.getCurrentPosition());
                    previousAudioProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //SeekBar implementation: http://mrbool.com/how-to-play-audio-files-in-android-with-a-seekbar-feature-and-mediaplayer-class/28243
    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            seekBarPosition();
        }
    };

    private void seekBarPosition() {
        if(mediaPlayer != null) {

            int mCurrentPosition = mediaPlayer.getCurrentPosition() ;
            seekBar.setProgress(mCurrentPosition);
            previousAudioProgress = mCurrentPosition;

        }
        mediaHandler.postDelayed(runnable, 100);
    }

}
