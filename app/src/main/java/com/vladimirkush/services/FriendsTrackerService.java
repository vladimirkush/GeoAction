
  /*******************************************************************
  * FriendsTrackerService.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package com.vladimirkush.services;

  import android.content.Context;

  import com.backendless.Backendless;
  import com.backendless.async.callback.AsyncCallback;


public class FriendsTrackerService
{
    private static final String BACKENDLESS_HOST = "https://api.backendless.com";
    private static final String SERVICE_NAME = "FriendsTrackerService";
    private static final String SERVICE_VERSION_NAME = "1.0.0";
    private static final String APP_VERSION = "v1";
    private static final String APP_ID = "C337AADB-C5FF-6F79-FF35-1C8CCE400600";
    private static final String SECRET_KEY = "E7EF3DA5-FE66-15DC-FF2D-32B0EFA9C800";

    private static FriendsTrackerService ourInstance = new FriendsTrackerService();

    private FriendsTrackerService(  )
    {
    }

    public static FriendsTrackerService getInstance()
    {
        return ourInstance;
    }

    public static void initApplication(Context ctx)
    {
        Backendless.setUrl( FriendsTrackerService.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        //Backendless.initApp( FriendsTrackerService.APP_ID, FriendsTrackerService.SECRET_KEY, FriendsTrackerService.APP_VERSION );
        Backendless.initApp(ctx, FriendsTrackerService.APP_ID, FriendsTrackerService.SECRET_KEY, FriendsTrackerService.APP_VERSION);
    }


    
    public java.util.ArrayList<java.lang.String> getFriendsNearMe(java.util.ArrayList<java.lang.String> ids, double lat, double lon)
    {
        Object[] args = new Object[]{ids, lat, lon};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getFriendsNearMe", args, java.util.ArrayList.class );
    }
    
    public void getFriendsNearMeAsync(java.util.ArrayList<java.lang.String> ids, double lat, double lon, AsyncCallback<java.util.ArrayList<java.lang.String>> callback)
    {
        Object[] args = new Object[]{ids, lat, lon};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getFriendsNearMe", args, java.util.ArrayList.class, callback);
    }
    
}
