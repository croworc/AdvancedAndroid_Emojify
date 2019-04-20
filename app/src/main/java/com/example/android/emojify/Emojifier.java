package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

class Emojifier {
    private static final String LOG_TAG = "Emojifier.class";

    static void detectFaces(Context context, Bitmap bitmap) {
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                // .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        SparseArray<Face> faces = detector.detect(frame);

        Log.d(LOG_TAG, "Number of faces detected: " + faces.size());

        // Show a toast in case we couldn't detect any faces
        if (faces.size() == 0) {
            Toast.makeText(context, context.getString(R.string.no_faces_detected), Toast.LENGTH_SHORT)
                    .show();
        }

        detector.release();
    }
}
