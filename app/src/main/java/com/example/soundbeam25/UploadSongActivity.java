package com.example.soundbeam25;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class UploadSongActivity extends AppCompatActivity {
    Uri uriSong, image;
    byte[] bytes;
    String fileName, songUrl, imageUrl;
    String songLength;
    private StorageReference storageReference;
    ProgressDialog progressDialog;
    EditText selectSongNameEditText;
    EditText artistName;
    ImageView selectImage;
    Button uploadButton;
    ImageButton selectSong;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Upload Song");
        storageReference = FirebaseStorage.getInstance().getReference();
        progressDialog = new ProgressDialog(this);

        selectSongNameEditText = findViewById(R.id.selectSong);
        selectImage = findViewById(R.id.selectImage);
        uploadButton = findViewById(R.id.uploadSongButton);
        artistName = findViewById(R.id.artistNameEditText);
        selectSong = findViewById(R.id.selectSongButton);

        selectSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickSong();
            }
        });

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });
    }

    private void pickSong() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent,1);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,2);

    }

    //after selecting song from internal storage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data !=null) {
            if (requestCode == 1 && resultCode == RESULT_OK){
                uriSong = data.getData();
                fileName = getFileName(uriSong);
                selectSongNameEditText.setText(fileName);
                songLength = getSongDuration(uriSong);
                Log.i("duration", songLength);
            }
            if (requestCode == 2 && resultCode == RESULT_OK){
                image = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image);
                    selectImage.setImageBitmap(bitmap);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                    bytes = byteArrayOutputStream.toByteArray();

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }


    public void upload(View view){
        if(uriSong == null){
            Toast.makeText(this, "Please select a song", Toast.LENGTH_SHORT).show();
        }
        else if (selectSongNameEditText.getText().toString().equals("")){
            Toast.makeText(this, "Song name cannot be empty!", Toast.LENGTH_SHORT).show();
        }
        else if(artistName.getText().toString().equals("")){
            Toast.makeText(this, "Please enter song language", Toast.LENGTH_SHORT).show();
        }
        else if (image == null){
            Toast.makeText(this, "Please select a Thumbnail", Toast.LENGTH_SHORT).show();
        }
        else {
            fileName = selectSongNameEditText.getText().toString();
            String artist = artistName.getText().toString();
            uploadImageToServer(bytes,fileName);
            uploadFileToServer(uriSong,fileName,artist,songLength);
        }

    }

    private void uploadImageToServer(byte[] bytes, String fileName) {
        UploadTask uploadTask = storageReference.child("Thumbnails").child(fileName).putBytes(bytes);
        progressDialog.show();
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                while (!task.isComplete());
                Uri urlsong = task.getResult();
                imageUrl = urlsong.toString();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("image url", "failed");
            }
        });

    }

    private void uploadFileToServer(Uri uriSong, String fileName, String artist, String songLength) {
        StorageReference filePath = storageReference.child("Audios").child(fileName);
        progressDialog.show();
        filePath.putFile(uriSong).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri urlSong = uriTask.getResult();
                songUrl = urlSong.toString();

                uploadDetailsToDatabase(fileName,songUrl,imageUrl,artist,songLength);



            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0*snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                int currentProgress = (int) progress;
                progressDialog.setMessage("Uploading: " + currentProgress + "%");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Upload Failed! Please Try again!", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void uploadDetailsToDatabase(String fileName, String songUrl, String imageUrl, String artist, String songLength) {

        Song song = new Song(fileName,songUrl, imageUrl, artist, songLength);
        FirebaseDatabase.getInstance().getReference("Songs")
                .push().setValue(song).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.i("database", "upload success");
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Song Uploaded to Database", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });


    }


    private String getFileName(Uri uriSong) {
        String result = null;
        if (Objects.equals(uriSong.getScheme(),"content")){
            try (Cursor cursor = getContentResolver().query(uriSong, null, null, null, null)){
                if (cursor != null && cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null){
            result = uriSong.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1){
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private String getSongDuration(Uri uriSong) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getApplicationContext(),uriSong);
        String durationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long time = Long.parseLong(durationString);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(time);
        int totalSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(time);
        int seconds = totalSeconds-(minutes*60);
        if (String.valueOf(seconds).length() == 1){
            return minutes + ":0" + seconds;
        }else {
            return minutes + ":" + seconds;
        }

    }
}
