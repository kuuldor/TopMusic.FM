package the.topmusic;

import android.graphics.Bitmap;
import the.topmusic.model.Song;

interface ITopMusicService
{
    void openFile(String path);
    void open(in Song [] list, int position);
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void enqueue(in Song [] list, int action);
    void setQueuePosition(int index);
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void toggleFavorite();
    void refresh();
    boolean isFavorite();
    boolean isPlaying();
    Song [] getQueue();
    long duration();
    long position();
    long seek(long pos);
    String getAudioId();
    String getArtistId();
    String getAlbumId();
    String getArtistName();
    String getTrackName();
    String getAlbumName();
    String getArtworkURL();
    String getArtworkThumbnailURL();
    String getPath();
    int getQueuePosition();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(String id);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
}

