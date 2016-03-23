package io.github.alsoltani.wavestagram.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.android.ConvertBitmap;
import boofcv.android.ImplConvertBitmap;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import butterknife.Bind;
import butterknife.ButterKnife;
import io.github.alsoltani.wavestagram.R;
import io.github.alsoltani.wavestagram.ui.activity.TakePhotoActivity;

public class PhotoFiltersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int itemsCount = 12;
    public boolean isDenoised = false;
    public static final int IS_DENOISED_REQUEST_CODE = 1;

    public PhotoFiltersAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_photo_filter, parent, false);
        PhotoFilterViewHolder photoFilterViewHolder = new PhotoFilterViewHolder(view);
        setupClickableFilters(view, photoFilterViewHolder);

        return photoFilterViewHolder;
    }

    private void setupClickableFilters(final View view, final PhotoFilterViewHolder photoFilterViewHolder) {

        photoFilterViewHolder.ivPhotoFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v("PhotoFilterClickList", String.valueOf(isDenoised));

                int[] startingLocation = new int[2];
                view.getLocationOnScreen(startingLocation);
                startingLocation[0] += view.getWidth() / 2;

                Intent intent =  new Intent(context, TakePhotoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(TakePhotoActivity.ARG_REVEAL_START_LOCATION, startingLocation);
                intent.putExtra("isDenoised", isDenoised? "true": "false");
                ((Activity) context).startActivityForResult(intent, IS_DENOISED_REQUEST_CODE);
                isDenoised ^= true;
            }
        });
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

    }

    @Override
    public int getItemCount() {
        return itemsCount;
    }

    public static class PhotoFilterViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivPhotoFilter)
        ImageView ivPhotoFilter;

        public PhotoFilterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bindView() {
            //ivPhotoFilter.setImageResource();
        }
    }
}
