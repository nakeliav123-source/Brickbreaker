package com.example.brickbreaker;


import android.content.ContentValues ;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

    public class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "BrickBreaker.db";
        private static final int DATABASE_VERSION = 1;

        private static final String TABLE_NAME = "scores";
        private static final String COL_ID = "id";
        private static final String COL_NAME = "username";
        private static final String COL_SCORE = "score";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_NAME + " TEXT, " +
                    COL_SCORE + " INTEGER)";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        // פונקציה לשמירת שיא חדש
        public void saveScore(String name, int score) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_SCORE, score);
            db.insert(TABLE_NAME, null, values);
            db.close();
        }

        // פונקציה לשליפת 10 התוצאות הכי טובות
        public List<String> getTop10Scores() {
            List<String> topScores = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();

            // שאילתה שממיינת מהגבוה לנמוך ומגבילה ל-10 תוצאות
            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_SCORE + " DESC LIMIT 10";
            Cursor cursor = db.rawQuery(query, null);

            int rank = 1;
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                    int score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCORE));
                    topScores.add(rank + ". " + name + " - " + score);
                    rank++;
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
            return topScores;
        }
}
