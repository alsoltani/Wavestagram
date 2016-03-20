package io.github.alsoltani.wavestagram.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.github.alsoltani.wavestagram.R;

public class PhotoFiltersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int itemsCount = 12;

    public PhotoFiltersAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_photo_filter, parent, false);
        PhotoFilterViewHolder photoFilterViewHolder = new PhotoFilterViewHolder(view);

        return photoFilterViewHolder;
    }

    private void setupClickableFilters(final View view, final PhotoFilterViewHolder photoFilterViewHolder) {

        photoFilterViewHolder.ivPhotoFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                //

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
