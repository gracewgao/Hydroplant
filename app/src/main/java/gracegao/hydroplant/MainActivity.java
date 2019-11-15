package gracegao.hydroplant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

// This class is the main Activity when the user first opens the app
public class MainActivity extends AppCompatActivity {

    // Views
    private EditText editWater;
    private TextView plantTv;
    private ImageView plantImage, puddleImage, cloud1, cloud2, cloud3, skyBg;

    private int water, state;
    private String name;

    // Array to store images of plant states
    private Drawable[] states = new Drawable[8];

    // Phone storage
    private SharedPreferences settings;

    // Class
    private Timer timer;
    private Handler handler = new Handler();

    // When the Activity is started (similar to the main method)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assigns variables to each view
        editWater = findViewById(R.id.editWater);
        plantTv =  findViewById(R.id.plantName);
        plantImage = findViewById(R.id.plantImage);
        puddleImage = findViewById(R.id.puddleImage);
        skyBg = findViewById(R.id.skyBg);
        cloud1 = findViewById(R.id.cloud1);
        cloud2 = findViewById(R.id.cloud2);
        cloud3 = findViewById(R.id.cloud3);

        // Sets timer to move clouds in the background
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Cloud moving right
                        float cloud1X = cloud1.getX();
                        if(cloud1X<skyBg.getWidth())
                            cloud1.setX(cloud1X+2);
                        else {
                            cloud1.setX(0 - cloud1.getWidth());
                            cloud1.setAlpha((float)(Math.random()/2 + 0.5));
                        }
                        // Cloud moving left
                        float cloud2X = cloud2.getX();
                        if(cloud2X > 0-cloud2.getWidth())
                            cloud2.setX(cloud2X-4);
                        else {
                            cloud2.setX(skyBg.getWidth());
                            cloud2.setAlpha((float)(Math.random()/2 + 0.5));
                        }
                        // Cloud moving left
                        float cloud3X = cloud3.getX();
                        if(cloud3X > 0-cloud3.getWidth())
                            cloud3.setX(cloud3X-2);
                        else {
                            cloud3.setX(skyBg.getWidth());
                            cloud3.setAlpha((float)(Math.random()/2 + 0.5));
                        }
                    }
                });
            }
        }, 0, 20);

        // Initializes array to store images corresponding to plant state
        for (int i=0; i<states.length; i++){
            states[i] = getResources().getDrawable(R.drawable.plant+(i+1));
        }

        // Retrieves amount of water from storage
        settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        water = settings.getInt("WATER", 0);
        // Changes display based on water amount
        changeWater(water);

        // Retrieves plant name from storage
        name = settings.getString("NAME", "");
        if (name.equals("")){
            changeName();
        } else {
            plantTv.setText(name);
        }
        // Can change plant name when clicked
        plantTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeName();
            }
        });

        // If the done button on the keyboard is pressed when user enters number
        editWater.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Retrieves user input
                    String s = editWater.getText().toString();
                    // Validates user input
                    boolean validated = true;
                    if (s.isEmpty())
                        validated = false;
                    for(int i = 0; i < s.length(); i++) {
                        if (!Character.isDigit(s.charAt(i)))
                            validated = false;
                    }
                    if (!validated){
                        // Shows an error message if user input is invalid
                        Toast.makeText(getApplicationContext(), "invalid input: please enter a number!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Hides keyboard
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                        // Converts input to an integer and stores the amount
                        water = Integer.parseInt(s);
                        changeWater(water);
                    }
                    return true;
                }
                return false;
            }
        });

        // Schedules a reset everyday at midnight using AlarmReceiver class method
        AlarmReceiver.setReset(this);

        // If the reset happens while the user is using the app, the listener will change the displayed amount
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("WATER")){
                    water = prefs.getInt("WATER", 0);
                    // If the water is changed to 0 from the reset
                    if (water == 0){
                        Toast.makeText(MainActivity.this, "drink up, it's a new day!", Toast.LENGTH_SHORT).show();
                    }
                    changeWater(water);
                }
            }
        };
        settings.registerOnSharedPreferenceChangeListener(listener);

    }

    public void changeName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("name your plant!")
        .setCancelable(false);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.layout_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.layout_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name = input.getText().toString();
                if (name.isEmpty() || name.length() > 15){
                    // Shows an error message if user input is invalid
                    Toast.makeText(getApplicationContext(), "invalid input: please enter a name with less than 15 characters!", Toast.LENGTH_SHORT).show();
                    changeName();
                } else {
                    // Changes plant name displayed and stored
                    plantTv.setText(name);
                    SharedPreferences settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("NAME", name);
                    editor.commit();
                }
            }
        });
        builder.show();
    }

    // Method when user changes amount of water consumed
    public void changeWater(int amount){

        // Gives feedback on water consumption
        if (amount < 500) {
            state = 1;
            if (amount!=0){
                Toast.makeText(getApplicationContext(), "you need more water!", Toast.LENGTH_LONG).show();
            }
        } else if (amount < 1000) {
            state = 2;
            Toast.makeText(getApplicationContext(), "keep it up!", Toast.LENGTH_LONG).show();
        } else if (amount < 1500){
            state = 3;
            Toast.makeText(getApplicationContext(), "keep going!", Toast.LENGTH_LONG).show();
        } else if (amount < 2000){
            state = 4;
            Toast.makeText(getApplicationContext(), "almost there!", Toast.LENGTH_LONG).show();
        } else if (amount < 4000) {
            state = 5;
            Toast.makeText(getApplicationContext(), "your plant is healthy & happy", Toast.LENGTH_LONG).show();
        } else if (amount < 7000) {
            state = 6;
            Toast.makeText(getApplicationContext(), "be careful not to over water!", Toast.LENGTH_LONG).show();
        } else if (amount < 10000) {
            state = 7;
            Toast.makeText(getApplicationContext(), "stop, your plant will drown!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "don't water more than 10L!", Toast.LENGTH_LONG).show();
            // Sets maximum water amount per day
            amount = 9999;
        }

        // Changes plant image according to its hydration state
        plantImage.setImageDrawable(states[state-1]);

        // If plant is over watered, display a puddle
        if (state==7)
            puddleImage.setVisibility(View.VISIBLE);
        else
            puddleImage.setVisibility(View.INVISIBLE);

        water = amount;

        // Displays new amount on screen
        editWater.setText(String.valueOf(water));

        // Saves water amount into phone storage
        SharedPreferences settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("WATER", amount);
        editor.commit();
    }

    // Method called when user clicks the more button
    public void more(View view){
        // Increases water amount by one cup
        water += 250;
        changeWater(water);
    }

    // Method called when user clicks the less button
    public void less(View view){
        // Decrease water amount by one cup
        water -=250;
        // Value must be greater or equal to 0
        if (water<0)
            water = 0;
        changeWater(water);
    }

    // Method called when user clicks the game button
    public void game(View view){
        // Opens a new screen to play the game
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        startActivity(i);
    }
}
