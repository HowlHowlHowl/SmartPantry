
package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListProductsFragment extends Fragment {
    TextView title;
    TextView warning;
    FloatingActionButton addProduct;
    String barcode;
    private RecyclerView rv;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products_found_list, container, false);
        rv = view.findViewById(R.id.productsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(view.getContext()));
        title = view.findViewById(R.id.productsResultTitle);
        warning = view.findViewById(R.id.noProductsFound);
        addProduct  = view.findViewById(R.id.addProductFloatingBtn);
        barcode = this.getArguments().getString("barcode");
        Log.println(Log.ASSERT, "FRAGMENT", "CREATED");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        title.append("\"" + barcode + "\"");
        warning.setVisibility(View.INVISIBLE);
        addProduct.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("barcode", barcode);
            AddProductFragment addProductFragment = new AddProductFragment();
            addProductFragment.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_main, addProductFragment)
                    .addToBackStack(null)
                    .commit();
        });
        try {
            JSONArray products = new JSONArray(this.getArguments().getString("productsString"));
            RVAdapterProducts adapter = new RVAdapterProducts(populateProductsList(products));
            rv.setAdapter(adapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    private List<ProductListItem> populateProductsList(JSONArray products) {
        List<ProductListItem> toShowProducts = new ArrayList<ProductListItem>();
        for (int i=0; i<products.length(); i++) {
            try {
                JSONObject item = products.getJSONObject(i);
                toShowProducts.add(new ProductListItem(
                        item.getString("id"),
                        item.getString("name"),
                        item.getString("description"),
                        item.getString("barcode"),
                        item.getString("userId"),
                        item.getString("createdAt"),
                        item.getString("updatedAt"),
                        item.getBoolean("test")
                ));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //TODO: ___DEBUG___
        toShowProducts.add(new ProductListItem("id", "name",  "description", "barcode",
                "userId", "created", "updated", true));
        toShowProducts.add(new ProductListItem("id", "name",  "description", "barcode",
                "userId", "created", "updated", true));
        toShowProducts.add(new ProductListItem("id", "name",  "description", "barcode",
                "userId", "created", "updated", true));
        Log.println(Log.ASSERT, "LIST", toShowProducts.toString());
        if (toShowProducts.size() < 1) {
            warning.setVisibility(View.VISIBLE);
        }
        return toShowProducts;
    }
}
