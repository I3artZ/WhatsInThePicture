package com.bartz.whatsInThePicture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import java.io.IOException;
import java.util.List;

class PictureHandler {

    static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    static float getPictureRotationFromCamera(String currentPhotoPath) {
        int rotation;
        try {
            ExifInterface ei = new ExifInterface(currentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotation = 0;
            }
            return rotation;
        } catch (IOException e) {
            Log.wtf("getting orientation ", e);
            return 0;
        }
    }

    static void setPic(ImageView imageView, String currentPhotoPath, TextView textView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // convert the image file into a Bitmap sized to fill the image_view
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        //get rotation and rotate bitmap
        float rotation = PictureHandler.getPictureRotationFromCamera(currentPhotoPath);
        bitmap = PictureHandler.rotateBitmap(bitmap, rotation);

        //populate image_view with taken picture
        imageView.setImageBitmap(bitmap);

        analyzePicture(bitmap, textView);

    }

    static void analyzePicture(Bitmap bitmap,  final TextView textView) {
        FirebaseVisionImage firebaseVisionImage;
        firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        //create labeler
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();

        //pass image to labeler and decide on action to be taken
        labeler.processImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        StringBuilder result = new StringBuilder();

                        for (FirebaseVisionImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            result.append(text).append(" - ").append((int) Math.ceil(confidence * 100)).append("% | ");
                        }
                        textView.setText(result);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.wtf("labaler error", e);
                        textView.setText("no matches for your picture!");
                    }
                });
    }


}
