package com.example.lab_1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button searchButton, likeButton, dislikeButton, loadMoreButton, openButton, downloadButton;
    private ImageView imageView;
    private ProgressBar progressBar;
    private final ArrayList<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;
    private int currentPage = 1;
    private String currentImageUrl = "";
    private final String UNSPLASH_API_KEY = "kYP0hFACDS8PhGaCwJ0kblQb39pmb1R9ZAmvW-MG3fs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        likeButton = findViewById(R.id.likeButton);
        dislikeButton = findViewById(R.id.dislikeButton);
        loadMoreButton = findViewById(R.id.loadMoreButton);
        openButton = findViewById(R.id.openButton);
        downloadButton = findViewById(R.id.downloadButton);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            if (!query.isEmpty()) {
                currentPage = 1;  // Сброс страницы
                new ImageSearchTask().execute(query);
            } else {
                Toast.makeText(MainActivity.this, "Введите запрос", Toast.LENGTH_SHORT).show();
            }
        });

        loadMoreButton.setOnClickListener(v -> loadNextImage());

        likeButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Лайк!", Toast.LENGTH_SHORT).show();
        });

        dislikeButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Дизлайк", Toast.LENGTH_SHORT).show();
        });

        openButton.setOnClickListener(v -> {
            if (!currentImageUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentImageUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(MainActivity.this, "Нет изображения для открытия", Toast.LENGTH_SHORT).show();
            }
        });

        downloadButton.setOnClickListener(v -> {
            if (!currentImageUrl.isEmpty()) {
                downloadImage(currentImageUrl);
            } else {
                Toast.makeText(MainActivity.this, "Нет изображения для загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNextImage() {
        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "Изображений не найдено", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentImageIndex >= imageUrls.size()) {
            currentImageIndex = 0;
        }
        currentImageUrl = imageUrls.get(currentImageIndex); // Запоминаем текущий URL изображения
        Glide.with(this)
                .load(currentImageUrl)
                .into(imageView);
        currentImageIndex++;
    }

    private void downloadImage(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        saveImageToStorage(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }

    private void saveImageToStorage(Bitmap bitmap) {
        String savedImagePath = null;
        String imageFileName = "IMAGE_" + System.currentTimeMillis() + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/UnsplashImages");

        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }

        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            try {
                FileOutputStream fOut = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
                Toast.makeText(this, "Загрузка завершена", Toast.LENGTH_LONG).show();
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImageSearchTask extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            imageUrls.clear();
            currentImageIndex = 0;
        }

        @Override
        protected ArrayList<String> doInBackground(String... queries) {
            String query = queries[0];
            ArrayList<String> resultUrls = new ArrayList<>();
            try {
                String searchUrl = "https://api.unsplash.com/search/photos?page=" + currentPage + "&query=" + query + "&client_id=" + UNSPLASH_API_KEY;
                URL url = new URL(searchUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();
                JSONObject jsonObject = new JSONObject(content.toString());
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject imageObject = results.getJSONObject(i);
                    String imageUrl = imageObject.getJSONObject("urls").getString("regular");
                    resultUrls.add(imageUrl);
                }
                currentPage++;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultUrls;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);
            if (!result.isEmpty()) {
                imageUrls.addAll(result);
                loadNextImage();
            } else {
                Toast.makeText(MainActivity.this, "Изображения не найдены", Toast.LENGTH_SHORT).show();
            }
        }
    }
}