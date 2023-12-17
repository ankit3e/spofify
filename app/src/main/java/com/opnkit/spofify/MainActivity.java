package com.opnkit.spofify;

import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Inside your MainActivity class



public class MainActivity extends AppCompatActivity {

    private ApiInterface apiInterface;
    private SimpleExoPlayer player;
    private StyledPlayerView playerView;
    private final String TAG = "Spofify";
    private String BASE_URL = "http://192.168.1.7:5000/";
    private byte[] data; // Add this field to your class


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(ApiInterface.class);

        // Implement button click to list songs
        Button btnListSongs = findViewById(R.id.btnListSongs);
        btnListSongs.setOnClickListener(v -> listSongs());
    }

    private void listSongs() {

        Call<List<String>> call = apiInterface.getSongs();
        Log.d(TAG,"CLicked listSongs");
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Populate the list view with the song list
                    Log.d(TAG,"Successful query listSongs");
                    List<String> songs = response.body();
                    Log.d(TAG, "Size of song list" + songs.get(0));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_list_item_1, songs);

                    ListView listView = findViewById(R.id.listView);
                    listView.setAdapter(adapter);

                    // Implement item click to play the selected song
                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedSong = songs.get(position);
                        playSong(selectedSong);
                    });
                }
                else {
                    Log.d(TAG,"failed Query listSongs");
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                // Handle failure
                if (t instanceof HttpException) {
                    HttpException httpException = (HttpException) t;
                    int statusCode = httpException.code();
                    Log.d(TAG, "HTTP status code: " + statusCode);
                }
                // Handle other types of failures
                Log.d(TAG, "Failed Query listSongs", t);
            }
        });
    }

    private void playSong(String songName) {
        Call<ResponseBody> call = apiInterface.getSong(songName);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Start audio playback
                    Log.d(TAG, "Response stream size" + songName);
                    startAudioPlayback(BASE_URL + "songs/" + songName);
                } else {
                    // Handle unsuccessful response
                    Toast.makeText(MainActivity.this, "Failed to retrieve the audio file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle failure
                Toast.makeText(MainActivity.this, "Failed to connect to the server", Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void startAudioPlayback(String audioUrl) {
        // Create a new ExoPlayer instance
        player = new SimpleExoPlayer.Builder(this).build();

        // Set up the player view
        playerView = findViewById(R.id.playerView);
        playerView.setPlayer(player);

        // Create a data source factory
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, "YourAppName")
        );

        // Create a media source
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(audioUrl)));

        // Prepare the player with the media source
        player.setPlayWhenReady(true); // Auto-play
        player.prepare(mediaSource);
    }
    // Release the player when it's no longer needed
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }



}

