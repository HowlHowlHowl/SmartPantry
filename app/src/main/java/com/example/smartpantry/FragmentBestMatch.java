package com.example.smartpantry;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class FragmentBestMatch extends Fragment {
    private RecyclerView rv;
    private String toMatch;
    public List<AdapterBestMatch.Match> list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_best_matches, container, false);
        view.findViewById(R.id.confirmChoice).setOnClickListener(v->{
        JSONArray voteList = new JSONArray();
            for(int i=0; i<rv.getAdapter().getItemCount(); i++) {
                boolean isCorrect = !((AdapterBestMatch.BestMatchViewHolder) Objects.requireNonNull(rv.findViewHolderForAdapterPosition(i))).vote.isChecked();
                String id = list.get(i).id;
                String name = list.get(i).name;
                JSONObject voteObject = new JSONObject();
                try {
                    voteObject.put("id", id);
                    voteObject.put("name", name);
                    voteObject.put("isCorrect", isCorrect);
                    voteList.put(voteObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            JSONObject vote = new JSONObject();
            try {
                vote.put("toMatch", toMatch);
                vote.put("voteList", voteList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ((ActivityMain)getActivity()).sendMatchVote(vote);
            closeFragment();
        });
        list = getArguments().getParcelableArrayList("list");
        toMatch = getArguments().getString("toMatch");
        Log.println(Log.ASSERT, "to match", toMatch);
        rv = view.findViewById(R.id.matchedList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setItemAnimator(null);
        rv.setAdapter(new AdapterBestMatch(list));
        return view;
    }
    public void closeFragment() {
        FragmentManager fm = getActivity()
                .getSupportFragmentManager();
        fm.beginTransaction().remove(FragmentBestMatch.this).commit();
        fm.popBackStack(Global.FRAG_BEST_MATCH, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
