package com.receparslan.basicsocialmedia.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.receparslan.basicsocialmedia.databinding.RecyclerRowBinding;
import com.receparslan.basicsocialmedia.model.Post;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    final ArrayList<Post> postArrayList;

    public RecyclerAdapter(ArrayList<Post> postArrayList) {
        this.postArrayList = postArrayList;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerRowBinding binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        holder.binding.emailTextView.setText(postArrayList.get(position).getEmail());
        holder.binding.dateTextView.setText(postArrayList.get(position).getDate());
        holder.binding.displayNameTextView.setText(String.format("%s : ", postArrayList.get(position).getDisplayName()));
        holder.binding.commentTextView.setText(holder.binding.displayNameTextView.getText().toString().concat(postArrayList.get(position).getComment()));
        Picasso.get().load(postArrayList.get(position).getImageUri()).into(holder.binding.imageView);
    }

    @Override
    public int getItemCount() {
        return postArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final RecyclerRowBinding binding;

        public ViewHolder(RecyclerRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
