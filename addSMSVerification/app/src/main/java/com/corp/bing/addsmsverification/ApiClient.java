package com.corp.bing.addsmsverification;

/**
 * Created by Administrator on 2017-11-30.
 */

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Retrofit Api Client
 * Singleton Pattern
 */
public class ApiClient {
    public static final String BASE_URL = "https://api.androidhive.info/json/";
    private static Retrofit retrofit = null;

    // Create Retrofit singleton
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}