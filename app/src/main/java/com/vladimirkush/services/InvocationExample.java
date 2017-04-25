
package com.vladimirkush.services;

public class InvocationExample
{
    public static void main( String[] args )
    {
        FriendsTrackerService.initApplication(null);

        FriendsTrackerService friendsTrackerService = FriendsTrackerService.getInstance();
        // invoke methods of you service
        //Object result = friendsTrackerService.yourMethod();
        //System.out.println( result );
    }
}
