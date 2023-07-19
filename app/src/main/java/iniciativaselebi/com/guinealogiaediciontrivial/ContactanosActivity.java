package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import iniciativaselebi.com.guinealogiaediciontrivial.R;


public class ContactanosActivity extends AppCompatActivity {

    ImageButton imagebuttonwhatsap;
    Button buttonregresar;
    Animation growShrinkAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactanos);


        buttonregresar = (Button) findViewById(R.id.buttonregresar);
        buttonregresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContactanosActivity.this, Menuprincipal.class);
                startActivity(intent);
            }
        });
        imagebuttonwhatsap = (ImageButton) findViewById(R.id.imagebuttonwhatsap);
        growShrinkAnimation = AnimationUtils.loadAnimation(this, R.anim.shrink_animation);
        imagebuttonwhatsap.startAnimation(growShrinkAnimation);
        imagebuttonwhatsap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            openWhatsapp();
            }
        });
    }

    private void openWhatsapp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "https://api.whatsapp.com/send?phone=" + "+240222780886"; // Replace 'your_phone_number' with the phone number you want to open the chat with
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
    }