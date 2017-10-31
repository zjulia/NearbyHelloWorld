package com.apps.zhaojulia.nearbyhelloworld;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultTransform;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_RESOLVE_ERROR = 1;
    private GoogleApiClient mGoogleApiClient;
    private Message mActiveMessage;
    private String TAG = "gNearby ";
    private MessageListener mMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        You can avoid the opt in dialog if your app has been granted the ACCESS_FINE_LOCATION
//        permission and only uses BLE during publishes and subscribes.
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
                    builder.addApi(Nearby.MESSAGES_API);
                    builder.addConnectionCallbacks(this);
                    builder.enableAutoManage(this, this);
        mGoogleApiClient = builder.build();
//        }
        handleSubscribeListener();
    }

    private void handleSubscribeListener() {
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                String messageAsString = new String(message.getContent());
                Toast.makeText(MainActivity.this, "Received: " + messageAsString, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Found message: " + messageAsString);
            }

            @Override
            public void onLost(Message message) {
                String messageAsString = new String(message.getContent());
                Log.d(TAG, "Lost sight of message: " + messageAsString);
            }
        };
    }

    private void publish(String message) {
        Log.i(TAG, "Publishing message: " + message);
        Toast.makeText(MainActivity.this, "Sending Message: " + message, Toast.LENGTH_SHORT).show();
        mActiveMessage = new Message(message.getBytes());
        Nearby.Messages.publish(mGoogleApiClient, mActiveMessage);
    }

    // Subscribe to receive messages.
    private void subscribe() {
        Log.i(TAG, "Subscribing.");

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(com.google.android.gms.nearby.messages.Strategy.DEFAULT)
                .setCallback(new SubscribeCallback())
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options).then(new ResultTransform<Status, Result>() {
            @Nullable
            @Override
            public PendingResult<Result> onSuccess(@NonNull Status status) {
                return null;
            }

            @NonNull
            @Override
            public Status onFailure(@NonNull Status status) {
                return super.onFailure(status);
            }
        });
    }

    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        if (mActiveMessage != null) {
            Nearby.Messages.unpublish(mGoogleApiClient, mActiveMessage);
            mActiveMessage = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        publish("Hello World");
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient disconnected with cause: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "GoogleApiClient connection failed");
        }
    }



    @Override
    protected void onStop() {
        unpublish();
        unsubscribe();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * To unsubscribe and stop receiving device messages, call Nearby.Messages.unsubscribe().
     * Pass the same MessageListener object that was used to subscribe
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                Log.e(TAG, "GoogleApiClient connection failed. Unable to resolve.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void sendClicked(View view) {
        EditText messageView = (EditText) findViewById(R.id.message_view);
        String mes = messageView.getText().toString();
        publish(mes);
    }
}
