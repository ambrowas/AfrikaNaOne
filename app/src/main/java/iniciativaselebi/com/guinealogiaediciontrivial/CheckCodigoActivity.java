package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckCodigoActivity extends AppCompatActivity {
    private Button button_validar, button_regresar;
    private DatabaseReference databaseRef, userRef;
    private String userID, fullname;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_check_codigo);

            final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("gamecodes");

            button_regresar = (Button)findViewById(R.id.button_regresar);
            button_regresar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CheckCodigoActivity.this, Modocompeticion.class);
                    startActivity(intent);
                }
            });
            button_validar = (Button) findViewById(R.id.button_validar);

            button_validar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Collect the digits entered by the user
                    String enteredCode = "";
                    for (int i = 1; i <= 4; i++) {
                        int id = getResources().getIdentifier("editTextCode" + i, "id", getPackageName());
                        EditText editText = findViewById(id);
                        enteredCode += editText.getText().toString();
                    }

                    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
                    String finalEnteredCode = enteredCode;
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String fullName = dataSnapshot.child("fullname").getValue(String.class);
                            validateCode(userId, fullName, finalEnteredCode);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Handle database error
                        }
                    });
                }
            });
        }

        private void validateCode(String userId, String fullName, String enteredCode) {
            DatabaseReference gameCodesRef = FirebaseDatabase.getInstance().getReference("gamecodes");
            gameCodesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean codeFound = false;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String code = String.valueOf(snapshot.child("code").getValue(Long.class));

                        if (enteredCode.equals(code)) {
                            codeFound = true;
                            handleCodeValidation(snapshot, userId, fullName);
                            break;
                        }
                    }

                    if (!codeFound) {
                        Toast.makeText(getApplicationContext(), "CODIGO INVALIDO", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void handleCodeValidation(DataSnapshot snapshot, String userId, String fullName) {
            if (Boolean.parseBoolean(snapshot.child("used").getValue(String.class))) {
                Toast.makeText(getApplicationContext(), "ESTE CODIGO YA HA SIDO USADO. INTRODUZCA UNO NUEVO", Toast.LENGTH_SHORT).show();
                return;
            }

            String timeStamp = snapshot.child("timestamp").getValue(String.class);
            DateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
            try {
                Date codeDate = formatter.parse(timeStamp);
                Date currentDate = new Date();
                long diff = currentDate.getTime() - codeDate.getTime();
                long diffHours = diff / (60 * 60 * 1000);
                if (diffHours <= 72) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
                    userRef.child("currentGameFallos").setValue(0);
                    userRef.child("currentGameAciertos").setValue(0);
                    userRef.child("currentGamePuntuacion").setValue(0);
                    snapshot.getRef().child("usedByUserID").setValue(userId);
                    snapshot.getRef().child("usedByfullname").setValue(fullName);

                    Toast.makeText(getApplicationContext(), "CODIGO VALIDADO: BUENA SUERTE", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "CODIGO CADUCADO. INTRODUZCA UNO NUEVO", Toast.LENGTH_SHORT).show();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            snapshot.getRef().child("used").setValue(true);
            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
            String usedTimestamp = df.format(new Date());
            snapshot.getRef().child("usedTimestamp").setValue(usedTimestamp);
        }
    }




