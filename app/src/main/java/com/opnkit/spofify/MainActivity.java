package com.opnkit.spofify;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
    private PlayerView playerView;
    private final String TAG = "Spofify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.7:5000/")
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
                    startAudioPlayback(response.body().byteStream());
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
    private void startAudioPlayback(InputStream inputStream) {
        // Create a new ExoPlayer instance
        player = new SimpleExoPlayer.Builder(this).build();

        // Set up the player view
        playerView.setPlayer(player);

        // Create a custom DataSource to stream from InputStream
        DataSource.Factory streamDataSourceFactory = () -> new DataSource() {
            private InputStream inputStream;

            @Override
            public void addTransferListener(TransferListener transferListener) {
            }

            @Override
            public long open(DataSpec dataSpec) throws IOException {
                // Ensure data is properly initialized
                byte[] data = new byte[(int) dataSpec.length];

                // Read data from the InputStream
                int bytesRead = inputStream.read(data, 0, data.length);

                // Check if the end of the stream is reached
                if (bytesRead == -1) {
                    return C.RESULT_END_OF_INPUT;
                }

                // Process the read data as needed

                return bytesRead;
            }

            @Nullable
            @Override
            public Uri getUri() {
                return null;
            }

            @Override
            public int read(byte[] buffer, int offset, int readLength) throws IOException {
                return inputStream.read(buffer, offset, readLength);
            }

            @Override
            public void close() throws IOException {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        };

        MediaSource audioSource = new ProgressiveMediaSource.Factory(streamDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse("")));

        // Set the media source for audio playback
        player.prepare(audioSource);
        player.setPlayWhenReady(true);
    }

    // Release the player when it's no longer needed
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }



}

