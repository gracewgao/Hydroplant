package gracegao.hydroplant;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

// This class is the main Activity when the user first opens the app
public class MainActivity extends AppCompatActivity {

    // Views
    private EditText editWater;
    private TextView plantTv, goalTv;
    private ImageView plantImage, puddleImage, cloud1, cloud2, cloud3, skyBg;
    private FloatingActionButton more, less, gameButton;

    private int water, state, goal;
    private String name;

    // Array to store images of plant states
    private Drawable[] states = new Drawable[8];

    // Phone storage
    private SharedPreferences settings;

    // Class
    private Timer timer;
    private Handler handler = new Handler();

    final private String SHOWCASE_ID = "SHOWCASE";

    // When the Activity is started (similar to the main method)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assigns variables to each view
        editWater = findViewById(R.id.editWater);
        plantTv =  findViewById(R.id.plantName);
        goalTv = findViewById(R.id.waterLabel);
        plantImage = findViewById(R.id.plantImage);
        puddleImage = findViewById(R.id.puddleImage);
        skyBg = findViewById(R.id.skyBg);
        cloud1 = findViewById(R.id.cloud1);
        cloud2 = findViewById(R.id.cloud2);
        cloud3 = findViewById(R.id.cloud3);
        more = findViewById(R.id.moreButton);
        less = findViewById(R.id.lessButton);
        gameButton = findViewById(R.id.gameButton);

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
        // Retrieves info from storage
        name = settings.getString("NAME", "");
        goal = settings.getInt("GOAL", 0);
        water = settings.getInt("WATER", 0);
        // Changes display based on water amount
        changeWater(water);

        if (name.equals("")||goal==0){
            goal=3000;
            tutorial();
        } else {
            plantTv.setText(name);
            String waterLabel = "/ " + goal + " mL today";
            goalTv.setText(waterLabel);
        }

        // Can change plant name when clicked
        plantTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeName();
            }
        });
        // Can change goal when clicked
        goalTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeGoal();
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

    @Override
    protected void onResume() {
        super.onResume();
        // Retrieves amount of water from storage
        settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        water = settings.getInt("WATER", 0);
        changeWater(water);
    }

    public boolean changeName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<b>Give your plant a name<b>"))
            .setCancelable(false);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setText(name);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.alert_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.alert_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name = input.getText().toString();
                if (name.isEmpty() || name.length() > 15){
                    // Shows an error message if user input is invalid
                    Toast.makeText(getApplicationContext(), "Please enter a name with less than 15 characters!", Toast.LENGTH_SHORT).show();
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
        return true;
    }

    public void changeGoal(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<b>Your daily goal (in mL)</i><b>"))
                .setCancelable(false);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        String goalLabel = Integer.toString(goal);
        input.setText(goalLabel);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.alert_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.alert_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String goalString = input.getText().toString();
                if (goalString.isEmpty() || (Integer.parseInt(goalString)>=10000 || Integer.parseInt(goalString)<1000)){
                    // Shows an error message if user input is invalid
                    Toast.makeText(getApplicationContext(), "Please enter a realistic goal between 1-10L", Toast.LENGTH_SHORT).show();
                    changeGoal();
                } else {
                    // Changes water label displayed and stored
                    goal = Integer.parseInt(input.getText().toString());
                    String waterLabel = "/ " + goal + " mL today";
                    goalTv.setText(waterLabel);
                    SharedPreferences settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("GOAL", goal);
                    editor.commit();
                    changeWidget();
                }
            }
        });
        builder.show();
    }

    private void tutorial(){

        int green = Color.argb(255,49, 113, 48);
        int black = Color.argb(255, 0, 0, 0);
        int white = Color.argb(240, 255, 255, 255);
        Typeface dismiss = Typeface.create("sans-serif-smallcaps", Typeface.NORMAL);

        ShowcaseConfig config = new ShowcaseConfig();
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);
        sequence.setConfig(config);

        MaterialShowcaseView nameView = new MaterialShowcaseView.Builder(this)
                .setTarget(plantImage)
                .setTitleText(Html.fromHtml("Meet your new plant-friend!"))
                .setContentText("You'll help them grow by drinking water so that you can both stay happy and hydrated together. ")
                .setListener(new IShowcaseListener() {
                         @Override
                         public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
                             plantImage.setImageDrawable(states[4]);
                         }
                         @Override
                         public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                             changeName();
                         }
                     }
                )
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setDismissTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();
        MaterialShowcaseView goalView = new MaterialShowcaseView.Builder(this)
                .setTarget(goalTv)
                .setTitleText(Html.fromHtml("Set yourself a goal!"))
                .setContentText(Html.fromHtml("It is typically recommended to drink 2-4 litres of water daily</p><p>Tap the bottom label to edit your goal</p>"))
                .setListener(new IShowcaseListener() {
                         @Override
                         public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
                         }
                         @Override
                         public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                             changeGoal();
                             settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
                             goal = settings.getInt("GOAL", 3000);
                         }
                     }
                )
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();
        MaterialShowcaseView editView = new MaterialShowcaseView.Builder(this)
                .setTarget(goalTv)
                .setTitleText("How to water your plant")
                .setContentText("Tap this number to edit how of water you drank today...")
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();
        MaterialShowcaseView moreView = new MaterialShowcaseView.Builder(this)
                .setTarget(more)
                .setTitleText(" ")
                .setContentText("...or use the buttons at the bottom of the screen to increase/decrease by 250mL")
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setShapePadding(100)
                .setGravity(Gravity.BOTTOM)
                .setDismissOnTouch(true)
                .build();
        MaterialShowcaseView gameView = new MaterialShowcaseView.Builder(this)
                .setTarget(gameButton)
                .setTitleText("Fun times ahead")
                .setContentText("Enjoy quality bonding time with your plant by playing games together!")
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();
        MaterialShowcaseView endView = new MaterialShowcaseView.Builder(this)
                .setTarget(plantImage)
                .setTitleText("Congratulations!")
                .setContentText("You'll be a great plant parent. Now remember to stay hydrated, your plant will thank you for it!")
                .setListener(new IShowcaseListener() {
                         @Override
                         public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
                         }
                         @Override
                         public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                             changeWater(water);
                         }
                     }
                )
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();

        sequence.addSequenceItem(styleView(nameView));
        sequence.addSequenceItem(styleView(goalView));
        sequence.addSequenceItem(styleView(editView));
        moreView.setPadding(84,0, 200, 84);
        sequence.addSequenceItem(moreView);
        sequence.addSequenceItem(styleView(gameView));
        sequence.addSequenceItem(styleView(endView));

        sequence.start();
    }

    public MaterialShowcaseView styleView(MaterialShowcaseView v){
        v.setPadding(84,0, 84, 0);
        return v;
    }

    // Method when user changes amount of water consumed
    public void changeWater(int amount){

        // Gives feedback on water consumption
        if (amount < (float)goal/4) {
            state = 1;
            if (amount!=0){
                Toast.makeText(getApplicationContext(), "you need more water!", Toast.LENGTH_LONG).show();
            }
        } else if (amount < (float)goal/2) {
            state = 2;
            Toast.makeText(getApplicationContext(), "keep it up!", Toast.LENGTH_LONG).show();
        } else if (amount < (float)goal*3/4){
            state = 3;
            Toast.makeText(getApplicationContext(), "keep going!", Toast.LENGTH_LONG).show();
        } else if (amount < goal){
            state = 4;
            Toast.makeText(getApplicationContext(), "almost there!", Toast.LENGTH_LONG).show();
        } else if (amount < goal+((float)(10000-goal)/2)) {
            state = 5;
            Toast.makeText(getApplicationContext(), "your plant is hydrated & happy", Toast.LENGTH_LONG).show();
        } else if ((amount < goal+((float)(10000-goal)*3/4))) {
            state = 6;
            Toast.makeText(getApplicationContext(), "be careful not to overwater!", Toast.LENGTH_LONG).show();
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

        changeWidget();
    }

    private void changeWidget(){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        PlantWidget widget = new PlantWidget();
        widget.updateWidget(this, appWidgetManager, new ComponentName(this, PlantWidget.class));
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
