package com.corp.bing.addsmsverification;

/**
 * Created by Administrator on 2017-11-30.
 */

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;


/**
 * Retrofit api interface
 */
public interface ApiInterface {

    @GET("inbox.json")
    Call<List<Message>> getInbox();

}