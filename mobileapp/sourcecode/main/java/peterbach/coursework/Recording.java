package peterbach.coursework;

public class Recording {

    String Uri;
    String myAudioFileName;
    boolean isAudioPlaying = false;


    public Recording(String uri, String myAudioFileName, boolean isAudioPlaying) {
        Uri = uri;
        this.myAudioFileName = myAudioFileName;
        this.isAudioPlaying = isAudioPlaying;
    }

    public String getUri() {
        return Uri;
    }

    public String getmyAudioFileName() {
        return myAudioFileName;
    }

    public boolean isAudioPlaying() {
        return isAudioPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isAudioPlaying = playing;
    }
}
