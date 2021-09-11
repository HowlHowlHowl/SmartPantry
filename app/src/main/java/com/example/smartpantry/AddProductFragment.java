package com.example.smartpantry;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;


public class AddProductFragment extends Fragment {
    //TODO: put it in res file
    static final String ICONS_DIR_NAME = "groceries_icons";

    private ConstraintLayout expendable;
    private EditText nameField, descriptionField, expireDateField, quantityField;
    private CheckBox testCheckBox;
    private SwitchCompat switchExpand;
    private Button addProductButton;
    private ImageButton cancelDateButton;
    private ImageView iconPicker;
    //Init to default icon TODO: put it in res file
    private String icon = "shopping_basket.png";
    onProductAddedListener productAddedListener;

    public interface onProductAddedListener {
        void productAdded(String name, String barcode,
                          String description, String expire,
                          String quantity, String icon, boolean test, boolean addLocal, boolean isNew);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_add, container, false);
        ((TextView)view.findViewById(R.id.viewFragmentTitle)).append(getArguments().getString("barcode"));
        expendable = view.findViewById(R.id.viewExpireExtendable);
        nameField = view.findViewById(R.id.productNameField);
        descriptionField = view.findViewById(R.id.productDescriptionField);
        quantityField = view.findViewById(R.id.productQuantity);
        testCheckBox = view.findViewById(R.id.testCheckBox);
        expireDateField = view.findViewById(R.id.productExpireDateField);
        switchExpand = view.findViewById(R.id.addToPantry);
        addProductButton = view.findViewById(R.id.viewProductBtn);
        cancelDateButton = view.findViewById(R.id.cancelDateButton);
        iconPicker = view.findViewById(R.id.productIconPreview);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getArguments().getBoolean("alreadyExistingProduct", false)) {
            fillFormData(
                    getArguments().getString("name"),
                    getArguments().getString("description"));
            descriptionField.setEnabled(false);
            nameField.setEnabled(false);
        }
        switchExpand.setChecked(true);
        //Switch event
        switchExpand.setOnCheckedChangeListener((v, isChecked) -> {
            expendable.setVisibility(isChecked  ? View.VISIBLE : View.GONE);
        });

        //Date picker event
        expireDateField.setOnClickListener(v -> {
            //Set date picker
            Calendar myCalendar = Calendar.getInstance();
            DatePickerDialog dpd  = new DatePickerDialog(
                    getContext(),
                    R.style.MyDatePickerDialogTheme,
                    (vv, year, month, day) -> {
                        myCalendar.set(year, month, day);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        expireDateField.setText(sdf.format(myCalendar.getTime()));
                    },
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            dpd.show();
        });
        //Icon Picker Event
        iconPicker.setOnClickListener(v -> {
            IconPickerFragment iconPickerFragment = new IconPickerFragment();
            //iconPickerFragment.setArguments();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main, iconPickerFragment)
                    .addToBackStack(null)
                    .commit();
        });

        //Add product event
        addProductButton.setOnClickListener(v -> {
            String barcode = getArguments().getString("barcode");
            String name = nameField.getText().toString();
            String description = descriptionField.getText().toString();
            String quantity = quantityField.getText().toString();
            String date = expireDateField.getText().toString();
            String fullIconFilename = ICONS_DIR_NAME + File.separator + icon;
            boolean test = testCheckBox.isChecked();
            boolean addLocal = switchExpand.isChecked();
            if (checkFields(name, description)) {
                productAddedListener.productAdded(barcode, name, description, date, quantity, fullIconFilename, test, addLocal,
                        !getArguments().getBoolean("alreadyExistingProduct", false));
                getActivity()
                        .getSupportFragmentManager()
                        .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        //Clear date field
        cancelDateButton.setOnClickListener(v->{
            expireDateField.setText("");
        });
    }

    private boolean checkFields(String name, String description) {
        boolean formOk = true;
        if (name.isEmpty()) {
            nameField.setError(getResources().getString(R.string.addProductNameError));
            formOk = false;
        }
        if (description.isEmpty()) {
            descriptionField.setError(getResources().getString(R.string.addProductDescriptionError));
            formOk = false;
        }
        return formOk;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            productAddedListener = (onProductAddedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onProductAddedListener");
        }
    }

    public void fillFormData(String name, String description) {
        nameField.setText(name);
        descriptionField.setText(description);
        switchExpand.setEnabled(false);
        //TODO: CHANGE ADD BUTTON BEHAVIOR.

    }

    public void onSelectIconPressed(String iconFileName) {
        icon = iconFileName;
        AssetManager assetsManager = getContext().getAssets();
        try {
            InputStream ims;
            ims = assetsManager.open(ICONS_DIR_NAME + File.separator + iconFileName);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            iconPicker.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: TAKE TEXT FROM RES
            Toast.makeText(getContext(),
                    "An error occurred, please close the app and try again",
                    Toast.LENGTH_SHORT).show();
        }

    }
}
