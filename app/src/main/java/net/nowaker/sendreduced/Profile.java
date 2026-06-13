package net.nowaker.sendreduced;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** A named set of reduction settings the user can pick from when sharing. */
public class Profile {
    public static final String FORMAT_JPEG = "jpeg";
    public static final String FORMAT_PNG = "png";
    public static final int RESOLUTION_KEEP = 0;

    public String id;
    public String name;
    public String format;
    public int quality;        // JPEG quality 10..100, ignored for PNG
    public int maxResolution;  // longest side in px; 0 = keep original
    public boolean preserveMetadata;

    public Profile() {
        this(UUID.randomUUID().toString(), "", FORMAT_JPEG, 85, 1024, false);
    }

    public Profile(String id, String name, String format, int quality, int maxResolution,
                   boolean preserveMetadata) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.quality = quality;
        this.maxResolution = maxResolution;
        this.preserveMetadata = preserveMetadata;
    }

    public boolean isJpeg() {
        return !FORMAT_PNG.equals(format);
    }

    public boolean keepsResolution() {
        return maxResolution <= RESOLUTION_KEEP;
    }

    public String extension() {
        return isJpeg() ? ".jpg" : ".png";
    }

    public String mimeType() {
        return isJpeg() ? "image/jpeg" : "image/png";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("format", format);
        o.put("quality", quality);
        o.put("maxResolution", maxResolution);
        o.put("preserveMetadata", preserveMetadata);
        return o;
    }

    public static Profile fromJson(JSONObject o) {
        return new Profile(
                o.optString("id", UUID.randomUUID().toString()),
                o.optString("name", "Profile"),
                o.optString("format", FORMAT_JPEG),
                o.optInt("quality", 85),
                o.optInt("maxResolution", 1024),
                o.optBoolean("preserveMetadata", false));
    }

    public static Profile createDefault() {
        return new Profile(UUID.randomUUID().toString(), "Default", FORMAT_JPEG, 85, 1024, false);
    }
}
