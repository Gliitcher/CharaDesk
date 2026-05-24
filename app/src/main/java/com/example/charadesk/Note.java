package com.example.charadesk;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Note extends AppCompatActivity {
    private EditText titleText;
    private LinearLayout blocksContainer;
    private NoteData currentNote;
    private int noteIndex;
    private View currentOpenPanelBlock = null;
    private static final int REQUEST_PICK_IMAGE = 100;
    private static final int REQUEST_PICK_IMAGE_REPLACE = 101;
    private static final int REQUEST_CHANGE_AVATAR = 102;
    private NoteData.BlockData pendingImageBlock;
    private NoteData.BlockData replaceImageBlock;
    private ImageView avatarView;
    private String currentProfile;
    private String currentRole;
    private short click = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainMenu.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        avatarView = findViewById(R.id.note_avatar);
        titleText = findViewById(R.id.title);
        blocksContainer = findViewById(R.id.linear);
        noteIndex = getIntent().getIntExtra("note_index", -1);
        currentRole = getIntent().getStringExtra("role");
        if (currentRole == null) currentRole = MainMenu.ROLE_WRITER;
        currentProfile = getIntent().getStringExtra("profile_name");
        if (noteIndex == -1 || noteIndex >= MainMenu.notesList.size()) {
            finish();
            return;
        }
        currentNote = MainMenu.notesList.get(noteIndex);

        titleText.setText(currentNote.getTitle());
        titleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentNote.setTitle(s.toString().trim());
            }
        });
        for (NoteData.BlockData block : currentNote.getBlocks()) {
            addBlockView(block);
        }
        if (currentNote.getAvatarPath() != null) {
            File avatarFile = new File(currentNote.getAvatarPath());
            if (avatarFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(currentNote.getAvatarPath(), options);
                avatarView.setImageBitmap(bitmap);
            }
        }
        if (currentRole.equals(MainMenu.ROLE_READER)) {
            setReadOnlyMode();
        }
    }

    private void setReadOnlyMode() {
        titleText.setEnabled(false);
        titleText.setFocusable(false);
        avatarView.setClickable(false);
        avatarView.setEnabled(false);
        ImageButton addButton = findViewById(R.id.button_add);
        if (addButton != null) addButton.setVisibility(View.GONE);
    }

    // Добавление блока
    public void onClickAddElement(View view) {
        // Показываем диалог выбора типа блока
        String[] blockTypes = {getString(R.string.single_block), getString(R.string.title_block),
                getString(R.string.image_block)};
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.add_block))
                .setItems(blockTypes, (dialog, which) -> {
                    String type = "";
                    if (which == 0) {
                        type = "multy";
                    } else if (which == 1) {
                        type = "multiline";
                    } else if (which == 2) {
                        type = "image";
                    }
                    if (type!="image") {
                        NoteData.BlockData newBlock = new NoteData.BlockData(type, "", "");
                        currentNote.addBlock(newBlock);
                        addBlockView(newBlock);
                    }
                    else if (type =="image"){
                        NoteData.BlockData newBlock = new NoteData.BlockData("image", null);
                        currentNote.addBlock(newBlock);
                        addBlockView(newBlock); // отобразится плейсхолдер
                        MainMenu.saveNotesToFile(this, currentProfile);
                        pickImageForNewBlock(newBlock);
                    }
                })
                .show();
    }
    private void pickImageForNewBlock(NoteData.BlockData block) {
        pendingImageBlock = block;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void pickImageForReplace(NoteData.BlockData block) {
        replaceImageBlock = block;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE_REPLACE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Uri selectedImageUri = data.getData();
        if (selectedImageUri == null) return;

        if (requestCode == REQUEST_PICK_IMAGE && pendingImageBlock != null) {
            String savedPath = saveImageToInternalStorage(selectedImageUri);
            if (savedPath != null) {
                pendingImageBlock.setImagePath(savedPath);
                refreshBlocks();
                MainMenu.saveNotesToFile(this, currentProfile);
            } else {
                currentNote.getBlocks().remove(pendingImageBlock);
                refreshBlocks();
                Toast.makeText(this, getString(R.string.error_image), Toast.LENGTH_SHORT).show();
            }
            pendingImageBlock = null;
        }
        else if (requestCode == REQUEST_PICK_IMAGE_REPLACE && replaceImageBlock != null) {
            String oldPath = replaceImageBlock.getImagePath();
            if (oldPath != null) new File(oldPath).delete();
            String newPath = saveImageToInternalStorage(selectedImageUri);
            if (newPath != null) {
                replaceImageBlock.setImagePath(newPath);
                refreshBlocks();
                MainMenu.saveNotesToFile(this, currentProfile);
            } else {
                Toast.makeText(this, getString(R.string.error_image_change), Toast.LENGTH_SHORT).show();
            }
            replaceImageBlock = null;
        } else if (requestCode == REQUEST_CHANGE_AVATAR) {
            String oldPath = currentNote.getAvatarPath();
            if (oldPath != null) new File(oldPath).delete();
            String newPath = saveImageToInternalStorage(selectedImageUri);
            if (newPath != null) {
                currentNote.setAvatarPath(newPath);
                // Обновляем ImageView аватара
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(newPath, options);
                avatarView.setImageBitmap(bitmap);
                MainMenu.saveNotesToFile(this, currentProfile);
            } else {
                Toast.makeText(this, getString(R.string.error_avatar), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            File imagesDir = new File(getFilesDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
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

    private void addBlockView(NoteData.BlockData block) {
        View blockView;
        boolean isReader = currentRole.equals(MainMenu.ROLE_READER);

        if (block.getType().equals("multy")) {
            blockView = LayoutInflater.from(this).inflate(R.layout.note_single_multy, blocksContainer, false);
            EditText editText = blockView.findViewById(R.id.note_text);
            editText.setText(block.getText());
            if (isReader) {
                editText.setEnabled(false);
                editText.setFocusable(false);
            } else {
                attachTextWatcher(editText, block, "Text");
            }
        }
        else if (block.getType().equals("multiline")) {
            blockView = LayoutInflater.from(this).inflate(R.layout.note_multy, blocksContainer, false);
            EditText editTitle = blockView.findViewById(R.id.title_text);
            EditText editText = blockView.findViewById(R.id.note_text);
            editTitle.setText(block.getTitle());
            editText.setText(block.getText());
            if (isReader) {
                editTitle.setEnabled(false);
                editTitle.setFocusable(false);
                editText.setEnabled(false);
                editText.setFocusable(false);
            } else {
                attachTextWatcher(editTitle, block, "Title");
                attachTextWatcher(editText, block, "Text");
            }
        }
        else if (block.getType().equals("image")) {
            blockView = LayoutInflater.from(this).inflate(R.layout.note_image, blocksContainer, false);
            ImageView imageView = blockView.findViewById(R.id.block_image);
            String imagePath = block.getImagePath();
            if (imagePath != null && new File(imagePath).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder);
            }
            if (isReader) {
                imageView.setClickable(false);
                imageView.setEnabled(false);
            } else {
                imageView.setOnClickListener(v -> pickImageForReplace(block));
            }
            EditText editTitle = blockView.findViewById(R.id.title_text);
            editTitle.setText(block.getTitle());
            if (isReader) {
                editTitle.setEnabled(false);
                editTitle.setFocusable(false);
            } else {
                attachTextWatcher(editTitle, block, "Title");
            }
        } else {
            blockView = null;
        }

        if (blockView != null) {
            // Управление панелью кнопок (button_panel)
            ImageButton buttonPanel = blockView.findViewById(R.id.note_menu);
            if (buttonPanel != null) {
                if (isReader) {
                    buttonPanel.setVisibility(View.GONE);
                }
            }
            blocksContainer.addView(blockView);
        }
    }

    // Вспомогательный метод, который вешает TextWatcher на EditText и сохраняет текст в BlockData
    private void attachTextWatcher(EditText editText, NoteData.BlockData block, String type) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (type.equals("Title")) {
                    block.setTitle(s.toString());
                } else if (type.equals("Text")) {
                    block.setText(s.toString());
                }
                MainMenu.saveNotesToFile(Note.this, currentProfile);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClickChangeImage(View view){
        if (!currentRole.equals(MainMenu.ROLE_READER)) {
            View blockCard = getBlockCardView(view);
            int position = blocksContainer.indexOfChild(blockCard);
            pickImageForReplace(currentNote.getBlocks().get(position));
        }
    }

    public void onClickChangeAvatar(View view) {
        if (!currentRole.equals(MainMenu.ROLE_READER)) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CHANGE_AVATAR);
        }
        else {
            click++;
            if (click == 5){
                Toast.makeText(this, "0" , Toast.LENGTH_SHORT).show();
                MainMenu.iforget();
            }
        }
    }

    public void onClickUp(View view) {
        View blockCard = getBlockCardView(view);
        int position = blocksContainer.indexOfChild(blockCard);
        if (position > 0) {
            NoteData.BlockData block = currentNote.getBlocks().remove(position);
            currentNote.getBlocks().add(position - 1, block);
            refreshBlocks();
        }
    }

    public void onClickDown(View view) {
        View blockCard = getBlockCardView(view);
        int position = blocksContainer.indexOfChild(blockCard);
        if (position < currentNote.getBlocks().size() - 1) {
            NoteData.BlockData block = currentNote.getBlocks().remove(position);
            currentNote.getBlocks().add(position + 1, block);
            refreshBlocks();
        }
    }

    public void onClickDelete(View view) {
        View blockCard = getBlockCardView(view);
        int position = blocksContainer.indexOfChild(blockCard);
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.delete_block))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    if (currentNote.getBlocks().get(position).getType().equals("image")) {
                        String path = currentNote.getBlocks().get(position).getImagePath();
                        if (path != null) new File(path).delete();
                    }
                    currentNote.getBlocks().remove(position);
                    refreshBlocks();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    public void onClickMenu(View view) {
        View blockCard = getBlockCardView(view);
        LinearLayout buttonPanel = blockCard.findViewById(R.id.button_panel);
        if (buttonPanel == null) return;
        if (currentOpenPanelBlock != null && currentOpenPanelBlock != blockCard) {
            LinearLayout oldPanel = currentOpenPanelBlock.findViewById(R.id.button_panel);
            if (oldPanel != null) {
                oldPanel.setVisibility(View.GONE);
            }
        }
        if (buttonPanel.getVisibility() == View.GONE) {
            buttonPanel.setVisibility(View.VISIBLE);
            currentOpenPanelBlock = blockCard;
        } else {
            buttonPanel.setVisibility(View.GONE);
            currentOpenPanelBlock = null;
        }
    }

    private View getBlockCardView(View button) {
        View parent = (View) button.getParent();
        while (!(parent.getParent() instanceof androidx.cardview.widget.CardView)) {
            parent = (View) parent.getParent();
        }
        return (View) parent.getParent();
    }

    private void refreshBlocks() {
        blocksContainer.removeAllViews();
        currentOpenPanelBlock = null;
        for (NoteData.BlockData block : currentNote.getBlocks()) {
            addBlockView(block);
        }
        MainMenu.saveNotesToFile(this, currentProfile);
    }
    @Override
    protected void onPause() {
        super.onPause();
        MainMenu.saveNotesToFile(this, currentProfile);
    }
    public void onClickBack(View view) {
        finish();
    }
}