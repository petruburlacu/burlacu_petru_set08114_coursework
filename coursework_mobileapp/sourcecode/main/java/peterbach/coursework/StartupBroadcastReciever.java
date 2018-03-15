package peterbach.coursework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class StartupBroadcastReciever extends BroadcastReceiver {

    //Reference for detect on boot | Broadcast Receiver
    // https://stackoverflow.com/questions/20595337/how-to-start-service-at-device-boot-in-android
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent1 = new Intent(context, AppService.class);
        context.startService(intent1);
        //Need to make a foreground service
    }
}
