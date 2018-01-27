package rm.iamjosephvarghese.deadlock2k18;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.hash.Hashing;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Firestore extends AppCompatActivity {



    private FirebaseAuth mAuth;
    private FirebaseUser user;


    String currentHash,previousHash;

    String photoURL;
    String levelString;
    int level;

    ImageView  imageView;
    TextView levelText;
    EditText answer;

    Button submit;

    String toBeHashed,generatedHash;


    SharedPreferences sharedPreferences;
    //assume this is the user id genrated from google auth


    MaterialDialog.Builder builder;
    MaterialDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firestore);


        sharedPreferences = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
//        user=sharedPreferences.getString("UID",null);
//        Log.d("user",user);

        imageView = findViewById(R.id.imageView);
        levelText = findViewById(R.id.levelText);
        submit = findViewById(R.id.submit);
        answer = findViewById(R.id.answer);

        submit.setVisibility(View.INVISIBLE);
        answer.setVisibility(View.INVISIBLE);

        builder = new MaterialDialog.Builder(Firestore.this)
                .title("Correct Answer!")
                .content("Loading Next Question")
                .progress(true,0)
                .progressIndeterminateStyle(true)
                .cancelable(false);
        dialog = builder.build();


        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        final FirebaseFirestore db = FirebaseFirestore.getInstance();


        DocumentReference userRef = db.collection("users").document(user.getUid());


        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                currentHash = documentSnapshot.get("currentHash").toString();
                Log.d("currentHash",currentHash);
                previousHash = documentSnapshot.get("previousHash").toString();
                Log.d("previousHash",previousHash);


                DocumentReference questionRef = db.collection("q").document("questions").collection(currentHash).document(previousHash);
                questionRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        photoURL = documentSnapshot.get("photoURL").toString();
                        Log.d("photoUrl",photoURL);
//                        TODO: fetching level error...need to be implemented
//                        level is currentLevel
//                        level = (Integer) documentSnapshot.get("currentLevel");
                        levelString = documentSnapshot.get("level").toString();
                        level = Integer.parseInt(levelString);
                        Glide.with(getApplicationContext()).load(photoURL).placeholder(R.drawable.common_google_signin_btn_icon_dark).into(imageView);

                        if(!photoURL.equals("")){
                            submit.setVisibility(View.VISIBLE);
                            answer.setVisibility(View.VISIBLE);
                        }else{
//                            String nullPhoto = "https://firebasestorage.googleapis.com/v0/b/tourist-23bb8.appspot.com/o/deadlock_questions%2Fbackground-of-coming-soon-with-a-clock_1017-5059.jpg?alt=media&token=6b9373b5-0bc5-4bfa-9ab7-70fe782ed4bd";
//                            Glide.with(getApplicationContext()).load(nullPhoto).into(imageView);
                            submit.setVisibility(View.INVISIBLE);
                            answer.setVisibility(View.INVISIBLE);

                        }


                        levelText.setText("Level " + levelString);



                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!answer.getText().toString().equals("")){
                    toBeHashed = answer.getText().toString() + photoURL + currentHash;

                    Log.d("tobehashed",toBeHashed);

                    generatedHash = Hashing.sha256()
                            .hashString(toBeHashed, Charset.forName("UTF-8"))
                            .toString();


                    final DocumentReference logRef = db.collection("logs").document();
                    final DocumentReference userUpdateRef = db.collection("users").document(user.getUid());

                    DocumentReference checkRef = db.collection("q").document("questions").collection(generatedHash).document(currentHash);

                    checkRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()){

//                                TODO: change user levels....both previous and together..then refresh page
                                Log.d("Answer","correct");


//                                TODO : error pushing timestamp to logRef

                                WriteBatch batch = db.batch();
                                batch.set(logRef,new LogData(user.getUid(),answer.getText().toString(),level,user.getDisplayName(),user.getEmail(),sharedPreferences.getString("mobno",null),new Date()));


//                                TODO: doing 2 seperate updates for currentHash and previousHash
                                batch.update(userUpdateRef,"previousHash",currentHash);
                                batch.update(userUpdateRef,"currentHash",generatedHash);


                                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("batch","success");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("batch","error");
                                    }


                                });


                                Log.d("refresh","");

                                dialog.show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                       dialog.dismiss();
                                       finish();
                                       startActivity(getIntent());

                                    }
                                },3000);


                            }else{
                                Log.d("Answer","incorrect");
                                logRef.set(new LogData(user.getUid(),answer.getText().toString(),level,user.getDisplayName(),user.getEmail(),sharedPreferences.getString("mobno",null),new Date())).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("incorrectLog","success");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("incorrectLog","error");
                                    }
                                });

                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });


                    Log.d("generatedHash",generatedHash);
                }
            }
        });


    }


}
