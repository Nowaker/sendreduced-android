package net.nowaker.sendreduced;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Lets other apps request a reduced image via GET_CONTENT / PICK. Uses the first profile. */
public class GetReduced extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                    deliver(data.getData());
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
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

        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pick.putExtra(Utils.INTENT_FROM_ME, true);
        picker.launch(pick);
    }

    private void deliver(Uri source) {
        List<Profile> profiles = Store.loadProfiles(this);
        Uri reduced = new Utils(getApplicationContext(), profiles.get(0)).reduce(source);
        if (reduced == null) {
            setResult(RESULT_CANCELED);
            return;
        }
        Intent result = new Intent().setData(reduced);
        result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, result);
    }
}
