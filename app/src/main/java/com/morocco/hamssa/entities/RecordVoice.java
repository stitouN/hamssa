package com.morocco.hamssa.entities;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class RecordVoice {

    private MediaRecorder mediaRecorder;
    private SoundPool soundPool;
    private String audioPath = null;


    public void setAudioPath(String path){
        this.audioPath = path;
    }
    public String getAudioPath(){
        return  this.audioPath;
    }

    public void startRecording() throws IOException {
         if(mediaRecorder != null) mediaRecorder.release();
         File outFile = new File(audioPath);
         if(outFile.exists()) outFile.delete();

         mediaRecorder = new MediaRecorder();
         mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         mediaRecorder.setOutputFile(audioPath);
         mediaRecorder.prepare();
         mediaRecorder.start();

    }

    public void stopRecording(){
        if(mediaRecorder != null) mediaRecorder.stop();
    }

    int soundId;
    boolean loaded = false;
    public void ChangeVoice(String type) {

        if (audioPath != null) {

            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            soundId = soundPool.load(audioPath, 1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    loaded = true;
                }
            });

            if (loaded) {
                switch (type) {

                    case "Voice A":
                        soundPool.play(soundId, 100, 100, 0, 1, 1.4f);
                        break;
                    case "Voice B":
                        soundPool.play(soundId, 100, 100, 0, 1, 1.5f);
                        break;
                    case "Voice C":
                        soundPool.play(soundId, 100, 100, 0, 1, 1.6f);
                        break;
                }
            }

        }


    }

    public void StopSoundPlaying(){
        soundPool.pause(soundId);
    }

    public void deleteFile(){
        if(audioPath != null){
            File file = new File(audioPath);
            if(file.exists()) file.delete();
        }

    }



}
