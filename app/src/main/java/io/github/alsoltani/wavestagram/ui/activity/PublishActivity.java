package io.github.alsoltani.wavestagram.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import io.github.alsoltani.wavestagram.R;
import io.github.alsoltani.wavestagram.ui.utils.Utils;
import io.github.alsoltani.wavestagram.database.DatabaseHandler;

public class PublishActivity extends BaseActivity {
    public static final String ARG_TAKEN_PHOTO_URI = "arg_taken_photo_uri";

    @Bind(R.id.ivPhoto)
    ImageView ivPhoto;
    @Bind(R.id.etDescription)
    EditText etDescription;
    @Bind(R.id.toolbar)
    android.support.v7.widget.Toolbar toolbar;

    private Uri photoUri;
    private int photoSize;
    public Bitmap bmThumbnail;

    public static void openWithPhotoUri(Activity openingActivity, Uri photoUri) {
        Intent intent = new Intent(openingActivity, PublishActivity.class);
        intent.putExtra(ARG_TAKEN_PHOTO_URI, photoUri);
        openingActivity.startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_grey600_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int[] startingLocation = new int[2];
                view.getLocationOnScreen(startingLocation);
                startingLocation[0] += toolbar.getWidth() / 2;

                Intent intent = new Intent(view.getContext(), TakePhotoActivity.class);
                intent.putExtra(TakePhotoActivity.ARG_REVEAL_START_LOCATION, startingLocation);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                overridePendingTransition(0, 0);

                new File(photoUri.toString()).delete();
            }
        });

        photoSize = getResources().getDimensionPixelSize(R.dimen.publish_photo_thumbnail_size);

        if (savedInstanceState == null) {
            photoUri = getIntent().getParcelableExtra(ARG_TAKEN_PHOTO_URI);
        } else {
            photoUri = savedInstanceState.getParcelable(ARG_TAKEN_PHOTO_URI);
        }
        updateStatusBarColor();

        ivPhoto.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ivPhoto.getViewTreeObserver().removeOnPreDrawListener(this);
                loadThumbnailPhoto();
                return true;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateStatusBarColor() {
        if (Utils.isAndroid5()) {
            getWindow().setStatusBarColor(0xff888888);
        }
    }

    public File getNewestFileInDirectory() {
       File newestFile = null;

       // start loop trough files in directory
       File file = new File(MainActivity.galleryPath);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "MM/dd/yyyy hh:mm:ss");
       if (newestFile == null || file.lastModified() > newestFile.lastModified()) {
           newestFile = file;
       }
       // end loop trough files in directory

       return newestFile;
    }

    private void loadThumbnailPhoto() {

        //File newestFile = getNewestFileInDirectory();
        //Uri newestUri = Uri.fromFile(newestFile);

        Log.v("LoadThumbnailPhoto", photoUri.toString());
        ivPhoto.setScaleX(0);
        ivPhoto.setScaleY(0);
        Picasso.with(this)
                .load(photoUri)
                .centerCrop()
                .resize(photoSize, photoSize)
                .into(ivPhoto, new Callback() {
                    @Override
                    public void onSuccess() {
                        ivPhoto.animate()
                                .scaleX(1.f).scaleY(1.f)
                                .setInterpolator(new OvershootInterpolator())
                                .setDuration(400)
                                .setStartDelay(200)
                                .start();
                    }

                    @Override
                    public void onError() {
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_publish, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_publish) {

            DatabaseHandler handler = DatabaseHandler.getInstance(this);
            handler.addOrUpdateFile(
                    etDescription.getText().toString(),
                    new File(photoUri.getPath()).getName());
            bringMainActivityToTop();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void bringMainActivityToTop() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_TAKEN_PHOTO_URI, photoUri);
    }
}
