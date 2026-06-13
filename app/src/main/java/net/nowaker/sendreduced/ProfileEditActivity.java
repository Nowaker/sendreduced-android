package net.nowaker.sendreduced;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Add or edit a single {@link Profile}. */
public class ProfileEditActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "profile_id";

    private List<Profile> all;
    private Profile profile;
    private boolean isNew;

    private TextInputEditText inputName;
    private MaterialButtonToggleGroup formatGroup;
    private Slider qualitySlider;
    private android.widget.TextView qualityLabel;
    private MaterialSwitch keepResolution;
    private TextInputLayout resolutionLayout;
    private TextInputEditText inputResolution;
    private MaterialSwitch preserveMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        inputName = findViewById(R.id.inputName);
        formatGroup = findViewById(R.id.formatGroup);
        qualitySlider = findViewById(R.id.qualitySlider);
        qualityLabel = findViewById(R.id.qualityLabel);
        keepResolution = findViewById(R.id.keepResolution);
        resolutionLayout = findViewById(R.id.resolutionLayout);
        inputResolution = findViewById(R.id.inputResolution);
        preserveMetadata = findViewById(R.id.preserveMetadata);

        all = Store.loadProfiles(this);
        String id = getIntent().getStringExtra(EXTRA_ID);
        profile = findById(id);
        isNew = profile == null;
        if (isNew) {
            profile = new Profile();
        }

        toolbar.setTitle(isNew ? R.string.new_profile : R.string.edit_profile);
        bind();

        formatGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> updateConditionalFields());
        qualitySlider.addOnChangeListener((slider, value, fromUser) -> updateQualityLabel());
        keepResolution.setOnCheckedChangeListener((b, checked) -> updateConditionalFields());

        MaterialButton save = findViewById(R.id.saveButton);
        save.setOnClickListener(v -> save());

        MaterialButton delete = findViewById(R.id.deleteButton);
        if (!isNew && all.size() > 1) {
            delete.setVisibility(android.view.View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete());
        }
    }

    private Profile findById(String id) {
        if (id == null) {
            return null;
        }
        for (Profile p : all) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    private void bind() {
        inputName.setText(profile.name);
        formatGroup.check(profile.isJpeg() ? R.id.formatJpeg : R.id.formatPng);
        qualitySlider.setValue(Math.max(10, Math.min(100, profile.quality)));
        keepResolution.setChecked(profile.keepsResolution());
        inputResolution.setText(profile.keepsResolution() ? "" : String.valueOf(profile.maxResolution));
        preserveMetadata.setChecked(profile.preserveMetadata);
        updateQualityLabel();
        updateConditionalFields();
    }

    private void updateQualityLabel() {
        qualityLabel.setText(getString(R.string.jpeg_quality, (int) qualitySlider.getValue()));
    }

    private void updateConditionalFields() {
        boolean jpeg = formatGroup.getCheckedButtonId() != R.id.formatPng;
        int qualityVisibility = jpeg ? android.view.View.VISIBLE : android.view.View.GONE;
        qualityLabel.setVisibility(qualityVisibility);
        qualitySlider.setVisibility(qualityVisibility);

        boolean keep = keepResolution.isChecked();
        resolutionLayout.setVisibility(keep ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void save() {
        String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        profile.name = name;
        profile.format = formatGroup.getCheckedButtonId() == R.id.formatPng
                ? Profile.FORMAT_PNG : Profile.FORMAT_JPEG;
        profile.quality = (int) qualitySlider.getValue();
        if (keepResolution.isChecked()) {
            profile.maxResolution = Profile.RESOLUTION_KEEP;
        } else {
            profile.maxResolution = parseResolution();
        }
        profile.preserveMetadata = preserveMetadata.isChecked();

        if (isNew) {
            all.add(profile);
        } else {
            replaceById(profile);
        }
        Store.saveProfiles(this, all);
        finish();
    }

    private int parseResolution() {
        String text = inputResolution.getText() == null ? "" : inputResolution.getText().toString().trim();
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : 1024;
        } catch (NumberFormatException e) {
            return 1024;
        }
    }

    private void replaceById(Profile updated) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(updated.id)) {
                all.set(i, updated);
                return;
            }
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_profile)
                .setMessage(profile.name)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_profile, (d, w) -> {
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(profile.id)) {
                            all.remove(i);
                            break;
                        }
                    }
                    Store.saveProfiles(this, all);
                    finish();
                })
                .show();
    }
}
