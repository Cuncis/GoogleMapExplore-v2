package com.gdc.googlemapexplore.api;


import com.gdc.googlemapexplore.model.GMapResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("/maps/api/directions/json")
    Call<GMapResponse> getGmapData(@Query("origin") String origin,
                                   @Query("waypoints") String waypoint,
                                   @Query("destination") String destination,
                                   @Query("sensor") String sensor,
                                   @Query("mode") String mode,
                                   @Query("key") String key);

}
















