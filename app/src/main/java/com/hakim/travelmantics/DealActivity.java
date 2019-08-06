package com.hakim.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    public static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDesciption;
    TravelDeal deal;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_acivity);

//        FirebaseUtil.openFbReference("traveldeals", this);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        txtTitle = (EditText) findViewById(R.id.txtTitle);
        txtPrice = (EditText) findViewById(R.id.txtPrice);
        txtDesciption = (EditText) findViewById(R.id.txtDesciption);
        imageView = (ImageView) findViewById(R.id.image);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null){
            deal = new TravelDeal();

        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDesciption.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());

        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin == true){
            menu.findItem(R.id.save_menu).setVisible(true);
            menu.findItem(R.id.delete_menu).setVisible(true);
            enableEdittext(true);
        } else {
            menu.findItem(R.id.save_menu).setVisible(false);
            menu.findItem(R.id.delete_menu).setVisible(false);
            enableEdittext(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            Uri imageuri = data.getData();
            final StorageReference ref = FirebaseUtil.mStorageRef.child(imageuri.getLastPathSegment());

            UploadTask uploadTask = ref.putFile(imageuri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        System.out.println("Upload " + downloadUri);
//                        Toast.makeText(mActivity, "Successfully uploaded", Toast.LENGTH_SHORT).show();
                        if (downloadUri != null) {

                            String photoStringLink = downloadUri.toString(); //YOU WILL GET THE DOWNLOAD URL HERE !!!!
                            System.out.println("Upload " + photoStringLink);
                            deal.setImageUrl(photoStringLink);
                            showImage(photoStringLink);
                        }

                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });


//            ref.putFile(imageuri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    String url = taskSnapshot.getStorage().getDownloadUrl().toString();
//                    deal.setImageUrl(url);
//                }
//            });
        }
    }

    private void clean() {
        txtTitle.setText("");
        txtDesciption.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDesciption.getText().toString());
        if (deal.getId() == null){
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }


    }

    private void deleteDeal(){
        if (deal.getId() == null){
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_SHORT).show();
        }
        mDatabaseReference.child(deal.getId()).removeValue();
    }

    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    private void enableEdittext(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        txtDesciption.setEnabled(isEnabled);
    }

    private void showImage(String url){
        if (url != null && url.isEmpty() == false){
            int width  = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get().load(url)
                    .resize(width, width*2/3)
                    .centerCrop()
                    .into(imageView);

        }
    }

}
