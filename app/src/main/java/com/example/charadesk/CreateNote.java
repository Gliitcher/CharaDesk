package com.example.charadesk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CreateNote extends AppCompatActivity {
    private static final int REQUEST_PICK_AVATAR = 200;
    private EditText titleNote;
    private ImageView avatarPreview;
    private String selectedAvatarPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainMenu.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        titleNote = findViewById(R.id.note_title);
        avatarPreview = findViewById(R.id.avatar_preview);
    }

    public void onClickSelectAvatar(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_AVATAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_AVATAR && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                selectedAvatarPath = saveImageToInternalStorage(selectedImage, "avatar");
                if (selectedAvatarPath != null) {
                    avatarPreview.setImageURI(selectedImage);
                } else {
                    avatarPreview.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
        }
    }

    private String saveImageToInternalStorage(Uri imageUri, String prefix) {
        try {
            File imagesDir = new File(getFilesDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();
            String fileName = prefix + "_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(imagesDir, fileName);
            InputStream in = getContentResolver().openInputStream(imageUri);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            in.close();
            out.close();
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onClickSave(View view) {
        String title = titleNote.getText().toString().trim();
        if (title.isEmpty()) title = getString(R.string.untitle);

        Spinner templateSpinner = findViewById(R.id.template_spinner);
        int selectedTemplate = templateSpinner.getSelectedItemPosition();

        NoteData newNote = new NoteData(title);
        if (selectedAvatarPath != null) {
            newNote.setAvatarPath(selectedAvatarPath);
        }

        switch (selectedTemplate) {
            case 1: // Базовый
                newNote.addBlock(new NoteData.BlockData("multy", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Характер", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Биография", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Способности", ""));
                break;
            case 2: // Локация
                newNote.addBlock(new NoteData.BlockData("multy", ""));
                newNote.addBlock(new NoteData.BlockData("image", "Вид", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Описание", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "История", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Её особенность", ""));
                break;
            case 3: // Рецепт
                newNote.addBlock(new NoteData.BlockData("multy", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Ингридиенты", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Приготовление", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Особенности", ""));
                newNote.addBlock(new NoteData.BlockData("image", "Продукт", ""));
                break;
            case 4: // Чудо-юдо
                newNote.addBlock(new NoteData.BlockData("multy", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Способности", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Характер", ""));
                newNote.addBlock(new NoteData.BlockData("image", "Высшая форма", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Эволюция", ""));
                newNote.addBlock(new NoteData.BlockData("multiline", "Особенности", ""));
                break;
            default: // Пустой – блоков не добавляем
                break;
        }

        Gson gson = new Gson();
        String blocksJson = gson.toJson(newNote.getBlocks());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("note_title", title);
        if (selectedAvatarPath != null) {
            resultIntent.putExtra("avatar_path", selectedAvatarPath);
        }
        resultIntent.putExtra("blocks_json", blocksJson);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    public void onClickBack(View view) {
        finish();
    }
}