package io.github.alsoltani.wavestagram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;
import io.github.alsoltani.wavestagram.R;
import io.github.alsoltani.wavestagram.ui.adapter.FeedItemAnimator;
import io.github.alsoltani.wavestagram.ui.utils.Utils;
import io.github.alsoltani.wavestagram.ui.adapter.FeedAdapter;
import io.github.alsoltani.wavestagram.ui.view.FeedContextMenu;
import io.github.alsoltani.wavestagram.ui.view.FeedContextMenuManager;
import io.github.alsoltani.wavestagram.database.DatabaseHandler;

public class MainActivity extends BaseActivity implements FeedAdapter.OnFeedItemClickListener,
        FeedContextMenu.OnFeedContextMenuItemClickListener {

    public static final String galleryPath = System.getenv("SECONDARY_STORAGE")
            + "/" + Environment.DIRECTORY_PICTURES +
            "/Wavestagram/";

    private static final int ANIM_DURATION_TOOLBAR = 300;
    private static final int ANIM_DURATION_FAB = 400;

    @Bind(R.id.rvFeed)
    RecyclerView rvFeed;
    @Bind(R.id.btnCreate)
    FloatingActionButton fabCreate;
    @Bind(R.id.content)
    CoordinatorLayout clContent;

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFeed();

        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        } else {
            feedAdapter.updateItems(false);
        }
    }

    private void setupFeed() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);

        DatabaseHandler handler = DatabaseHandler.getInstance(this);
        SQLiteDatabase db = handler.getWritableDatabase();

        handler.addFileOrPass("File 1", "Picture1.png");
        handler.addFileOrPass("File 2", "Picture2.png");
        handler.addFileOrPass("File 3", "Picture3.png");
        handler.addFileOrPass("File 4", "Picture4.png");

        Cursor pictureCursor = db.rawQuery("SELECT * FROM pictureTable ORDER BY _id DESC", null);

        feedAdapter = new FeedAdapter(this, pictureCursor);
        feedAdapter.setOnFeedItemClickListener(this);
        rvFeed.setAdapter(feedAdapter);
        rvFeed.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                FeedContextMenuManager.getInstance().onScrolled(recyclerView, dx, dy);
            }
        });
        rvFeed.setItemAnimator(new FeedItemAnimator());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (pendingIntroAnimation) {
            pendingIntroAnimation = false;
            startIntroAnimation();
        }
        return true;
    }

    private void startIntroAnimation() {
        fabCreate.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));

        int actionbarSize = Utils.dpToPx(56);
        getToolbar().setTranslationY(-actionbarSize);
        getIvLogo().setTranslationY(-actionbarSize);
        getInboxMenuItem().getActionView().setTranslationY(-actionbarSize);

        getToolbar().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        getIvLogo().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400);
        getInboxMenuItem().getActionView().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }
                })
                .start();
    }

    private void startContentAnimation() {
        fabCreate.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();
        feedAdapter.updateItems(true);
    }

    @Override
    public void onMoreClick(View v, int itemPosition) {
        FeedContextMenuManager.getInstance().toggleContextMenuFromView(v, itemPosition, this);
    }

    @Override
    public void onCancelClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }


    @Override
    public void onDeleteClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
        DatabaseHandler handler = DatabaseHandler.getInstance(this);

        // Remove item from ArrayList.
        feedAdapter.feedItems.remove(feedItem);

        // Delete from database, and from picture folder, by id.
        // Remind that pictures are stored in reverse order.
        int length = handler.getNumberRows();
        String fileNameToDelete = handler.deleteFile(length - feedItem);

        Log.v("feedItem", String.valueOf(feedItem));
        Log.v("Deletion", galleryPath + fileNameToDelete);
        if (!fileNameToDelete.equals("none")){

            new File(galleryPath + fileNameToDelete).delete();

            // Use MediaScanner to refresh the gallery.
            deleteFileFromMediaStore(getContentResolver(), new File(galleryPath + fileNameToDelete));
        }

        //Notify changes to feed.
        feedAdapter.notifyDataSetChanged();
        feedAdapter.notifyItemRemoved(feedItem);
        feedAdapter.notifyItemRangeChanged(feedItem, length);

        setContentView(R.layout.activity_main);
        setupFeed();
        feedAdapter.updateItems(false);

        showDeletedSnackbar();
    }

    public static void deleteFileFromMediaStore(final ContentResolver contentResolver, final File file) {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[] {canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

    @OnClick(R.id.btnCreate)
    public void onTakePhotoClick() {
        int[] startingLocation = new int[2];
        fabCreate.getLocationOnScreen(startingLocation);
        startingLocation[0] += fabCreate.getWidth() / 2;
        TakePhotoActivity.startCameraFromLocation(startingLocation, this);
        overridePendingTransition(0, 0);
    }

    public void showDeletedSnackbar() {
        Snackbar.make(clContent, "Deleted!", Snackbar.LENGTH_SHORT).show();
    }
}