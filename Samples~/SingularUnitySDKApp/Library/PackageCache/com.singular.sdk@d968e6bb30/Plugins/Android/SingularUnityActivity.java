package com.singular.unitybridge;

import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayerActivity;

import android.util.Log;
public class SingularUnityActivity extends UnityPlayerActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SingularUnityBridge.onNewIntent(intent);
    }
}
