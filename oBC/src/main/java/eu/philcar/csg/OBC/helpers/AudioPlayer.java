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


public class AudioPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{


    private MediaPlayer player;
    private static int queue=0;
    private static final String TAG = "AudioPlayer";
    private boolean isBusy=false; //indicate if the player is busy
    public int lastAudioState = 0,actualAudioState = 1;
    public boolean isSystem = false;    //flag per il via libera alla riproduzione
    public boolean reqSystem = false;  //flag per salvare il valore precedente di canale audio
    private static Uri playing=null;

    private Context context;
    public static AudioPlayer Instance;
    private DLog dlog = new DLog(this.getClass());



    public AudioPlayer(Context context) {
        this.context=context;
        Instance = this;

    }
    public void inizializePlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

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
    public void waitToPlayFile(Uri uri) {
        int tries=0;
        try {
            queue++;
            while(isBusy){
                if(playing.compareTo(uri)==0)
                    return;
                Thread.sleep(1000);
                tries++;
                if(tries>15){
                    reset();
                    if(context instanceof AMainOBC)
                        ((AMainOBC)context).setAudioSystem(lastAudioState);
                    else if (context instanceof AGoodbye)
                        ((AGoodbye)context).setAudioSystem(lastAudioState);


                    dlog.d("waitToPlayFile: reset and abort");
                    return;
                }
                dlog.d("waitToPlayFile: aspetto da "+tries +"s");

            }
            player.reset();
            isBusy=true;
            try {
                playing=uri;
                player.setDataSource(context,uri);
                dlog.d("waitToPlayFile: riproduco ");
            } catch (IllegalStateException ile) {
                try{
                player.reset();
                    isBusy=true;
                    playing=uri;
                    player.setDataSource(context,uri);}
                catch(Exception e){
                    isBusy=false;
                    dlog.e("waitToPlayFile: deep Exception ");
                }
            }
            Thread.sleep(1100);
            //player.setDataSource(context,uri);
            player.prepare();
            player.start();
        } catch (Exception e) {
            dlog.e("Eccezione in play file",e);
        }
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
        isBusy = false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        queue--;
        reset();
        isBusy = false;
        if(context instanceof AMainOBC)
            ((AMainOBC)context).setAudioSystem(lastAudioState);
        else if (context instanceof AGoodbye)
            ((AGoodbye)context).setAudioSystem(lastAudioState);


    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        queue--;
        reset();
        isBusy = false;
        if(context instanceof AMainOBC)
            ((AMainOBC)context).setAudioSystem(lastAudioState);
        else if (context instanceof AGoodbye)
            ((AGoodbye)context).setAudioSystem(lastAudioState);
        return true; //error was handled
    }







}
