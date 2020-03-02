package peterbach.coursework;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder>{

    private Context context;
    private ArrayList<Recording> recordingArrayList;
    private MediaPlayer mediaPlayer;
    private boolean isAudioPlaying = false;
    private int positionLast = -1;

    public AudioAdapter(Context context, ArrayList<Recording> recordingArrayList){
        this.context = context;
        this.recordingArrayList = recordingArrayList;
    }// end constructor

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.audio_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        setUpData(holder,position);

    }

    private void setUpData(ViewHolder holder, int position) {

        Recording recording = recordingArrayList.get(position);
        holder.textViewName.setText(recording.getmyAudioFileName());

        if( recording.isAudioPlaying()) {
            holder.myImageViewAudioPlay.setImageResource(R.drawable.pause);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.VISIBLE);
            holder.seekBarPosition(holder);
        }else{
            holder.myImageViewAudioPlay.setImageResource(R.drawable.play);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.GONE);
        }

        holder.seekBarSetting(holder);

    }

    @Override
    public int getItemCount() {
        return recordingArrayList.size();
    }

    //ViewHolder Class
    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView myImageViewAudioPlay;
        SeekBar seekBar;
        TextView textViewName;
        private String recordingUri;
        private int previousAudioProgress = 0;
        private Handler mediaHandler = new Handler();
        ViewHolder holder;

        public ViewHolder(View itemView) {
            super(itemView);

            myImageViewAudioPlay = itemView.findViewById(R.id.myImageViewAudioPlay);
            seekBar = itemView.findViewById(R.id.seekBar);
            textViewName = itemView.findViewById(R.id.textViewRecordingname);

            //On Play Button Click
            myImageViewAudioPlay.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Recording recording = recordingArrayList.get(position);
                    //getting recorded file
                    recordingUri = recording.getUri();

                        if( isAudioPlaying ) {
                            stopAudioPlaying();
                            //in case we stop:
                            if( position == positionLast ) {
                                recording.setPlaying(false);
                                stopAudioPlaying();
                                notifyItemChanged(position);
                            }
                            else {
                                markAllPaused();
                                recording.setPlaying(true);
                                notifyItemChanged(position);
                                startAudioPlaying(recording,position);
                                positionLast = position;
                            }

                        }
                        else {
                            if( recording.isAudioPlaying() ) {
                                recording.setPlaying(false);
                                stopAudioPlaying();
                                Log.d("AudioPlaying","True");
                            }
                            else {
                                startAudioPlaying(recording,position);
                                recording.setPlaying(true);
                                seekBar.setMax(mediaPlayer.getDuration());
                                Log.d("AudioPlaying","False");
                            }
                            notifyItemChanged(position);
                            positionLast = position;
                        }

                    }

            });
        }
        public void seekBarSetting(ViewHolder holder) {
            holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if( mediaPlayer!=null && fromUser ){
                        mediaPlayer.seekTo(progress);
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

        private void markAllPaused() {
            for( int i=0; i < recordingArrayList.size(); i++ ){
                recordingArrayList.get(i).setPlaying(false);
                recordingArrayList.set(i,recordingArrayList.get(i));
            }
            notifyDataSetChanged();
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                seekBarPosition(holder);
            }
        };

        private void seekBarPosition(ViewHolder holder) {
            this.holder = holder;

            if(mediaPlayer != null) {
                int mCurrentPosition = mediaPlayer.getCurrentPosition() ;
                holder.seekBar.setMax(mediaPlayer.getDuration());
                holder.seekBar.setProgress(mCurrentPosition);
                previousAudioProgress = mCurrentPosition;
            }
            mediaHandler.postDelayed(runnable, 100);
        }

        private void stopAudioPlaying() {
            try{
                mediaPlayer.release();
            }
            catch (Exception exception){
                exception.printStackTrace();
            }
            mediaPlayer = null;
            isAudioPlaying = false;
        }

        private void startAudioPlaying(final Recording audio, final int position) {
            mediaPlayer = new MediaPlayer();

            try {
                mediaPlayer.setDataSource(recordingUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
            catch (IOException e) {
                Log.e("START_PLAY", "prepare() failed");
            }
            //showing the pause button
            seekBar.setMax(mediaPlayer.getDuration());
            isAudioPlaying = true;

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    audio.setPlaying(false);
                    notifyItemChanged(position);
                }
            });



        }

    }
}
