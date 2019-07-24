package com.morocco.hamssa.entities;


import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;

public class RecordVoice {

    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;
    String outputFile;

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }



    public void startRecording() throws IOException {
        ditchMediaRecord();
        deleteFile();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(getOutputFile());

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
        mediaPlayer.setDataSource(getOutputFile());
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

        File outFile = new File(getOutputFile());

        if(outFile.exists())
            outFile.delete();

    }

    public int getCurrentPos() throws IOException {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getOutputFile());
            return mediaPlayer.getDuration();



    }

}
