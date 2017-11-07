package com.dev.jvhis.inappbillingdemo;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dev.jvhis.inappbillingdemo.util.IabBroadcastReceiver;
import com.dev.jvhis.inappbillingdemo.util.IabHelper;
import com.dev.jvhis.inappbillingdemo.util.IabResult;
import com.dev.jvhis.inappbillingdemo.util.Inventory;
import com.dev.jvhis.inappbillingdemo.util.Purchase;

//import com.dev.jvhis.inappbillingdemo.util.IabHelper;

public class MainActivity extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener {

    private IabHelper iabHelper;
    String base64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi6NG04jSpkbJ7iQPkjUSy/bBAphCHoNyTrdKe5jIBB/R0RDohrr4L1k2eUqk6dQXOO+5hZfIEajywfYA/XhxCPHft8LHD5pXo5TcYxvkaJ0Mw3s9U7hXVvV6zA5tG6R6ZzVlA9zppx0n6uxKLxlXiyucdJdmAYEdz8AKwsVAt3BZ7nGQhPNMIiQLQSJZLrM0iBO3fr8euGxO8UFUG12HOQJG8LkrXqzS4PzkQZR7pHL3+dmBExGSY+o4zvDJ5f3PYeXCr/BGR7WDlbLECHmZATTXXfQeBxvSrgdJV7kdT2F/iqueNgQZ7VA32juWdWRRahEkGzYy/EqK3g67wV/LbwIDAQAB";
    public final String TAG = "MAIN_ACTIVITY";
    private final String SKU_BUTTON_CLICKED = "button_pressed";

    private BroadcastReceiver mBroadcastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iabHelper = new IabHelper(this,base64);

        iabHelper.enableDebugLogging(true);

        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener(){

            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (iabHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(MainActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    iabHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
            Purchase buttonClicks = inventory.getPurchase(SKU_BUTTON_CLICKED);
            if (buttonClicks != null && verifyDeveloperPayload(buttonClicks)) {
                Log.d(TAG, "We have gas. Consuming it.");
                try {
                    iabHelper.consumeAsync(inventory.getPurchase(SKU_BUTTON_CLICKED), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming gas. Another async operation in progress.");
                }
                return;
            }

        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener(){

        @Override
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (iabHelper == null) return;

            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                Log.d(TAG, "Consumption successful. Provisioning.");
            }
            else {
                complain("Error while consuming: " + result);
            }
            Log.d(TAG, "End consumption flow.");
        }
    };

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
    }

    @Override
    public void receivedBroadcast() {

    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }
}
