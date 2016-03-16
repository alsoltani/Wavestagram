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

    // All Static variables.
    // Database Version.
    private static final int DATABASE_VERSION = 1;

    // Database Name.
    private static final String DATABASE_NAME = "pictureDatabase";

    // Contacts table name.
    private static final String TABLE_PICTURES = "pictures";

    // Contacts Table Columns names.
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_FILENAME = "fileName";

    private static final String TAG = "Database Handler";

    public class Picture {
        public String name;
        public String fileName;
    }

    private static DatabaseHandler sInstance;

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

    /*
    // Insert a picture into the database.
    public void addPicture(Picture picture) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            // The user might already exist in the database (i.e. the same user created multiple posts).
           // long userId = addOrUpdateUser(picture.user);

            ContentValues values = new ContentValues();
            values.put(KEY_NAME, picture.name);
            values.put(KEY_FILENAME, picture.fileName);

            // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
            db.insertOrThrow(TABLE_PICTURES, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
    }
    */

    // Insert or update a picture in the database.
    public long addOrUpdateFile(String name, String fileName) {

        /* Since SQLite doesn't support "upsert" we need to fall back on an attempt to UPDATE (in case the
        user already exists) optionally followed by an INSERT (in case the user does not already exist).
        Unfortunately, there is a bug with the insertOnConflict method
        (https://code.google.com/p/android/issues/detail?id=13045) so we need to fall back to the more
        verbose option of querying for the user's primary key if we did an update.
         */

        // The database connection is cached so it's not expensive to call getWriteableDatabase() multiple times.
        SQLiteDatabase db = getWritableDatabase();
        long pictureId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, name);
            values.put(KEY_FILENAME, fileName);

            // First try to update the picture in picture the user already exists in the database
            // This assumes fileNames are unique.
            int rows = db.update(TABLE_PICTURES, values, KEY_FILENAME + "= ?", new String[]{fileName});

            // Check if update succeeded
            if (rows == 1) {
                // Get the primary key of the user we just updated
                String usersSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        KEY_ID, TABLE_PICTURES, KEY_FILENAME);
                Cursor cursor = db.rawQuery(usersSelectQuery, new String[]{String.valueOf(fileName)});
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
            Log.d(TAG, "Error while trying to add or update user");
        } finally {
            db.endTransaction();
        }
        return pictureId;
    }
}