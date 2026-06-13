package net.nowaker.sendreduced;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Entry point for shared images. Picks a profile (if several), reduces, then re-shares. */
public class SendReduced extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null || intent.getAction() == null
                || intent.getBooleanExtra(Utils.INTENT_FROM_ME, false)) {
            finish();
            return;
        }

        List<Uri> inputs = extractUris(intent);
        if (inputs.isEmpty()) {
            finish();
            return;
        }

        List<Profile> profiles = Store.loadProfiles(this);
        if (profiles.size() <= 1) {
            process(profiles.get(0), inputs);
        } else {
            showPicker(profiles, inputs);
        }
    }

    private List<Uri> extractUris(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u != null) {
                uris.add(u);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list != null) {
                for (Uri u : list) {
                    if (u != null) {
                        uris.add(u);
                    }
                }
            }
        }
        return uris;
    }

    private void showPicker(final List<Profile> profiles, final List<Uri> inputs) {
        String[] names = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) {
            names[i] = profiles.get(i).name;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_profile)
                .setItems(names, (dialog, which) -> process(profiles.get(which), inputs))
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void process(final Profile profile, final List<Uri> inputs) {
        final AlertDialog progress = new MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create();
        progress.show();

        new Thread(() -> {
            Utils utils = new Utils(getApplicationContext(), profile);
            final List<Uri> outputs = new ArrayList<>();
            for (Uri in : inputs) {
                Uri reduced = utils.reduce(in);
                if (reduced != null) {
                    outputs.add(reduced);
                }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                if (outputs.isEmpty()) {
                    Toast.makeText(this, R.string.error_no_images, Toast.LENGTH_LONG).show();
                } else {
                    Utils.share(this, outputs, profile.mimeType(), Store.isIncludeDirect(this));
                }
                finish();
            });
        }).start();
    }
}
