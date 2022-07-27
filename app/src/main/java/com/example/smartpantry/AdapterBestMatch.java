package com.example.smartpantry;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterBestMatch extends RecyclerView.Adapter<AdapterBestMatch.BestMatchViewHolder> {
    public List<Match> bestMatchList;
    public static class Match implements Parcelable {
        public String id;
        public String name;

        public Match(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Match(Parcel parcel) {
            id = parcel.readString();
            name = parcel.readString();
        }

        public static Creator<Match> CREATOR = new Creator<Match>() {
            @Override
            public Match createFromParcel(Parcel source) {
                return new Match(source);
            }

            @Override
            public Match[] newArray(int size) {
                return new Match[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(id);
        }
    }

    public AdapterBestMatch(List<AdapterBestMatch.Match> bestMatches) {
        this.bestMatchList = bestMatches;
    }

    @NonNull
    @Override
    public AdapterBestMatch.BestMatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_best_match, parent, false);
        return new AdapterBestMatch.BestMatchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterBestMatch.BestMatchViewHolder holder, int position) {
        holder.name.setText(bestMatchList.get(position).name);
    }

    @Override
    public int getItemCount() {
        return bestMatchList.size();
    }

    public static class BestMatchViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        ToggleButton vote;
         BestMatchViewHolder(@NonNull View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.bestMatchCard);
            name = itemView.findViewById(R.id.bestMatchName);
            vote = itemView.findViewById(R.id.bestMatchVote);
        }
    }
}
