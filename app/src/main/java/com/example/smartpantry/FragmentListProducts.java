
package com.example.smartpantry;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentListProducts extends Fragment {
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
        Log.println(Log.ASSERT, "LIST PRODUCT FRAGMENT", "CREATED");
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
            FragmentAddProduct fragmentAddProduct = new FragmentAddProduct();
            fragmentAddProduct.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_main, fragmentAddProduct, "addProductFragment")
                    .addToBackStack(null)
                    .commit();
        });
        try {
            JSONArray products = new JSONArray(this.getArguments().getString("productsString"));
            List<ProductListGeneric> populatedProductsList = populateProductsList(products);
            AdapterProductsList adapter = new AdapterProductsList(getContext(), populatedProductsList);
            listProducts.setAdapter(adapter);
            listProducts.setOnItemClickListener((parent, view1, position, id) -> {
                ProductListGeneric item = populatedProductsList.get(position);
                //If clicked item isn't an header
                if(item.getViewType() == 0) {
                    ProductListItem castedItem  = ((ProductListItem) item);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("isUserOwned", castedItem.isUserOwned);
                    bundle.putString(Global.ID, castedItem.id);
                    bundle.putString("name", castedItem.name);
                    bundle.putString("barcode", castedItem.barcode);
                    bundle.putString("description", castedItem.description);
                    FragmentPreviewProduct fragmentPreviewProduct = new FragmentPreviewProduct();
                    fragmentPreviewProduct.setArguments(bundle);
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.activity_main, fragmentPreviewProduct, "previewFragment")
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

        String userID = getActivity()
                .getSharedPreferences("UserDara", Context.MODE_PRIVATE)
                .getString(Global.ID,"");

        //Add user owned items first w/ relative header
        toShowProducts.addAll(addOwnedItems(products, userID));

        //The the other items are added w/ relative header
        toShowProducts.addAll(addOtherItems(products, userID));

        if (toShowProducts.size() < 1) {
            warning.setVisibility(View.VISIBLE);
        }

        Log.println(Log.ASSERT, "LIST RAW", products.toString());
        Log.println(Log.ASSERT, "LIST", toShowProducts.toString());

        return toShowProducts;
    }
    public ArrayList<ProductListGeneric> addOwnedItems(JSONArray products, String userID) {
        ArrayList<ProductListGeneric> ownedItemsList = new ArrayList<>();
        for (int i = 0; i < products.length(); i++) {
            try {
                JSONObject item = products.getJSONObject(i);
                if(item.getString(Global.ID).equals(userID)) {
                    ownedItemsList.add(new ProductListItem(
                            item.getString(Global.ID),
                            item.getString("name"),
                            item.getString("description"),
                            item.getString("barcode"),
                            item.getString("userId"),
                            item.getString("createdAt"),
                            item.getString("updatedAt"),
                            item.getBoolean("test"),
                            true
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(!ownedItemsList.isEmpty()){
            ownedItemsList.add(0, new ProductListHeader(
                    getResources().getString(R.string.ownedProductsHeader))
            );
        }
        return ownedItemsList;
    }
    public ArrayList<ProductListGeneric> addOtherItems(JSONArray products, String userID) {
        ArrayList<ProductListGeneric> otherItemsList = new ArrayList<>();
        for (int i = 0; i < products.length(); i++) {
            try {
                JSONObject item = products.getJSONObject(i);
                if(!item.getString(Global.ID).equals(userID)) {
                    otherItemsList.add(new ProductListItem(
                            item.getString(Global.ID),
                            item.getString("name"),
                            item.getString("description"),
                            item.getString("barcode"),
                            item.getString("userId"),
                            item.getString("createdAt"),
                            item.getString("updatedAt"),
                            item.getBoolean("test"),
                            false
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(!otherItemsList.isEmpty()){
            otherItemsList.add(0, new ProductListHeader(
                    getResources().getString(R.string.otherProductsHeader))
            );
        }
        return otherItemsList;
    }
}

