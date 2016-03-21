package io.github.alsoltani.wavestagram.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.github.alsoltani.wavestagram.R;
import io.github.alsoltani.wavestagram.database.DatabaseHandler;
import io.github.alsoltani.wavestagram.ui.activity.MainActivity;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final List<FeedItem> feedItems = new ArrayList<>();

    private Context context;
    private CursorAdapter cursorAdapter;
    private OnFeedItemClickListener onFeedItemClickListener;

    public FeedAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursorAdapter = new CursorAdapter(context, cursor, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

            }
        };
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = cursorAdapter.newView(context, cursorAdapter.getCursor(), parent);
        CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);

        setupClickableViews(view, cellFeedViewHolder, cursorAdapter.getCursor());
        return cellFeedViewHolder;
    }

    private void setupClickableViews(final View view, final CellFeedViewHolder cellFeedViewHolder, final Cursor cursor) {

        cellFeedViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onMoreClick(v, cellFeedViewHolder.getAdapterPosition());

                //
            }
        });

        cellFeedViewHolder.ivFeedName.setOnLongClickListener(
                new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        // TODO Auto-generated method stub

                        // get prompts.xml view
                        View promptsView = LayoutInflater.from(context).inflate(R.layout.prompts, null);

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                        alertDialogBuilder.setView(promptsView);
                        // set prompts.xml to alertdialog builder

                        final EditText userInput = (EditText) promptsView
                                .findViewById(R.id.editTextDialogUserInput);

                        // set dialog message
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int id) {

                                                //Get user input, and set it to result.

                                                CharSequence userInputText = userInput.getText();
                                                cellFeedViewHolder.ivFeedName.setText(userInputText);

                                                // Update name in database.

                                                //Log.v("AddOrUpdateFile", String.valueOf(cellFeedViewHolder.getAdapterPosition()));

                                                DatabaseHandler handler = DatabaseHandler.getInstance(context);

                                                handler.addOrUpdateFile(
                                                        userInputText.toString(),
                                                        cellFeedViewHolder.ivFeedFileName.getText().toString());
                                            }
                                        })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int id) {
                                                dialog.cancel();
                                            }
                                        });

                        // create alert dialog
                        final AlertDialog alertDialog = alertDialogBuilder.create();

                        // Setup to change color of the button.
                        alertDialog.setOnShowListener(
                                new DialogInterface.OnShowListener() {

                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                                .setTextColor(ContextCompat.getColor(context, R.color.btn_alert_dialog));
                                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setTextColor(ContextCompat.getColor(context, R.color.btn_alert_dialog));
                                    }
                                });

                        // show it
                        alertDialog.show();

                        return false;
                    }
                });

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        cursorAdapter.getCursor().moveToPosition(position);
        ((CellFeedViewHolder) viewHolder).bindView(feedItems.get(position), cursorAdapter.getCursor());

    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void updateItems(boolean animated) {

        DatabaseHandler handler = DatabaseHandler.getInstance(context);
        int length = handler.getNumberRows();

        List<FeedItem> updateList = new ArrayList<>(
                Collections.nCopies(length, new FeedItem("Image 1", "fileName 1")));

        feedItems.clear();
        feedItems.addAll(updateList);

        //Make sure elements show up at first.
        if (animated) {
            notifyItemRangeInserted(0, feedItems.size());
        } else {
            notifyDataSetChanged();
        }
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.ivFeedCenter)
        ImageView ivFeedCenter;
        @Bind(R.id.ivFeedName)
        TextView ivFeedName;
        @Bind(R.id.ivFeedFileName)
        TextView ivFeedFileName;
        @Bind(R.id.btnMore)
        ImageButton btnMore;

        FeedItem feedItem;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bindView(FeedItem feedItem, Cursor cursor) {

            this.feedItem = feedItem;

            //Log.v("Decode", cursor.getString(cursor.getColumnIndex("fileName")));
            Bitmap bmImage = BitmapFactory.decodeFile(
                    MainActivity.galleryPath + cursor.getString(cursor.getColumnIndex("fileName")));
            ivFeedCenter.setImageBitmap(bmImage);
            ivFeedName.setText(cursor.getString(cursor.getColumnIndex("name")));
            ivFeedFileName.setText(cursor.getString(cursor.getColumnIndex("fileName")));
        }

    }

    public static class FeedItem {

        public String name;
        public String fileName;

        public FeedItem(String name, String fileName) {
            this.name = name;
            this.fileName = fileName;
        }
    }


    public interface OnFeedItemClickListener {

        void onMoreClick(View v, int position);

    }
}