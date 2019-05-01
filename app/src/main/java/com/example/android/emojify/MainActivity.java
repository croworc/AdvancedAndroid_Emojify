/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.emojify;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String EXTRA_PHOTO_PATH = "extra_photo_path";

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    // COMPLETED (2): Replace all View declarations with Butterknife annotations
    @BindView(R.id.title_text_view) TextView             mTitleTextView;
    @BindView(R.id.image_view)      ImageView            mImageView;
    @BindView(R.id.emojify_button)  Button               mEmojifyButton;
    @BindView(R.id.share_button)    FloatingActionButton mShareFab;
    @BindView(R.id.save_button)     FloatingActionButton mSaveFab;
    @BindView(R.id.clear_button)    FloatingActionButton mClearFab;

    private String mTempPhotoPath;
    private Bitmap mResultsBitmap;
    private ExifInterface exif; // to obtain photo orientation info, so we can possibly
                                // rotate the image appropriately


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restore the path string to the temporary photo file, if it has been saved.
        if (savedInstanceState != null) {
            mTempPhotoPath = savedInstanceState.getString(EXTRA_PHOTO_PATH);
        }

        // COMPLETED (3): Replace the findViewById calls with the Butterknife data binding
        // Bind the views
        ButterKnife.bind(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the path string to the temporary photo file.
        outState.putString(EXTRA_PHOTO_PATH, mTempPhotoPath);
    }

    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     *
     * @param view The emojify me button.
     */
    @OnClick(R.id.emojify_button)
    public void emojifyMe(View view) {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    } // close method emojifyMe()

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    } // close method onRequestPermissionsResult()

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } // close: if (we've got at least one activity which can handle the 'take photo' intent)
    } // close method launchCamera()


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE) {

            if (resultCode == RESULT_OK) {

                // Try to obtain an exif interface to the photo, so we can retrieve the orientation
                //  information.
                try {
                    exif = new ExifInterface(mTempPhotoPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Delete the temporary image file
                    BitmapUtils.deleteImageFile(this, mTempPhotoPath);
                    Toast.makeText(this, "Couldn't obtain exif info for the photo", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                // Process the image and set it to the TextView
                processAndSetImage();

            } else { // result code was not "OK"...
                Toast.makeText(this, "Couldn't make a photo.", Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "onActivityResult: result code = " + resultCode);

                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);

            } // close: if (resultCode == RESULT_OK) / else

        } // close: if (requestCode == REQUEST_IMAGE_CAPTURE)

    } // close method onActivityResult()

    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.show();
        mShareFab.show();
        mClearFab.show();

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);

        // Get the photo's orientation, so that we can rotate it appropriately, if required.
        int photoOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        // Rotate the bitmap, if required
        mResultsBitmap = BitmapUtils.rotateBitmap(mResultsBitmap, photoOrientation);

        // Detect the faces and overlay the appropriate emoji
        mResultsBitmap = Emojifier.detectFacesandOverlayEmoji(this, mResultsBitmap);

        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);
    }


    // TODO (4): Replace OnClick methods with Butterknife annotations for OnClicks
    /**
     * OnClick method for the save button.
     *
     * @param view The save button.
     */
    @OnClick(R.id.save_button)
    public void saveMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
    }

    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     *
     * @param view The share button.
     */
    @OnClick(R.id.share_button)
    public void shareMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     *
     * @param view The clear button.
     */
    @OnClick(R.id.clear_button)
    public void clearImage(View view) {
        // Clear the image and toggle the view visibility
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mShareFab.hide();
        mSaveFab.hide();
        mClearFab.hide();

        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
    }
}
