package com.morocco.hamssa.entities;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class RecordVoice {

    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;
    final String audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/voice.amr";
    Context context;
    SoundPool soundPool;
    int soundId;


    public String getAudioFilePath() {
        return audioFilePath;
    }



    public void startRecording() throws IOException {
        ditchMediaRecord();
        deleteFile();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(getAudioFilePath());

        mediaRecorder.prepare();
        mediaRecorder.start();
    }
    public void stopRecording() {

        if(this.mediaRecorder != null)
            this.mediaRecorder.stop();

    }

    public void playAudio() throws IOException {
        ditchMediaPlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(getAudioFilePath());
        mediaPlayer.prepare();
        mediaPlayer.start();

    }

    private void ditchMediaPlayer() {
        if(this.mediaPlayer != null){
            try{
                this.mediaPlayer.release();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void  ditchMediaRecord(){
        if(this.mediaRecorder != null) mediaRecorder.release();
    }

    public void deleteFile(){

        File outFile = new File(getAudioFilePath());

        if(outFile.exists())
            outFile.delete();

    }

    public int getCurrentPos() throws IOException {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getAudioFilePath());
            return mediaPlayer.getDuration();



    }




    public void playSound(final float r) {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {


                soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
                soundId = soundPool.load(getAudioFilePath(), 1);
                soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        soundPool.play(soundId,100, 100, 0, 0, r);
                    }
                });

            }
        });

        streamThread.start();

    }

    private void stopSound(){
        soundPool.stop(soundId);
    }

    private void pauseSound(){
        soundPool.pause(soundId);
    }

}
