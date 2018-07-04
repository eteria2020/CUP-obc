package eu.philcar.csg.OBC.helpers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;

import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.devices.LowLevelInterface;


public class AudioPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{


    private final Context context;
    private DLog dlog = new DLog(this.getClass());
    private static MediaPlayer player;

    private static int queue=0;
    private static Uri playing=null;
    private static boolean isBusy=false; //indicate if the player is busy

    public static int lastAudioState = 0;
    public static boolean isSystem = false;    //flag per il via libera alla riproduzione
    public static boolean reqSystem=false;
    public static boolean ignoreVolume=false;




    public AudioPlayer(Context context) {
        this.context=context;
        inizializePlayer(false);
    }
    private void inizializePlayer(boolean forced) {
        if(player == null|| forced) {

            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playing=null;
        queue--;
        reset();
        isBusy = false;
        if(queue<=0&&ProTTS.getQueue()==0) {
            queue=0;
            if (context instanceof AMainOBC)
                ((AMainOBC) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
            else if (context instanceof AGoodbye)
                ((AGoodbye) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
        }


    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        queue--;
        playing=null;
        reset();
        isBusy = false;
        if(queue<=0&&ProTTS.getQueue()==0) {
            queue=0;
            if (context instanceof AMainOBC)
                ((AMainOBC) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
            else if (context instanceof AGoodbye)
                ((AGoodbye) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
        }
        return true; //error was handled
    }

    public void waitToPlayFile(final Uri uri) {
        new Thread() {
            public void run() {
                Thread.currentThread().setName("Audio Player");
                int tries = 0;
                try {
                    queue++;
                    while (isBusy) {
                        if (playing.compareTo(uri) == 0) {
                            queue--;
                            return;
                        }
                        Thread.sleep(2000);
                        tries++;
                        if (tries > 15) {
                            reset();
                            isBusy = false;
                            if (context instanceof AMainOBC)
                                ((AMainOBC) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                            else if (context instanceof AGoodbye)
                                ((AGoodbye) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);


                            dlog.d("waitToPlayFile: reset and abort");
                            return;
                        }
                        dlog.d("waitToPlayFile: aspetto da " + (tries * 2) + "s queue is " + queue);

                    }
                    isBusy = true;
                    player.reset();
                    try {
                        playing = uri;
                        player.setDataSource(context, uri);
                        dlog.d("waitToPlayFile: riproduco ");
                    } catch (IllegalStateException ile) {
                        try {
                            player.reset();
                            playing = uri;
                            player.setDataSource(context, uri);
                        } catch (Exception e) {
                            queue--;
                            isBusy = false;
                            inizializePlayer(true);
                            //handling end track operation
                            if (queue <= 0 && ProTTS.getQueue() == 0) {
                                queue = 0;
                                if (context instanceof AMainOBC)
                                    ((AMainOBC) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                                else if (context instanceof AGoodbye)
                                    ((AGoodbye) context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                            }
                            dlog.e("waitToPlayFile: deep Exception ");
                            return;
                        }
                    }
                    Thread.sleep(2500);
                    //player.setDataSource(context,uri);
                    player.prepare();
                    player.start();
                } catch (Exception e) {
                    dlog.e("Eccezione in play file", e);
                }
            }
        }.start();
    }

    public void reset() {

        playing=null;

        if (player != null) {
            try {
                player.reset();
                //player.setOnCompletionListener(this);
                //player.setOnErrorListener(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        else{
            inizializePlayer(false);
        }
    }

    public static void askForSystem(){
        reqSystem=true;
        ignoreVolume=true;
    }

    public static void pausePlayer(){
        try {
            if(player.isPlaying())
                player.pause();
        }catch(Exception e){
            DLog.E("Exception while pausing player",e);
        }
    }


    public static boolean isBusy(){
        return isBusy;
    }


    public static void resumePlayer(){
        try {
            player.start();
        }catch(Exception e){
            DLog.E("Exception while resuming player",e);
        }
    }






    public void playFile(String filePath) {

        try {
            player.reset();
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            try {
                player.setDataSource(fileDescriptor);
            } catch (IllegalStateException ile) {
                player.reset();
                player.setDataSource(fileDescriptor);
            }
            fileInputStream.close();

            player.prepare();
            player.start();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }
    public void playFile(Uri uri) {

        try {
            player.reset();
            try {
                player.setDataSource(context,uri);
            } catch (IllegalStateException ile) {
                player.reset();
                player.setDataSource(context,uri);
            }

            player.prepare();
            while(!isSystem)
            {}
            player.start();
        } catch (Exception e) {
            dlog.e("Eccezione in play file",e);
        }
    }


    public static int getQueue() {
        return queue;
    }
}
