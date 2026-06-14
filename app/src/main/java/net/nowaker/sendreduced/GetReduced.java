package net.nowaker.sendreduced;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Source for other apps that request an image (GET_CONTENT / PICK): opens the
 * gallery, reduces the chosen photo with the selected profile, and returns it
 * to the caller.
 */
public class GetReduced extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                    onPicked(data.getData());
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(Utils.INTENT_FROM_ME, false)) {
            Toast.makeText(this, R.string.error_no_images, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.putExtra(Utils.INTENT_FROM_ME, true);
            picker.launch(pick);
        }
    }

    private void onPicked(final Uri source) {
        List<Profile> profiles = Store.loadProfiles(this);
        if (profiles.size() <= 1) {
            process(profiles.get(0), source);
        } else {
            String[] names = new String[profiles.size()];
            for (int i = 0; i < profiles.size(); i++) {
                names[i] = profiles.get(i).name;
            }
            final List<Profile> finalProfiles = profiles;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.choose_profile)
                    .setItems(names, (dialog, which) -> process(finalProfiles.get(which), source))
                    .setOnCancelListener(dialog -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .show();
        }
    }

    private void process(final Profile profile, final Uri source) {
        final AlertDialog progress = new MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create();
        progress.show();

        new Thread(() -> {
            final Uri reduced = new Utils(getApplicationContext(), profile).reduce(source);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                if (reduced == null) {
                    Toast.makeText(this, R.string.error_no_images, Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                } else {
                    Intent result = new Intent().setData(reduced);
                    result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    setResult(RESULT_OK, result);
                }
                finish();
            });
        }).start();
    }
}
