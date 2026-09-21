package com.vernacular.learning.data.models;

public class FlashcardItem {
    public int imageResId;
    public String hindiWord;
    public String transliteration;
    public String englishWord;
    public String santhaliWord;
    public String mundariWord;
    public String hoWord;

    public FlashcardItem(int imageResId, String hindiWord, String transliteration,
                         String englishWord, String santhaliWord, String mundariWord, String hoWord) {
        this.imageResId = imageResId;
        this.hindiWord = hindiWord;
        this.transliteration = transliteration;
        this.englishWord = englishWord;
        this.santhaliWord = santhaliWord;
        this.mundariWord = mundariWord;
        this.hoWord = hoWord;
    }
}
