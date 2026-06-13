package net.nowaker.sendreduced;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Reduction engine: reads an image, applies a {@link Profile}, writes a shareable file. */
public class Utils {
    static final String TAG = "SendReduced";
    private static final int BUFSIZE = 16384;
    private static final long CLEAN_TIME = 2 * 3600 * 1000L;
    public static final String INTENT_FROM_ME = "net.nowaker.sendreduced.INTENT_FROM_ME";

    // EXIF tags copied to the output when a profile preserves metadata (JPEG only).
    // TAG_ORIENTATION is intentionally excluded: pixels are physically rotated.
    private static final String[] PRESERVE_TAGS = {
            ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            ExifInterface.TAG_FLASH, ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_ISO_SPEED, ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_EXPOSURE_MODE, ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_DIGITAL_ZOOM_RATIO, ExifInterface.TAG_SCENE_CAPTURE_TYPE,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_SPEED,
            ExifInterface.TAG_GPS_SPEED_REF, ExifInterface.TAG_GPS_IMG_DIRECTION,
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF
    };

    private final Context context;
    private final ContentResolver cr;
    private final Profile profile;
    private final File outDir;
    private int seq = 0;

    public Utils(Context context, Profile profile) {
        this.context = context.getApplicationContext();
        this.cr = this.context.getContentResolver();
        this.profile = profile;

        long now = System.currentTimeMillis();
        cleanCache(this.context, now);
        File base = new File(this.context.getCacheDir(), "shared");
        File dir = new File(base, Long.toString(now));
        int i = 0;
        while (dir.exists()) {
            dir = new File(base, now + "-" + (i++));
        }
        dir.mkdirs();
        this.outDir = dir;
    }

    /** Reduces a single image and returns a content:// uri, or null on failure. */
    public Uri reduce(Uri uri) {
        byte[] data = readAll(uri);
        if (data == null) {
            return null;
        }
        int rotation = rotationFromExif(data);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bmp == null) {
            Log.e(TAG, "could not decode " + uri);
            return null;
        }
        bmp = transform(bmp, rotation);
        File out = compress(bmp, sourceName(uri));
        bmp.recycle();
        if (out == null) {
            return null;
        }
        if (profile.preserveMetadata && profile.isJpeg()) {
            copyExif(data, out);
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", out);
    }

    private Bitmap transform(Bitmap in, int rotation) {
        int w = in.getWidth();
        int h = in.getHeight();
        Matrix m = new Matrix();
        boolean changed = false;

        int max = profile.maxResolution;
        if (max > 0 && (w > max || h > max)) {
            float scale = (h > w) ? (max / (float) h) : (max / (float) w);
            m.postScale(scale, scale);
            changed = true;
        }
        if (rotation != 0) {
            m.postRotate(rotation);
            changed = true;
        }
        if (!changed) {
            return in;
        }
        Bitmap out = Bitmap.createBitmap(in, 0, 0, w, h, m, true);
        if (out != in) {
            in.recycle();
        }
        return out;
    }

    private File compress(Bitmap bmp, String origName) {
        File out = new File(outDir, buildName(origName));
        try (OutputStream os = new FileOutputStream(out)) {
            boolean ok;
            if (profile.isJpeg()) {
                ok = bmp.compress(Bitmap.CompressFormat.JPEG, clampQuality(profile.quality), os);
            } else {
                ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            if (!ok) {
                out.delete();
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "compress failed", e);
            out.delete();
            return null;
        }
        out.setReadable(true, false);
        return out;
    }

    private String buildName(String origName) {
        String base = "image";
        if (origName != null && !origName.isEmpty()) {
            int dot = origName.lastIndexOf('.');
            base = dot > 0 ? origName.substring(0, dot) : origName;
            base = base.replaceAll("[^A-Za-z0-9_-]", "_");
            if (base.isEmpty()) {
                base = "image";
            }
        }
        String candidate = base + profile.extension();
        File f = new File(outDir, candidate);
        while (f.exists()) {
            candidate = base + "-" + (++seq) + profile.extension();
            f = new File(outDir, candidate);
        }
        return candidate;
    }

    private static int clampQuality(int q) {
        return Math.max(1, Math.min(100, q));
    }

    private byte[] readAll(Uri uri) {
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) {
                return null;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            // openInputStream may throw SecurityException for a revoked/ungranted
            // uri, or other unchecked errors; skip the image rather than crash.
            Log.e(TAG, "read failed", e);
            return null;
        }
    }

    private int rotationFromExif(byte[] data) {
        try {
            ExifInterface ei = new ExifInterface(new ByteArrayInputStream(data));
            switch (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void copyExif(byte[] srcData, File dest) {
        try {
            ExifInterface src = new ExifInterface(new ByteArrayInputStream(srcData));
            ExifInterface dst = new ExifInterface(dest.getAbsolutePath());
            for (String tag : PRESERVE_TAGS) {
                String value = src.getAttribute(tag);
                if (value != null) {
                    dst.setAttribute(tag, value);
                }
            }
            dst.saveAttributes();
        } catch (Exception e) {
            Log.e(TAG, "exif copy failed", e);
        }
    }

    private String sourceName(Uri uri) {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new File(uri.getPath()).getName();
        }
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return c.getString(idx);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Builds and launches a share chooser for the reduced files. */
    public static void share(Activity activity, List<Uri> uris, String mimeType, boolean includeDirect) {
        Intent intent;
        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        }
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(INTENT_FROM_ME, true);

        List<ResolveInfo> targets =
                activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : targets) {
            for (Uri uri : uris) {
                activity.grantUriPermission(ri.activityInfo.packageName, uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        Intent toStart;
        if (includeDirect) {
            toStart = Intent.createChooser(intent, activity.getString(R.string.share_chooser_title));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                toStart.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,
                        new ComponentName[]{new ComponentName(activity, SendReduced.class)});
            }
        } else {
            toStart = intent;
        }
        activity.startActivity(toStart);
    }

    /** Deletes shared cache directories older than {@link #CLEAN_TIME}. */
    public static void cleanCache(Context c, long now) {
        File base = new File(c.getApplicationContext().getCacheDir(), "shared");
        File[] dirs = base.listFiles();
        if (dirs == null) {
            return;
        }
        for (File d : dirs) {
            if (!d.isDirectory()) {
                continue;
            }
            try {
                long t = Long.parseLong(d.getName().split("-")[0]);
                if (Math.abs(t - now) >= CLEAN_TIME) {
                    deleteRecursive(d);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void deleteRecursive(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursive(c);
            }
        }
        f.delete();
    }
}
