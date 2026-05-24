package com.example.charadesk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainMenu extends AppCompatActivity {
    //Коды запросос
    private static final int REQUEST_CREATE_NOTE = 1;
    private static final int REQUEST_EXPORT = 100;
    private static final int REQUEST_IMPORT = 101;
    private static final int REQUEST_ACCOUNTS = 300;
    public static final String EXTRA_PROFILE_NAME = "profile_name";

    private String currentProfile = "notes";// Поле для текущего имени профиля
    public static List<NoteData> notesList = new ArrayList<>();//Список заметок
    private LinearLayout notesContainer;//Контейнер для заметок
    private View currentOpenPanelNote = null;
    private String pendingExportJson = null;
    public static final String PREFS_NAME = "settings";
    public static final String KEY_ROLE = "user_role";
    public static final String KEY_NOTES = "saved_notes";
    public static final String KEY_THEME = "theme";
    public static final String KEY_PASSWORD = null;
    public static final String ROLE_WRITER = "writer";
    public static final String ROLE_READER = "reader";
    private String currentRole;
    private static String currentTheme;
    private static String password;

    public static void applyTheme(Context context){
        if (currentTheme.equals("LAVENDER")) {
            context.setTheme(R.style.Theme_Lavander);
        } else if (currentTheme.equals("HIBISCUS")) {
            context.setTheme(R.style.Theme_Hibiscus);
        } else if (currentTheme.equals("MOLUCELLA")) {
            context.setTheme(R.style.Theme_Molucella);
        } else if (currentTheme.equals("DANDELION")) {
            context.setTheme(R.style.Theme_Dandelion);
        } else if (currentTheme.equals("FORGETMENOT")) {
            context.setTheme(R.style.Theme_ForgetMeNot);
        } else {
            context.setTheme(R.style.Theme_Lavander);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentRole = prefs.getString(KEY_ROLE, ROLE_WRITER);
        currentProfile = prefs.getString(KEY_NOTES, currentProfile);
        password = prefs.getString(KEY_PASSWORD, password);
        currentTheme = prefs.getString(KEY_THEME, "LAVENDER");

        applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateRoleIcon();

        TextView universes = findViewById(R.id.universe);
        universes.setText(currentProfile);

        notesContainer = findViewById(R.id.linear);
        loadNotesFromFile(currentProfile);
        refreshNotesList();
        updateUIBasedOnRole();
    }

    private void refreshNotesList() {
        notesContainer.removeAllViews();
        currentOpenPanelNote = null;
        for (int i = 0; i < notesList.size(); i++) {
            addNoteViewToContainer(notesList.get(i), i);
        }
    }
    // Метод добавления заметки в контейнер
    private void addNoteViewToContainer(NoteData note, final int position) {
        View noteView = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false);
        TextView noteText = noteView.findViewById(R.id.note_text);
        ImageView avatar = noteView.findViewById(R.id.avatar);
        noteText.setText(note.getTitle());
        if (note.getAvatarPath() != null) {
            File avatarFile = new File(note.getAvatarPath());
            if (avatarFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(note.getAvatarPath(), options);
                avatar.setImageBitmap(bitmap);
            }
        }

        noteView.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, Note.class);
            intent.putExtra("note_index", position);
            intent.putExtra("profile_name", currentProfile);
            intent.putExtra("role", currentRole);
            startActivity(intent);
        });
        notesContainer.addView(noteView);

        ImageButton menuButton = noteView.findViewById(R.id.note_menu);
        if (currentRole.equals(ROLE_READER)) {
            if (menuButton != null) menuButton.setVisibility(View.GONE);
        } else {
            if (menuButton != null) menuButton.setVisibility(View.VISIBLE);
        }
    }

    //Метод создания заметки
    public void onClickCreateNewNote(View view) {
        Intent intent = new Intent(this, CreateNote.class);
        startActivityForResult(intent, REQUEST_CREATE_NOTE);
    }
    //Сохранение заметок в файл
    public static void saveNotesToFile(Context context, String profileName) {
        String fileName = "notes_" + profileName + ".json";
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), fileName))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(notesList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCurrentProfile() {
        saveNotesToFile(this, currentProfile);
    }

    // Загружает список заметок из файла
    private void loadNotesFromFile(String profileName) {
        String fileName = "notes_" + profileName + ".json";
        File file = new File(getFilesDir(), fileName);
        if (!file.exists()) {
            notesList = new ArrayList<>();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<NoteData>>(){}.getType();
            notesList = gson.fromJson(reader, type);
            if (notesList == null) notesList = new ArrayList<>();
        } catch (Exception e) {
            notesList = new ArrayList<>();
            e.printStackTrace();
        }
    }
    private void saveAndRefresh() {
        saveCurrentProfile();
        refreshNotesList();
    }
    // Обработчик результатов
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_NOTE && resultCode == RESULT_OK) {
            String title = data.getStringExtra("note_title");
            String avatarPath = data.getStringExtra("avatar_path");
            NoteData newNote = new NoteData(title);
            if (avatarPath != null) newNote.setAvatarPath(avatarPath);
            notesList.add(newNote);
            saveAndRefresh();
        }
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQUEST_EXPORT && pendingExportJson != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri);
                     Writer writer = new OutputStreamWriter(out)) {
                    writer.write(pendingExportJson);
                    Toast.makeText(this, getString(R.string.export_note), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.error_export_note), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                pendingExportJson = null;
            }
        } else if (requestCode == REQUEST_IMPORT) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     Reader reader = new InputStreamReader(in)) {
                    Gson gson = new Gson();
                    NoteData importedNote = gson.fromJson(reader, NoteData.class);
                    if (importedNote != null) {
                        notesList.add(importedNote);
                        saveAndRefresh();
                        Toast.makeText(this, getString(R.string.import_note), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.import_note_incorrect), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.error_import_note), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == REQUEST_ACCOUNTS && resultCode == RESULT_OK && data != null) {
            String newProfile = data.getStringExtra(EXTRA_PROFILE_NAME);
            if (newProfile != null && !newProfile.equals(currentProfile)) {
                saveCurrentProfile();
                currentProfile = newProfile;
                TextView universes = findViewById(R.id.universe);
                universes.setText(currentProfile);
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                        .putString("current_profile", currentProfile).apply();
                loadNotesFromFile(currentProfile);
                refreshNotesList();
                Toast.makeText(this, getString(R.string.universe_changed) + currentProfile, Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Метод для смены профиля
    public void onClickChangeUniverse(View view) {
        Intent intent = new Intent(this, Universe.class);
        intent.putExtra("current_profile", currentProfile);
        startActivityForResult(intent, REQUEST_ACCOUNTS);
    }
    // Перемещение заметки вверх
    public void onClickUp(View view) {
        View noteCard = getNoteCardView(view);
        int position = notesContainer.indexOfChild(noteCard);
        if (position > 0) {
            // Меняем местами в списке
            NoteData note = notesList.remove(position);
            notesList.add(position - 1, note);
            saveAndRefresh();
        }
    }
    // Перемещение заметки вниз
    public void onClickDown(View view) {
        View noteCard = getNoteCardView(view);
        int position = notesContainer.indexOfChild(noteCard);
        if (position < notesList.size() - 1) {
            NoteData note = notesList.remove(position);
            notesList.add(position + 1, note);
            saveAndRefresh();
        }
    }
    // Удаление заметки с подтверждением
    public void onClickDelete(View view) {
        View noteCard = getNoteCardView(view);
        int position = notesContainer.indexOfChild(noteCard);
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.delete_note))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    notesList.remove(position);
                    saveAndRefresh();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    // Управление меню
    public void onClickMenu(View view) {
        View noteCard = getNoteCardView(view);
        LinearLayout buttonPanel = noteCard.findViewById(R.id.button_panel);
        if (buttonPanel == null) return;
        if (currentOpenPanelNote != null && currentOpenPanelNote != noteCard) {
            LinearLayout oldPanel = currentOpenPanelNote.findViewById(R.id.button_panel);
            if (oldPanel != null) oldPanel.setVisibility(View.GONE);
        }
        if (buttonPanel.getVisibility() == View.GONE && !currentRole.equals(ROLE_READER)) {
            buttonPanel.setVisibility(View.VISIBLE);
            currentOpenPanelNote = noteCard;
        } else {
            buttonPanel.setVisibility(View.GONE);
            currentOpenPanelNote = null;
        }
    }
    //Экспорт заметки
    public void onClickExport(View view) {
        View noteCard = getNoteCardView(view);
        int position = notesContainer.indexOfChild(noteCard);
        NoteData note = notesList.get(position);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(note);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        String fileName = "note_" + note.getTitle() + ".json";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        pendingExportJson = json;
        startActivityForResult(intent, REQUEST_EXPORT);
    }
    // Импорт заметок
    public void onClickImport(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    public void onClickChangeTheme(View view){
        String[] theme = {getString(R.string.lavender), getString(R.string.hibiscus),
                getString(R.string.molucella),getString(R.string.dandelion),
                getString(R.string.forget_me_not)};
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.change_theme))
                .setItems(theme, (dialog, which) ->{
                    String newTheme = "";
                    if (which == 0)
                            newTheme = "LAVENDER";
                    else if (which == 1)
                        newTheme = "HIBISCUS";
                    else if (which == 2)
                        newTheme = "MOLUCELLA";
                    else if (which == 3)
                        newTheme = "DANDELION";
                    else if (which == 4)
                        newTheme = "FORGETMENOT";
                    currentTheme = newTheme;
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_THEME, currentTheme)
                            .apply();
                    recreate();
                })
                .show();
    }

    // Нахождение корневой CardView заметки
    private View getNoteCardView(View button) {
        View parent = (View) button.getParent();
        while (!(parent.getParent() instanceof androidx.cardview.widget.CardView)) {
            parent = (View) parent.getParent();
        }
        return (View) parent.getParent();
    }

    private void updateRoleIcon() {
        ImageButton toggleButton = findViewById(R.id.button_toggle_role);
        if (toggleButton != null) {
            if (currentRole.equals(ROLE_READER)) {
                toggleButton.setImageResource(R.drawable.icon_writter);
            } else {
                toggleButton.setImageResource(R.drawable.icon_reader);
                }
        }
    }

    public void onClickToggleRole(View view) {
        android.widget.EditText input = new android.widget.EditText(this);
        if (currentRole.equals(ROLE_WRITER)) {
            new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                    .setTitle(getString(R.string.password))
                    .setView(input)
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        password = input.getText().toString().trim();
                        currentRole = ROLE_READER;
                        applySwitch();
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        } else {
            new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                    .setTitle(getString(R.string.password))
                    .setView(input)
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        if (password.equals(input.getText().toString().trim())) {
                            currentRole = ROLE_WRITER;
                            applySwitch();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        }
    }

    public void applySwitch(){
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_ROLE, currentRole)
                .putString(KEY_NOTES, currentProfile)
                .putString(KEY_PASSWORD, password)
                .apply();
        Toast.makeText(this, currentRole.equals(ROLE_READER) ? "Режим чтения" : "Режим редактирования", Toast.LENGTH_SHORT).show();
        recreate();
    }

    public static void iforget(){
        password = "0";
    }

    private void updateUIBasedOnRole() {
        ImageButton btnCreate = findViewById(R.id.button_add);
        ImageButton btnImport = findViewById(R.id.button_import);
        ImageButton btnAccounts = findViewById(R.id.button_universe);
        ImageButton btnTheme = findViewById(R.id.button_change_theme);
        if (currentRole.equals(ROLE_READER)) {
            if (btnCreate != null) btnCreate.setVisibility(View.GONE);
            if (btnImport != null) btnImport.setVisibility(View.GONE);
            if (btnAccounts != null) btnAccounts.setVisibility(View.GONE);
            if (btnTheme != null) btnTheme.setVisibility(View.GONE);
        } else {
            if (btnCreate != null) btnCreate.setVisibility(View.VISIBLE);
            if (btnImport != null) btnImport.setVisibility(View.VISIBLE);
            if (btnAccounts != null) btnAccounts.setVisibility(View.VISIBLE);
            if (btnTheme != null) btnTheme.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        saveAndRefresh();
    }
}