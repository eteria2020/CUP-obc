package eu.philcar.csg.OBC.helpers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;


public class ProTTS implements TextToSpeech.OnInitListener{


    private TextToSpeech player;
    private UtteranceProgressListener utteranceListener;
    private ArrayList<String> playing =new ArrayList<>();

    public static int lastAudioState = 0;
    public static boolean reqSystem=false;



    private boolean ready=false;

    private int queue = 0;


    private Context context;
    private DLog dlog = new DLog(this.getClass());



    public ProTTS(Context context) {
        this.context=context;
        player=new TextToSpeech(context,this);

    }
    private void reinizializePlayer() {

        player=null;
        utteranceListener=null;

        player = new TextToSpeech(context, this);


    }



    public void shutdown(){
        try {
            player.stop();
            player.shutdown();
            player=null;
            utteranceListener=null;
        }catch(Exception e){
            dlog.e("Exception while pausing player",e);
        }
    }


    public boolean isBusy(){
        return player.isSpeaking();
    }


    /*public void resumePlayer(){
        try {
            player.start();
        }catch(Exception e){
            dlog.e("Exception while resuming player",e);
        }
    }*/

    public boolean isReady() {
        return ready;
    }


    public void speak(final String text) {


                try{
                   /* if(playing.size()>0){
                        for(String s : playing){
                            if(s.compareTo(text)==0){
                                dlog.d("waitToPlayTTS: already in queue");
                                return;
                            }
                        }
                    }*/
                    //playing.add(text);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
                    //player.setDataSource(context,uri);
                    player.speak(text,TextToSpeech.QUEUE_ADD,map);
                    map.clear();
                    dlog.d("speak: riproduco "+text);
                } catch (Exception e) {
                     dlog.e("speak: Eccezione in play tts",e);
                }


    }
    public void speak(final String text,final int queueMode) {

                try{
                   /* if(playing.size()>0){
                        for(String s : playing){
                            if(s.compareTo(text)==0){
                                dlog.d("waitToPlayTTS: already in queue");
                                return;
                            }
                        }
                    }*/
                    //playing.add(text);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
                    Thread.sleep(2000);
                    //player.setDataSource(context,uri);
                    player.speak(text,queueMode,map);
                    map.clear();
                    dlog.d("speak: riproduco "+text);
                } catch (Exception e) {
                    dlog.e("speak: Eccezione in play tts",e);
                }


    }
    public void reset() {

        if (player != null) {
            try {
                player.stop();
                player.shutdown();
                reinizializePlayer();
                //player.setOnCompletionListener(this);
                //player.setOnErrorListener(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        else
            reinizializePlayer();
    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
            if (((ABase)context).getActivityLocale().equalsIgnoreCase("it")) {
                player.setLanguage(Locale.ITALIAN);
            } else if (((ABase)context).getActivityLocale().equalsIgnoreCase("en")) {
                player.setLanguage(Locale.ENGLISH);
            }
            else {
                player.setLanguage(Locale.FRENCH);
            }

            utteranceListener = new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                        dlog.d("Player is Busy pausing advice");
                    Toast.makeText(context,"start",Toast.LENGTH_SHORT).show();
                        AMainOBC.player.pausePlayer();

                }

                @Override
                public void onDone(String utteranceId) {
                    Toast.makeText(context,"done",Toast.LENGTH_SHORT).show();
                    ((AMainOBC)context).setAudioSystem(lastAudioState);
                }

                @Override
                public void onError(String utteranceId) {

                }
            };

            Toast.makeText(context,"init",Toast.LENGTH_SHORT).show();
            player.setOnUtteranceProgressListener(utteranceListener);
            ready=true;
        }else
            ready=false;

    }

    public void postpone(){
        if(!player.isSpeaking())
            return;
        player.stop();
    }
}
