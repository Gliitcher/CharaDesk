package com.example.charadesk;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class Universe extends AppCompatActivity {
    /*Ключи доступа для обработчика явлений.*/
    private static final int REQUEST_EXPORT_PROFILE = 200;
    private static final int REQUEST_IMPORT_PROFILE = 201;
    /*Ссылки на объекты*/
    private LinearLayout profilesContainer;
    private String currentProfile;
    private List<String> profileNames;
    private View currentOpenPanel = null;
    private String pendingExportProfileName;
    /*Метод создания приложения, реализует подготовку явления*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainMenu.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universe);

        profilesContainer = findViewById(R.id.linear);
        currentProfile = getIntent().getStringExtra("current_profile");
        loadProfilesList();
        refreshProfiles();
    }
    /*Метод, загружающий список доступных профилей из внутреннего хранилища*/
    private void loadProfilesList() {
        profileNames = new ArrayList<>();
        File[] files = getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("notes_") && name.endsWith(".json")) {
                    String profile = name.substring(6, name.length() - 5);
                    profileNames.add(profile);
                }
            }
        }
        /*При отсутствии профилей создаёт базовый notes*/
        if (profileNames.isEmpty()) {
            profileNames.add("notes");
            MainMenu.saveNotesToFile(this, "notes");
        }
    }
    /*Метод, обновляющий список профилей*/
    private void refreshProfiles() {
        profilesContainer.removeAllViews();
        currentOpenPanel = null;
        for (String profile : profileNames) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.universe_item, profilesContainer, false);
            TextView nameView = itemView.findViewById(R.id.profile_name);
            nameView.setText(profile);
            LinearLayout buttonPanel = itemView.findViewById(R.id.button_panel);
            itemView.setOnLongClickListener(v -> {
                if (currentOpenPanel != null && currentOpenPanel != buttonPanel) {
                    currentOpenPanel.setVisibility(View.GONE);
                }
                if (buttonPanel.getVisibility() == View.GONE) {
                    buttonPanel.setVisibility(View.VISIBLE);
                    currentOpenPanel = buttonPanel;
                } else {
                    buttonPanel.setVisibility(View.GONE);
                    currentOpenPanel = null;
                }
                return true;
            });
            View mainContainer = itemView.findViewById(R.id.profile_name).getParent() instanceof LinearLayout ?
                    (View) itemView.findViewById(R.id.profile_name).getParent() : itemView;
            mainContainer.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra(MainMenu.EXTRA_PROFILE_NAME, profile);
                setResult(RESULT_OK, result);
                finish();
            });
            profilesContainer.addView(itemView);
        }
    }
    /*Метод для создания нового профиля.
    Вызывает диалог, предлагающий ввести имя профиля.*/
    public void onCreateNewProfile(View view) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Имя профиля");
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Новый профиль")
                .setView(input)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String newProfile = input.getText().toString().trim();
                    if (newProfile.isEmpty()) {
                        Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (profileNames.contains(newProfile)) {
                        Toast.makeText(this, "Профиль с таким именем уже существует", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<NoteData> oldNotes = new ArrayList<>(MainMenu.notesList);
                    MainMenu.notesList.clear();
                    MainMenu.saveNotesToFile(this, newProfile);
                    MainMenu.notesList.clear();
                    MainMenu.notesList.addAll(oldNotes);
                    profileNames.add(newProfile);
                    refreshProfiles();
                    Toast.makeText(this, "Профиль создан", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    /*Обработчик вызова меню у профиля*/
    public void onClickMenu(View view) {
        LinearLayout buttonPanel = null;
        View current = view;
        while (current != null) {
            buttonPanel = current.findViewById(R.id.button_panel);
            if (buttonPanel != null) break;
            current = (View) current.getParent();
        }
        if (buttonPanel == null) return;
        if (currentOpenPanel != null && currentOpenPanel != buttonPanel) {
            currentOpenPanel.setVisibility(View.GONE);
        }
        if (buttonPanel.getVisibility() == View.GONE) {
            buttonPanel.setVisibility(View.VISIBLE);
            currentOpenPanel = buttonPanel;
        } else {
            buttonPanel.setVisibility(View.GONE);
            currentOpenPanel = null;
        }
    }
    /*Метод, возвращающий ссылку на профиль, для дальнейшей обработки*/
    private View getProfileRoot(View button) {
        View current = button;
        while (current.getParent() != null && current.getParent() != profilesContainer) {
            current = (View) current.getParent();
        }
        return current;
    }
    /*Обработчик изменяющий положение профиля, перенося его вверх*/
    public void onClickUp(View view) {
        View profileItem = getProfileRoot(view);
        int position = profilesContainer.indexOfChild(profileItem);
        if (position > 0) {
            String profile = profileNames.remove(position);
            profileNames.add(position - 1, profile);
            refreshProfiles();
        }
    }
    /*Обработчик вниз*/
    public void onClickDown(View view) {
        View profileItem = getProfileRoot(view);
        int position = profilesContainer.indexOfChild(profileItem);
        if (position < profileNames.size() - 1) {
            String profile = profileNames.remove(position);
            profileNames.add(position + 1, profile);
            refreshProfiles();
        }
    }
    /*Обработчик, удаляющий профиль. Если это не активный профиль!!!*/
    public void onClickDelete(View view) {
        View profileItem = getProfileRoot(view);
        int position = profilesContainer.indexOfChild(profileItem);
        String profile = profileNames.get(position);
        if (profile.equals(currentProfile)) {
            Toast.makeText(this, "Нельзя удалить активный профиль", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Удалить профиль")
                .setMessage("Удалить профиль \"" + profile + "\"?")
                .setPositiveButton("Да", (dialog, which) -> {
                    String fileName = "notes_" + profile + ".json";
                    new File(getFilesDir(), fileName).delete();
                    profileNames.remove(position);
                    refreshProfiles();
                    Toast.makeText(this, "Профиль удалён", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }
    /*Обработчик экспорта профиля*/
    public void onClickExport(View view) {
        View profileItem = getProfileRoot(view);
        int position = profilesContainer.indexOfChild(profileItem);
        if (position == -1) return;

        String profileName = profileNames.get(position);
        String fileName = "notes_" + profileName + ".json";
        File file = new File(getFilesDir(), fileName);
        if (!file.exists()) {
            Toast.makeText(this, "Файл профиля не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingExportProfileName = profileName;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "profile_" + profileName + ".json");
        startActivityForResult(intent, REQUEST_EXPORT_PROFILE);
    }
    /*Обработчик импорта профиля*/
    public void onClickImport(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_PROFILE);
    }
    /*Обработчик нажатия кнопки "Назад"*/
    public void onClickBack(View view) {
        finish();
    }
    /*Метод, обрабатывающий результаты других явлений*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        /*Ключ экспорта, экспортирует выбранный файл*/
        if (requestCode == REQUEST_EXPORT_PROFILE && pendingExportProfileName != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri);
                     FileInputStream in = new FileInputStream(new File(getFilesDir(), "notes_" + pendingExportProfileName + ".json"))) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                    Toast.makeText(this, "Профиль экспортирован", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка экспорта", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            pendingExportProfileName = null;
        }
        /*Ключ импорта. Импортирует выбранный файл*/
        else if (requestCode == REQUEST_IMPORT_PROFILE) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    String json = readAllText(in);
                    String originalName = getFileNameFromUri(uri);
                    importProfileFromJson(json, originalName);
                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка импорта", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    /*Получает оригинальное имя файла*/
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {}
        }
        if (fileName == null) fileName = uri.getLastPathSegment();
        if (fileName == null) fileName = "imported";
        if (fileName.endsWith(".json")) fileName = fileName.substring(0, fileName.length() - 5);
        return fileName;
    }
    /*Вспомогательный метод для чтения всего текста из файла*/
    private String readAllText(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
    /*Метод импорта заметки*/
    private void importProfileFromJson(String json, String originalName) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<NoteData>>(){}.getType();
        List<NoteData> notes;
        try {
            notes = gson.fromJson(json, type);
            if (notes == null) throw new Exception();
        } catch (Exception e) {
            Toast.makeText(this, "Файл не содержит массива заметок", Toast.LENGTH_SHORT).show();
            return;
        }
        /*Создание имени профиля*/
        String baseName = originalName.replace("profile", "");
        if (baseName.equals("")) baseName = "imported";
        String profileName = baseName;
        /*Добавление числового суффика, если профиль импортирован несколько раз*/
        int counter = 1;
        while (profileNames.contains(profileName)) {
            profileName = baseName + "_" + (counter++);
        }
        /*Сохранение файла*/
        File newFile = new File(getFilesDir(), "notes_" + profileName + ".json");
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(json);
            writer.flush();
            profileNames.add(profileName);
            refreshProfiles();
            Toast.makeText(this, "Профиль импортирован как '" + profileName + "'", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения профиля", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}