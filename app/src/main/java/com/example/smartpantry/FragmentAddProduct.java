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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class FragmentAddProduct extends Fragment implements FragmentIconPicker.onIconChosen{

    private String icon = Global.ICON_DIRNAME + File.separator + Global.DEFAULT_ICON;

    private ConstraintLayout expendable;
    private EditText nameField, descriptionField, expireDateField, quantityField;
    private CheckBox testCheckBox;
    private SwitchCompat switchExpand;
    private Button addProductButton;
    private ImageButton cancelDateButton;
    private ImageView iconPicker;

    onProductAddedListener productAddedListener;

    @Override
    public void iconSelected(String icon, int position) {
        onSelectIconPressed(icon);
    }

    public interface onProductAddedListener {
        void onProductAdded(String id, String name, String barcode,
                            String description, String expire,
                            long quantity, String icon, boolean test, boolean addLocal, boolean isNew);
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
        view.findViewById(R.id.addProdFragBG).setOnClickListener(v->{});

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean alreadyExistingProduct = getArguments().getBoolean("alreadyExistingProduct", false);
        if(alreadyExistingProduct) {
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

            DateFormat df = DateFormat.getDateInstance();
            DatePickerDialog dpd  = new DatePickerDialog(
                    getContext(),
                    R.style.MyDatePickerDialogTheme,
                    (vv, year, month, day) -> {
                        myCalendar.set(year, month, day);

                        expireDateField.setText(df.format(myCalendar.getTime()));
                    },
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            String expireDate = expireDateField.getText().toString();
            if(!expireDate.isEmpty()) {
                try {
                    //Set current expire date on date picker
                    Date date = df.parse(expireDate);
                    Calendar currentExpireDate = Calendar.getInstance();
                    currentExpireDate.setTime(date);

                    dpd.updateDate(
                            currentExpireDate.get(Calendar.YEAR),
                            currentExpireDate.get(Calendar.MONTH),
                            currentExpireDate.get(Calendar.DAY_OF_MONTH)
                    );
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            dpd.show();
        });

        //Clear date field
        cancelDateButton.setOnClickListener(v->{
            expireDateField.setText("");
        });

        //Icon Picker Event
        iconPicker.setOnClickListener(v -> {
            FragmentIconPicker fragmentIconPicker = new FragmentIconPicker(this);
            //iconPickerFragment.setArguments();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_main, fragmentIconPicker)
                    .addToBackStack(null)
                    .commit();
        });

        //Add product event
        addProductButton.setOnClickListener(v -> {

            String productID = null;
            if(getArguments().getBoolean("alreadyExistingProduct", false)){
                productID = getArguments().getString("id");
            }
            String barcode = getArguments().getString("barcode");
            String name = nameField.getText().toString();
            String description = descriptionField.getText().toString();
            String quantity = quantityField.getText().toString();

            //Save date in fixed format dd/MM/yyyy
            String date = expireDateField.getText().toString();
            String formattedDate="";
            if(!date.isEmpty()) {
                DateFormat originalFormat = DateFormat.getDateInstance();
                DateFormat targetFormat = new SimpleDateFormat(Global.DB_DATE_FORMAT, Locale.getDefault());
                formattedDate = Global.changeDateFormat(date, originalFormat, targetFormat);
            }
            boolean test = testCheckBox.isChecked();
            boolean addLocal = switchExpand.isChecked();
            if (checkFields(name, description, quantity)) {
                productAddedListener.onProductAdded(productID, barcode, name, description,
                        formattedDate,
                        Long.parseLong(quantity), icon, test, addLocal,
                        !alreadyExistingProduct);
                getActivity()
                        .getSupportFragmentManager()
                        .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    private boolean checkFields(String name, String description, String quantity) {

        if (name.isEmpty()) {
            nameField.setError(getResources().getString(R.string.addProductNameError));
            return false;
        }
        if (description.isEmpty()) {
            descriptionField.setError(getResources().getString(R.string.addProductDescriptionError));
            return false;
        }
        if (Long.parseLong(quantity) <= 0 ) {
            quantityField.setError(getResources().getString(R.string.addProductQuantityError));
            return false;
        }
        return true;
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
    }

    public void onSelectIconPressed(String iconFileName) {
        icon = iconFileName;
        AssetManager assetsManager = getContext().getAssets();
        try {
            InputStream ims;
            ims = assetsManager.open(icon);
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            ims.close();
            iconPicker.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(),
                    getResources().getString(R.string.errorRestartApp),
                    Toast.LENGTH_SHORT).show();
        }

    }
}
