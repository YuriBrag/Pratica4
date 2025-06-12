package yuri.bragine.pratica4;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    public static final String ACTION_CLASSIFY_SENSOR = "br.ufrn.imd.intent.action.CLASSIFY";
    public static final String EXTRA_LIGHT_VALUE = "LIGHT_VALUE";
    public static final String EXTRA_PROXIMITY_VALUE = "PROXIMITY_VALUE";
    public static final String EXTRA_LIGHT_CLASSIFICATION = "LIGHT_CLASSIFICATION";
    public static final String EXTRA_PROXIMITY_CLASSIFICATION = "PROXIMITY_CLASSIFICATION";

    private Switch swLant, swVib;
    private Button btnClas;
    private TextView txtReadings;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor proximitySensor;


    private float currentLightValue = 0.0f;
    private float currentProximityValue = 0.0f;

    private LanternaHelper lanternaHelper;
    private MotorHelper motorHelper;

    private ActivityResultLauncher<Intent> classificationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swLant = findViewById(R.id.swLant);
        swVib = findViewById(R.id.swVib);
        btnClas = findViewById(R.id.btnClas);
        txtReadings = findViewById(R.id.txtView);

        swLant.setClickable(false);
        swVib.setClickable(false);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (lightSensor == null) {
            Toast.makeText(this, "Sensor de Luminosidade não encontrado!", Toast.LENGTH_SHORT).show();
        }
        if (proximitySensor == null) {
            Toast.makeText(this, "Sensor de Proximidade não encontrado!", Toast.LENGTH_SHORT).show();
        }

        lanternaHelper = new LanternaHelper(this);
        motorHelper = new MotorHelper(this);

        classificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            handleClassificationResult(data);
                        }
                    }
                });

        btnClas.setOnClickListener(v -> {
            Intent intent = new Intent(ACTION_CLASSIFY_SENSOR);
            intent.putExtra(EXTRA_LIGHT_VALUE, currentLightValue);
            intent.putExtra(EXTRA_PROXIMITY_VALUE, currentProximityValue);

            if (intent.resolveActivity(getPackageManager()) != null) {
                classificationLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Aplicativo classificador não encontrado!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleClassificationResult(Intent data) {
        String lightClassification = data.getStringExtra(EXTRA_LIGHT_CLASSIFICATION);
        if ("baixa".equalsIgnoreCase(lightClassification)) {
            lanternaHelper.ligar();
            swLant.setChecked(true);
        } else if ("alta".equalsIgnoreCase(lightClassification)) {
            lanternaHelper.desligar();
            swLant.setChecked(false);
        }

        String proximityClassification = data.getStringExtra(EXTRA_PROXIMITY_CLASSIFICATION);
        if ("distante".equalsIgnoreCase(proximityClassification)) {
            motorHelper.iniciarVibracao();
            swVib.setChecked(true);
        } else if ("proximo".equalsIgnoreCase(proximityClassification)) {
            motorHelper.pararVibracao();
            swVib.setChecked(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lanternaHelper.desligar();
        motorHelper.pararVibracao();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            currentLightValue = event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            currentProximityValue = event.values[0];
        }

        String readings = String.format("Luminosidade: %.2f lx\nProximidade: %.2f cm", currentLightValue, currentProximityValue);
        txtReadings.setText(readings);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
