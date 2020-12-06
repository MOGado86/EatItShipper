package com.example.eatitshipper.services;

import androidx.annotation.NonNull;

import com.example.eatitshipper.Common.Common;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv =remoteMessage.getData();
        if (dataRecv != null) {
            Common.showNotification(this, new Random().nextInt(),
                    dataRecv.get(Common.NOT1_TITLE),
                    dataRecv.get(Common.NOT1_CONTENT),
                    null);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            Common.updateToken(this, s, false, true);
    }

}
