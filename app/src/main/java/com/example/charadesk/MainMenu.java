package com.example.charadesk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private static final int REQUEST_CREATE_NOTE = 1;
    private static final int REQUEST_EXPORT = 100;
    private static final int REQUEST_IMPORT = 101;
    private static final int REQUEST_ACCOUNTS = 300;
    private static final int REQUEST_SAVE_PDF = 400;
    private PdfDocument pendingPdfDocument;
    public static final String EXTRA_PROFILE_NAME = "profile_name";

    private String currentProfile = "notes";
    public static List<NoteData> notesList = new ArrayList<>();
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private View currentOpenPanelNote = null;
    private String pendingExportJson = null;
    public static final String PREFS_NAME = "settings";
    public static final String KEY_ROLE = "user_role";
    public static final String KEY_NOTES = "saved_notes";
    public static final String KEY_PROFILE = "profile";
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
        currentProfile = prefs.getString(KEY_PROFILE, currentProfile);
        password = prefs.getString(KEY_PASSWORD, password);
        currentTheme = prefs.getString(KEY_THEME, "LAVENDER");

        applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateRoleIcon();

        TextView universes = findViewById(R.id.universe);
        universes.setText(currentProfile);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        loadNotesFromFile(currentProfile);
        refreshNotesList();
        updateUIBasedOnRole();
    }

    private void refreshNotesList() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        currentOpenPanelNote = null;
    }

    private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NoteData note = notesList.get(position);
            holder.noteText.setText(note.getTitle());
            if (note.getAvatarPath() != null) {
                File avatarFile = new File(note.getAvatarPath());
                if (avatarFile.exists()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeFile(note.getAvatarPath(), options);
                    holder.avatar.setImageBitmap(bitmap);
                } else {
                    holder.avatar.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                holder.avatar.setImageResource(R.drawable.ic_image_placeholder);
            }
            if (currentRole.equals(ROLE_READER)) {
                holder.menuButton.setVisibility(View.GONE);
            } else {
                holder.menuButton.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MainMenu.this, Note.class);
                intent.putExtra("note_index", position);
                intent.putExtra("profile_name", currentProfile);
                intent.putExtra("role", currentRole);
                startActivity(intent);
            });
            holder.menuButton.setOnClickListener(v -> onClickMenu(holder.menuButton));
        }

        @Override
        public int getItemCount() {
            return notesList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView noteText;
            ImageView avatar;
            ImageButton menuButton;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                noteText = itemView.findViewById(R.id.note_text);
                avatar = itemView.findViewById(R.id.avatar);
                menuButton = itemView.findViewById(R.id.note_menu);
            }
        }
    }

    public void onClickCreateNewNote(View view) {
        Intent intent = new Intent(this, CreateNote.class);
        startActivityForResult(intent, REQUEST_CREATE_NOTE);
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_NOTE && resultCode == RESULT_OK) {
            String title = data.getStringExtra("note_title");
            String avatarPath = data.getStringExtra("avatar_path");
            String blocksJson = data.getStringExtra("blocks_json");

            NoteData newNote = new NoteData(title);
            if (avatarPath != null) newNote.setAvatarPath(avatarPath);

            if (blocksJson != null && !blocksJson.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<NoteData.BlockData>>(){}.getType();
                List<NoteData.BlockData> blocks = gson.fromJson(blocksJson, type);
                if (blocks != null) {
                    newNote.getBlocks().addAll(blocks);
                }
            }

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
                        .putString(KEY_PROFILE, currentProfile).apply();
                loadNotesFromFile(currentProfile);
                refreshNotesList();
                Toast.makeText(this, getString(R.string.universe_changed) + currentProfile, Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_SAVE_PDF && pendingPdfDocument != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    pendingPdfDocument.writeTo(out);
                    Toast.makeText(this, "PDF сохранён", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Ошибка сохранения PDF", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            pendingPdfDocument.close();
            pendingPdfDocument = null;
        }
    }

    public void onClickChangeUniverse(View view) {
        Intent intent = new Intent(this, Universe.class);
        intent.putExtra("current_profile", currentProfile);
        startActivityForResult(intent, REQUEST_ACCOUNTS);
    }

    public void onClickUp(View view) {
        View noteCard = getNoteCardView(view);
        int position = recyclerView.getChildAdapterPosition(noteCard);
        if (position > 0) {
            if (currentOpenPanelNote != null) {
                LinearLayout panel = currentOpenPanelNote.findViewById(R.id.button_panel);
                if (panel != null) panel.setVisibility(View.GONE);
                currentOpenPanelNote = null;
            }
            NoteData note = notesList.remove(position);
            notesList.add(position - 1, note);
            adapter.notifyDataSetChanged();
            saveCurrentProfile();
        }
    }

    public void onClickDown(View view) {
        View noteCard = getNoteCardView(view);
        int position = recyclerView.getChildAdapterPosition(noteCard);
        if (position < notesList.size() - 1) {
            if (currentOpenPanelNote != null) {
                LinearLayout panel = currentOpenPanelNote.findViewById(R.id.button_panel);
                if (panel != null) panel.setVisibility(View.GONE);
                currentOpenPanelNote = null;
            }
            NoteData note = notesList.remove(position);
            notesList.add(position + 1, note);
            adapter.notifyDataSetChanged();
            saveCurrentProfile();
        }
    }

    public void onClickDelete(View view) {
        View noteCard = getNoteCardView(view);
        int position = recyclerView.getChildAdapterPosition(noteCard);
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.delete_note))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    if (currentOpenPanelNote != null) {
                        LinearLayout panel = currentOpenPanelNote.findViewById(R.id.button_panel);
                        if (panel != null) panel.setVisibility(View.GONE);
                        currentOpenPanelNote = null;
                    }
                    notesList.remove(position);
                    adapter.notifyDataSetChanged();
                    saveCurrentProfile();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    public void onClickMenu(View view) {
        View noteCard = getNoteCardView(view);
        LinearLayout buttonPanel = noteCard.findViewById(R.id.button_panel);
        if (buttonPanel == null) return;
        ImageButton buttonPDF = buttonPanel.findViewById(R.id.pdf_button);
        if (currentOpenPanelNote != null && currentOpenPanelNote != noteCard) {
            LinearLayout oldPanel = currentOpenPanelNote.findViewById(R.id.button_panel);
            if (oldPanel != null) oldPanel.setVisibility(View.GONE);
        }
        if (buttonPanel.getVisibility() == View.GONE && !currentRole.equals(ROLE_READER)) {
            buttonPanel.setVisibility(View.VISIBLE);
            buttonPDF.setVisibility(View.VISIBLE);
            currentOpenPanelNote = noteCard;
        } else {
            buttonPanel.setVisibility(View.GONE);
            currentOpenPanelNote = null;
        }
    }

    public void onClickExport(View view) {
        View noteCard = getNoteCardView(view);
        int position = recyclerView.getChildAdapterPosition(noteCard);
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

    public void onClickPDF(View view) {
        View noteCard = getNoteCardView(view);
        int position = recyclerView.getChildAdapterPosition(noteCard);
        if (position == -1) return;
        NoteData note = notesList.get(position);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        float x = 50;
        float y = 50;
        float pageWidth = pageInfo.getPageWidth();
        float usableWidth = pageWidth - 2 * x;
        float lineHeight = 20;
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);

        Bitmap avatarBitmap = null;
        if (note.getAvatarPath() != null) {
            File avatarFile = new File(note.getAvatarPath());
            if (avatarFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                avatarBitmap = BitmapFactory.decodeFile(note.getAvatarPath(), options);
            }
        }
        if (avatarBitmap != null) {
            float imageWidth = 100;
            float imageHeight = 100 * (float) avatarBitmap.getHeight() / avatarBitmap.getWidth();
            canvas.drawBitmap(avatarBitmap, null, new RectF(x, y, x + imageWidth, y + imageHeight), null);
            y += imageHeight + 20;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(24);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(note.getTitle(), x, y, paint);
        y += lineHeight * 2;
        paint.setTextSize(14);
        paint.setTypeface(Typeface.DEFAULT);

        for (NoteData.BlockData block : note.getBlocks()) {
            String blockTitle = block.getTitle();
            String blockText = block.getText();
            if (blockTitle != null && !blockTitle.isEmpty()) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(18);
                canvas.drawText(blockTitle, x, y, paint);
                y += lineHeight;
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(14);
            }
            if ("image".equals(block.getType()) && block.getImagePath() != null) {
                File imgFile = new File(block.getImagePath());
                if (imgFile.exists()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap img = BitmapFactory.decodeFile(block.getImagePath(), options);
                    if (img != null) {
                        float maxImageWidth = usableWidth;
                        float maxImageHeight = 200;
                        float imgWidth = maxImageWidth;
                        float imgHeight = imgWidth * (float) img.getHeight() / img.getWidth();
                        if (imgHeight > maxImageHeight) {
                            imgHeight = maxImageHeight;
                            imgWidth = imgHeight * (float) img.getWidth() / img.getHeight();
                        }
                        if (y + imgHeight > pageInfo.getPageHeight() - 50) {
                            pdfDocument.finishPage(page);
                            page = pdfDocument.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = 50;
                        }
                        canvas.drawBitmap(img, null, new RectF(x, y, x + imgWidth, y + imgHeight), null);
                        y += imgHeight + 20;
                        continue;
                    }
                }
            }
            if (blockText != null && !blockText.isEmpty()) {
                List<String> lines = splitText(blockText, paint, usableWidth);
                for (String line : lines) {
                    canvas.drawText(line, x, y, paint);
                    y += lineHeight;
                    if (y > pageInfo.getPageHeight() - 50) {
                        pdfDocument.finishPage(page);
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 50;
                    }
                }
            }
            y += lineHeight;
            if (y > pageInfo.getPageHeight() - 50) {
                pdfDocument.finishPage(page);
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
        }

        pdfDocument.finishPage(page);

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "note_" + note.getTitle() + ".pdf");
        startActivityForResult(Intent.createChooser(intent, "Сохранить PDF"), REQUEST_SAVE_PDF);
        pendingPdfDocument = pdfDocument;
    }

    private List<String> splitText(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = paint.measureText(testLine);
            if (width <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? word : " " + word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

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

    private View getNoteCardView(View button) {
        View parent = (View) button.getParent();
        while (!(parent instanceof androidx.cardview.widget.CardView)) {
            parent = (View) parent.getParent();
        }
        return parent;
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
                .putString(KEY_PROFILE, currentProfile)
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
    public void onStop(){
        super.onStop();
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString(KEY_PROFILE, currentProfile).apply();
    }

    @Override
    public void onResume(){
        super.onResume();
        saveAndRefresh();
    }
}