package com.filepicker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

public class FilePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    static final int REQUEST_LAUNCH_FILE_CHOOSER = 203040;

    private final ReactApplicationContext mReactContext;

    private Promise mPromise;

    FilePickerModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);

        mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return "FilePickerManager";
    }

    @ReactMethod
    public void pickFile(final ReadableMap options, final Promise promise) {
        int requestCode;
        Intent libraryIntent;

        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject(new Error("Cannot find current activity"));
            return;
        }

        String fileType, dialogTitle;

        if (options.hasKey("type")) {
            fileType = options.getString("type");
        } else {
            fileType = "*/*";
        }

        if (options.hasKey("title")) {
            dialogTitle = options.getString("title");
        } else {
            dialogTitle = "Select a file";
        }

        libraryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        libraryIntent.setType(fileType);
        libraryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            promise.reject(new Error("Cannot launch file library"));
            return;
        }

        mPromise = promise;

        try {
            currentActivity.startActivityForResult(Intent.createChooser(libraryIntent, dialogTitle), REQUEST_LAUNCH_FILE_CHOOSER);
        } catch (ActivityNotFoundException e) {
            mPromise.reject(e);
            mPromise = null;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        if (requestCode != REQUEST_LAUNCH_FILE_CHOOSER || mPromise == null) {
            return;
        }

        WritableMap response = Arguments.createMap();

        if (resultCode == Activity.RESULT_CANCELED) {
            response.putBoolean("cancelled", true);
            mPromise.resolve(response);
            mPromise = null;
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = getPath(activity, uri);
            response.putString("uri", uri.toString());
            if (path != null) {
                response.putString("path", path);
            }
            mPromise.resolve(response);
            mPromise = null;
            return;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

}