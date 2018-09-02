package com.shahzaib.freereecharge;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class SelectSim extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_sim);
    }


    public void onSimLogoClick(View view)
    {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_raise_animation));
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();

        switch (view.getId())
        {
            case R.id.jazz:
                spEditor.putString(getString(R.string.default_sim),getString(R.string.value_jazz));
                Toast.makeText(this, "Jazz SIM Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.warid:
                spEditor.putString(getString(R.string.default_sim),getString(R.string.value_warid));
                Toast.makeText(this, "Warid SIM Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.telenor:
                spEditor.putString(getString(R.string.default_sim),getString(R.string.value_telenor));
                Toast.makeText(this, "Telenor SIM Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.zong:
                spEditor.putString(getString(R.string.default_sim),getString(R.string.value_zong));
                Toast.makeText(this, "Zong SIM Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.ufone:
                spEditor.putString(getString(R.string.default_sim),getString(R.string.value_ufone));
                Toast.makeText(this, "Ufone SIM Selected", Toast.LENGTH_SHORT).show();
                break;
        }
        spEditor.apply();
        finish();
    }
}
