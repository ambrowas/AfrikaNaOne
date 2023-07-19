package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import iniciativaselebi.com.guinealogiaediciontrivial.R;


public class Menuprincipal extends AppCompatActivity {

        Button button_modolibre, button_modocompeticion, button_contactanos;
        EditText editTextNombre;
        ImageView logo;

        String nombre;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_menuprincipal);

            logo = (ImageView) findViewById(R.id.logo);
            Rotate3dAnimation animation = new Rotate3dAnimation(0, 360, logo.getWidth()/2, logo.getHeight()/2, 0, true);
            animation.setDuration(1000);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            logo.startAnimation(animation);

            
            button_modolibre = findViewById(R.id.button_modolibre);
            button_modocompeticion = findViewById(R.id.button_modocompeticion);
            button_contactanos = findViewById(R.id.button_contactanos);
            button_contactanos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Menuprincipal.this, ContactanosActivity.class);
                    startActivity(intent);
                }
            });

            button_modocompeticion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Menuprincipal.this, Modocompeticion.class);
                    startActivity(intent);
                }
            });


            button_modolibre.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Menuprincipal.this, ModoLibre.class);
                    startActivity(intent);
                }
            });
        }
    }
