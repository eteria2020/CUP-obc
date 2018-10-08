package eu.philcar.csg.OBC.helpers;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.devices.LowLevelInterface;

public class ProTTS implements TextToSpeech.OnInitListener {

    private TextToSpeech player;
    private UtteranceProgressListener utteranceListener;
    private static ArrayList<String> playing = new ArrayList<>();

    public static int lastAudioState = 0;
    public static boolean reqSystem = false;
    public static boolean ignoreVolume = false;

    private boolean ready = false;

    private int queue = 0;

    private Context context;
    private DLog dlog = new DLog(this.getClass());

    public ProTTS(Context context) {
        this.context = context;
        player = new TextToSpeech(context, this);

    }

    private void reinizializePlayer() {
        player.stop();
        player.shutdown();
        player = null;
        utteranceListener = null;

        player = new TextToSpeech(context, this);

    }

    public void shutdown() {
        try {
            player.stop();
            player.shutdown();
            player = null;
            utteranceListener = null;
            context = null;
        } catch (Exception e) {
            dlog.e("Exception while pausing player", e);
        }
    }

    public boolean isBusy() {
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

    public static void askForSystem() {
        reqSystem = true;
        ignoreVolume = true;
    }

    public void speak(final String text) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("ProTTS");

                try {
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
                    playing.add(text);
                    Thread.sleep(1500);
                    //player.setDataSource(context,uri);
                    player.setPitch(0.85f);
                    player.setSpeechRate(1.15f);
                    player.speak(text, TextToSpeech.QUEUE_ADD, map);
                    map.clear();
                    dlog.d("speak: riproduco " + text);
                } catch (Exception e) {
                    dlog.e("speak: Eccezione in play tts", e);
                }
            }
        }).start();

    }

    public void speak(final String text, final int queueMode) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("ProTTS");
                try {
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
                    Thread.sleep(300);
                    //player.setDataSource(context,uri);
                    player.speak(text, queueMode, map);
                    map.clear();
                    dlog.d("speak: riproduco " + text);
                } catch (Exception e) {
                    dlog.e("speak: Eccezione in play tts", e);
                }
            }
        }).start();

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
        } else
            reinizializePlayer();
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
            if (((ABase) context).getActivityLocale().equalsIgnoreCase("it")) {
                player.setPitch(0.85f);
                player.setSpeechRate(1.15f);
                player.setLanguage(Locale.ITALIAN);
            } else if (((ABase) context).getActivityLocale().equalsIgnoreCase("en")) {
                player.setPitch(0.7f);
                player.setSpeechRate(0.9f);
                player.setLanguage(Locale.ENGLISH);
            } else {
                player.setLanguage(Locale.FRENCH);
                player.setPitch(0.9f);
                player.setSpeechRate(0.4f);
            }

            utteranceListener = new UtteranceProgressListener() {
                private final Context Context = context;

                @Override
                public void onStart(String utteranceId) {
                    dlog.d("Player is Busy pausing advice");

                    if (AudioPlayer.isBusy())
                        AudioPlayer.pausePlayer();

                }

                @Override
                public void onDone(String utteranceId) {
                    // Toast.makeText(context,"done",Toast.LENGTH_SHORT).show();
                    try {
                        playing.remove(utteranceId);
                        if (playing.size() == 0 && AudioPlayer.getQueue() == 0)
                            if (Context instanceof AMainOBC)
                                ((AMainOBC) Context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                            else if (Context instanceof AGoodbye)
                                ((AGoodbye) Context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                    } catch (Exception e) {
                        dlog.e("ProTTS: Exception while executing onDone operation ", e);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    try {
                        playing.remove(utteranceId);
                        if (getQueue() == 0 && AudioPlayer.getQueue() == 0)
                            if (Context instanceof AMainOBC)
                                ((AMainOBC) Context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                            else if (Context instanceof AGoodbye)
                                ((AGoodbye) Context).setAudioSystem(lastAudioState, LowLevelInterface.AUDIO_LEVEL_LAST);
                    } catch (Exception e) {
                        dlog.e("ProTTS: Exception while executing onDone operation ", e);
                    }
                }
            };

            player.setOnUtteranceProgressListener(utteranceListener);
            ready = true;
        } else
            ready = false;

    }

    public void postpone() {
        if (!player.isSpeaking())
            return;
        player.stop();
    }

    public static int getQueue() {
        return playing.size();
    }
}
