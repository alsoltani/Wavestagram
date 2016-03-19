package io.github.alsoltani.wavestagram.database;

/**
 * Created by alain on 15/03/16.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler sInstance;

    // All Static variables.
    // Database Version.
    private static final int DATABASE_VERSION = 1;

    // Database Name.
    private static final String DATABASE_NAME = "pictureDatabase";

    // Contacts table name.
    private static final String TABLE_PICTURES = "pictureTable";

    // Contacts Table Columns names.
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_FILENAME = "fileName";

    private static final String TAG = "Database Handler";

    public class Picture {
        public String name;
        public String fileName;
    }

    public static synchronized DatabaseHandler getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.

        if (sInstance == null) {
            sInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHandler(Context context) {

        //Constructor should be private to prevent direct instantiation.
        //Make a call to the static method "getInstance()" instead.

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Creating Tables.
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_PICTURES_TABLE =
                "CREATE TABLE " + TABLE_PICTURES +
                        "(" +
                        KEY_ID + " INTEGER PRIMARY KEY," +
                        KEY_NAME + " TEXT," +
                        KEY_FILENAME + " TEXT" +
                        ")";
        db.execSQL(CREATE_PICTURES_TABLE);
    }

    // Upgrading database.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {

            // Drop older table if existed.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PICTURES);

            // Create tables again.
            onCreate(db);
        }
    }

    // Insert or update a picture in the database.
    public long addOrUpdateFile(String name, String fileName) {

        /* Since SQLite doesn't support "upsert" we need to fall back on an attempt to UPDATE (in case the
        picture already exists) optionally followed by an INSERT (in case the picture does not already exist).
        Unfortunately, there is a bug with the insertOnConflict method
        (https://code.google.com/p/android/issues/detail?id=13045) so we need to fall back to the more
        verbose option of querying for the picture's primary key if we did an update.
         */

        // The database connection is cached so it's not expensive to call getWriteableDatabase() multiple times.
        SQLiteDatabase db = getWritableDatabase();
        long pictureId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, name);
            values.put(KEY_FILENAME, fileName);

            // First try to update the picture in picture the picture already exists in the database
            // This assumes fileNames are unique.
            int rows = db.update(TABLE_PICTURES, values, KEY_FILENAME + "= ?", new String[]{fileName});

            // Check if update succeeded
            if (rows == 1) {
                // Get the primary key of the picture we just updated
                String picturesSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        KEY_ID, TABLE_PICTURES, KEY_FILENAME);
                Cursor cursor = db.rawQuery(picturesSelectQuery, new String[]{String.valueOf(fileName)});
                try {
                    if (cursor.moveToFirst()) {
                        pictureId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else {
                // picture with this fileName did not already exist, so insert new picture
                pictureId = db.insertOrThrow(TABLE_PICTURES, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add or update picture in database");
        } finally {
            db.endTransaction();
        }
        return pictureId;
    }

    // Insert a picture in the database if there is no previous occurence of it.
    public void addFileOrPass(String name, String fileName) {

        SQLiteDatabase db = getWritableDatabase();

        int count = -1;
        Cursor cursor = null;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, name);
            values.put(KEY_FILENAME, fileName);
            
            String checkIfFileName = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?",
                    TABLE_PICTURES, KEY_FILENAME);
            cursor = db.rawQuery(checkIfFileName, new String[]{String.valueOf(fileName)});
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            
            if (count <= 0) {
                db.insertOrThrow(TABLE_PICTURES, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to insert picture in database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    //Delete picture from the database, by _id. Returns fileName,
    //so that we can delete file from external storage right after.
    public String deleteFile(int id) {

        SQLiteDatabase db = getWritableDatabase();
        String fileName = "none";
        Cursor cursor = null;

        db.beginTransaction();
        try {
            String getFileName = String.format("SELECT %s FROM %s WHERE %s = ?",
                    KEY_FILENAME, TABLE_PICTURES, KEY_ID);
            cursor = db.rawQuery(getFileName, new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                fileName = cursor.getString(0);
                db.setTransactionSuccessful();

                String whereClause = "_id" + "=?";
                String[] whereArgs = new String[]{String.valueOf(id)};
                db.delete(TABLE_PICTURES, whereClause, whereArgs);
            }

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.endTransaction();
        }

        return fileName;
    }

    //Get number of rows in database.
    public int getNumberRows() {

        SQLiteDatabase db = getWritableDatabase();

        int rows = -1;
        Cursor cursor = null;

        db.beginTransaction();
        try {
            String nRowsQuery = String.format("SELECT COUNT(*) FROM %s", TABLE_PICTURES);
            cursor = db.rawQuery(nRowsQuery, null);
            if (cursor.moveToFirst()) {
                rows = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to count the number of rows");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.endTransaction();
        }
        return rows;
    }
}