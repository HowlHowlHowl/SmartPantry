package com.example.smartpantry;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentAddToShoppingList extends Fragment {
    private Button addBtn, dontAddBtn, cancelBtn;
    private EditText quantityField;
    private String id;
    boolean delete;
    private long quantity;
    private int position;
    private final AddToShoppingListEvent shoppingListener;
    public interface AddToShoppingListEvent {
        void deleteProductFromPantry(int position, Context context);
        void updateProductShoppingQuantity(int position, long toBuyQnt);
    }

    public FragmentAddToShoppingList(AddToShoppingListEvent listener) {
        this.shoppingListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_add_to_shopping_list, container, false);
        addBtn  = view.findViewById(R.id.addShoppingBtn);
        dontAddBtn  = view.findViewById(R.id.dontAddShoppingBtn);
        quantityField = view.findViewById(R.id.toAddQuantityField);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        //Close frag on click outside
        view.findViewById(R.id.addQuantity).setOnClickListener(v->{
            getActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        //Remove touch on window event to close frag
        view.findViewById(R.id.bgPopUp).setOnClickListener(v-> {
        });

        id = getArguments().getString("id", "");
        quantity = getArguments().getLong("quantity", 1);
        quantity = quantity > 0 ? quantity : 1;
        delete = getArguments().getBoolean("delete", false);
        position = getArguments().getInt("position", -1);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //If the fragment has the responsibility to delete the product the cancel button is shown
        //to give the user the possibility to cancel the operation
        cancelBtn.setVisibility(delete ? View.VISIBLE : View.GONE);
        cancelBtn.setOnClickListener(v-> {
            getActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });
        addBtn.setOnClickListener(v->{
            String addQuantityString = quantityField.getText().toString();
            if(!addQuantityString.isEmpty()) {
                if (delete) {
                    shoppingListener.deleteProductFromPantry(position, getContext());
                }
                long addQuantity = Long.parseLong(addQuantityString);
                DBHelper db = new DBHelper(getContext());
                db.addToShoppingList(id, addQuantity);
                db.close();
                shoppingListener.updateProductShoppingQuantity(position, addQuantity);
                getActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            } else {
                quantityField.setError(getString(R.string.addProductQuantityError));
            }
        });

        dontAddBtn.setOnClickListener(v->{
            if (delete) {
                shoppingListener.deleteProductFromPantry(position, getContext());
            }

            getActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        //Write past quantity
        quantityField.setText(String.valueOf(quantity));
    }

}
