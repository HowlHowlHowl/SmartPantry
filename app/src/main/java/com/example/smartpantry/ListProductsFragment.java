
package com.example.smartpantry;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListProductsFragment extends Fragment {
    private TextView title;
    private TextView warning;
    private ListView listProducts;
    private FloatingActionButton addProduct;
    private String barcode;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products_found_list, container, false);

        title = view.findViewById(R.id.productsResultTitle);
        warning = view.findViewById(R.id.noProductsFound);
        addProduct = view.findViewById(R.id.addProductFloatingBtn);
        listProducts = view.findViewById(R.id.productsRecyclerView);
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
            List<ProductListGeneric> populatedProductsList = populateProductsList(products);
            AdapterProducts adapter = new AdapterProducts(getContext(), populatedProductsList);
            listProducts.setAdapter(adapter);
            listProducts.setOnItemClickListener((parent, view1, position, id) -> {
                ProductListGeneric item = populatedProductsList.get(position);
                Log.d("########", "ITEM CLICKED ");
                //If clicked item isn't an header
                if(item.getViewType() == 0) {
                    ProductListItem castedItem  = ((ProductListItem) item);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("isUserOwned", castedItem.isUserOwned);
                    bundle.putString("id", castedItem.id);
                    bundle.putString("name", castedItem.name);
                    bundle.putString("barcode", castedItem.barcode);
                    bundle.putString("description", castedItem.description);
                    PreviewProductFragment previewProductFragment = new PreviewProductFragment();
                    previewProductFragment.setArguments(bundle);
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.activity_main, previewProductFragment, "previewFragment")
                            .addToBackStack(null)
                            .commit();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private List<ProductListGeneric> populateProductsList(JSONArray products) {
        List<ProductListGeneric> toShowProducts = new ArrayList<ProductListGeneric>();
        String userId = getContext()
                .getSharedPreferences("UserData", Context.MODE_PRIVATE)
                .getString("id", null);
        //the list of matched items is scanned 2 times
        //The first loop is made to add items owned by the user
        //The second one is made to add the other items
        //The reason behind this is to add header and graphically divide the shown list of items
        //TODO: REFACTOR CODE
        Log.println(Log.ASSERT, "LIST RAW", products.toString());
        boolean owned = true;
        for (int j = 0; j < 2; j++) {
            boolean headerNeeded = true;
            for (int i = 0; i < products.length(); i++) {
                try {
                    JSONObject item = products.getJSONObject(i);
                    if (owned) {
                        if (item.getString("userId") == userId) {
                            if (headerNeeded) {
                                toShowProducts.add(new ProductListHeader(getResources().getString(R.string.ownedProductsHeader)));
                                headerNeeded = false;
                            }
                            toShowProducts.add(new ProductListItem(
                                    item.getString("id"),
                                    item.getString("name"),
                                    item.getString("description"),
                                    item.getString("barcode"),
                                    item.getString("userId"),
                                    item.getString("createdAt"),
                                    item.getString("updatedAt"),
                                    item.getBoolean("test"),
                                    (item.getString("userId") == userId)
                            ));
                        }
                    } else {
                        if (headerNeeded) {
                            toShowProducts.add(new ProductListHeader(getResources().getString(R.string.otherProductsHeader)));
                            headerNeeded = false;
                        }
                        toShowProducts.add(new ProductListItem(
                                item.getString("id"),
                                item.getString("name"),
                                item.getString("description"),
                                item.getString("barcode"),
                                item.getString("userId"),
                                item.getString("createdAt"),
                                item.getString("updatedAt"),
                                item.getBoolean("test"),
                                (item.getString("userId") == userId)
                        ));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            owned = false;
        }
        Log.println(Log.ASSERT, "LIST", toShowProducts.toString());
        if (toShowProducts.size() < 1) {
            warning.setVisibility(View.VISIBLE);
        }
        return toShowProducts;
    }
}
