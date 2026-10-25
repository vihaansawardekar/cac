package com.example.soilmetrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "soil.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // make the tables
        db.execSQL("CREATE TABLE fields (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, acres REAL, lat REAL, lon REAL, state TEXT, tillage TEXT, cover INTEGER)");
        db.execSQL("CREATE TABLE samples (id INTEGER PRIMARY KEY AUTOINCREMENT, field_id INTEGER, date INTEGER, om REAL, note TEXT)");
        db.execSQL("CREATE TABLE gait_checks (id INTEGER PRIMARY KEY AUTOINCREMENT, animal TEXT, date INTEGER, score REAL, note TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // keep old fields safe
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE gait_checks (id INTEGER PRIMARY KEY AUTOINCREMENT, animal TEXT, date INTEGER, score REAL, note TEXT)");
        }
    }

    // save a field
    public int saveField(Field f) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", f.name);
        values.put("acres", f.acres);
        values.put("lat", f.lat);
        values.put("lon", f.lon);
        values.put("state", f.state);
        values.put("tillage", f.tillage);
        if (f.coverCrops) {
            values.put("cover", 1);
        } else {
            values.put("cover", 0);
        }

        if (f.id > 0) {
            db.update("fields", values, "id = " + f.id, null);
            return f.id;
        } else {
            long newId = db.insert("fields", null, values);
            return (int) newId;
        }
    }

    // delete field and samples
    public void deleteField(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("fields", "id = " + id, null);
        db.delete("samples", "field_id = " + id, null);
    }

    // get every field
    public ArrayList<Field> getAllFields() {
        ArrayList<Field> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM fields ORDER BY id", null);
        while (c.moveToNext()) {
            list.add(readField(c));
        }
        c.close();
        return list;
    }

    // get one field
    public Field getField(int id) {
        Field f = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM fields WHERE id = " + id, null);
        if (c.moveToFirst()) {
            f = readField(c);
        }
        c.close();
        return f;
    }

    // cursor row to field
    private Field readField(Cursor c) {
        Field f = new Field();
        f.id = c.getInt(0);
        f.name = c.getString(1);
        f.acres = c.getDouble(2);
        f.lat = c.getDouble(3);
        f.lon = c.getDouble(4);
        f.state = c.getString(5);
        f.tillage = c.getString(6);
        f.coverCrops = c.getInt(7) == 1;
        return f;
    }

    // save a soil test
    public void addSample(int fieldId, long date, double om, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("field_id", fieldId);
        values.put("date", date);
        values.put("om", om);
        values.put("note", note);
        db.insert("samples", null, values);
    }

    // delete one test
    public void deleteSample(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("samples", "id = " + id, null);
    }

    // tests oldest first
    public ArrayList<Sample> getSamples(int fieldId) {
        ArrayList<Sample> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM samples WHERE field_id = " + fieldId + " ORDER BY date", null);
        while (c.moveToNext()) {
            Sample s = new Sample();
            s.id = c.getInt(0);
            s.fieldId = c.getInt(1);
            s.date = c.getLong(2);
            s.om = c.getDouble(3);
            s.note = c.getString(4);
            list.add(s);
        }
        c.close();
        return list;
    }

    // save a cow check
    public void addGaitCheck(String animal, long date, double score, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("animal", animal);
        values.put("date", date);
        values.put("score", score);
        values.put("note", note);
        db.insert("gait_checks", null, values);
    }

    // cow checks newest first
    public ArrayList<GaitCheck> getGaitChecks() {
        ArrayList<GaitCheck> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM gait_checks ORDER BY date DESC LIMIT 20", null);
        while (c.moveToNext()) {
            GaitCheck g = new GaitCheck();
            g.id = c.getInt(0);
            g.animal = c.getString(1);
            g.date = c.getLong(2);
            g.score = c.getDouble(3);
            g.note = c.getString(4);
            list.add(g);
        }
        c.close();
        return list;
    }
}
