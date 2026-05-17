package com.example.charadesk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class NoteData implements Serializable{
    private String title;
    private List<BlockData> blocks;
    private String avatarPath;

    public NoteData(String title) {
        this.title = title;
        this.blocks = new ArrayList<>();
        this.avatarPath = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public List<BlockData> getBlocks() {
        return blocks;
    }

    public void addBlock(BlockData block) {
        blocks.add(block);
    }

    // Вспомогательный класс для хранения одного блока
    public static class BlockData implements Serializable {
        private String type;
        private String title;
        private String text;
        private String imagePath;

        public BlockData(String type, String title, String text) {
            this.type = type;
            this.title = title;
            this.text = text;
            this.imagePath = null;
        }
        public BlockData(String type, String imagePath) {
            this.type = type;
            this.imagePath = imagePath;
            this.title = null;
            this.text = null;
        }

        public String getType() {
            return type;
        }
        public String getText() {
            return text;
        }
        public void setText(String text) {
            this.text = text;
        }
        public String getTitle() {return title;}
        public void setTitle(String title) {
            this.title = title;
        }
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    }
}
