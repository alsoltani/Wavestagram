package io.github.alsoltani.wavestagram.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
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
    public boolean denoise = false;

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

                    //TODO: Implement the following :
                    // Setup intent and any boolean here.
                    // Switch on boolean in TakePhotoActivity.
                    // Question : how to handle startActivity on start on STATE_SETUP_PHOTO,
                    // with the correct photo shown ?

                    /*Intent intent =  new Intent(context, TakePhotoActivity.class);
                    intent.putExtra("DENOISE", denoise);
                    context.startActivity(intent);
                    */
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
