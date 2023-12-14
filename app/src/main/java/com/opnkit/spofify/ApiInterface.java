package com.opnkit.spofify;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiInterface {
    @GET("/songs")
    Call<List<String>> getSongs();

    @GET("/songs/{songName}")
    Call<ResponseBody> getSong(@Path("songName") String songName);
}
