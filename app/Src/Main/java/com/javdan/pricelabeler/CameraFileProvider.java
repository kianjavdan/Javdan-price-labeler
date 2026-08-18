package com.javdan.pricelabeler;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CameraFileProvider extends ContentProvider {

    public static Uri uriForFile(Context context, File file) throws IOException {
        File base = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "camera_capture");
        String basePath = base.getCanonicalPath() + File.separator;
        String target = file.getCanonicalPath();
        if (!target.startsWith(basePath)) throw new SecurityException("Camera file outside allowed directory");
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".camera.provider")
                .appendPath("camera")
                .appendPath(file.getName())
                .build();
    }

    private File fileForUri(Uri uri) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException("Context unavailable");
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new FileNotFoundException("Invalid camera file");
        }
        File base = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "camera_capture");
        File file = new File(base, name);
        try {
            String basePath = base.getCanonicalPath() + File.separator;
            String target = file.getCanonicalPath();
            if (!target.startsWith(basePath)) throw new FileNotFoundException("Invalid path");
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
        return file;
    }

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) { return "image/jpeg"; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = fileForUri(uri);
        int flags = mode != null && mode.contains("w")
                ? ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_WRITE_ONLY
                : ParcelFileDescriptor.MODE_READ_ONLY;
        return ParcelFileDescriptor.open(file, flags);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File f = fileForUri(uri);
            String[] cols = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            MatrixCursor c = new MatrixCursor(cols, 1);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
                else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
                else row[i] = null;
            }
            c.addRow(row);
            return c;
        } catch (Exception e) {
            return new MatrixCursor(projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, 0);
        }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
