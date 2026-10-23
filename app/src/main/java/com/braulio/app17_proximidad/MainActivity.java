package com.braulio.app17_proximidad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensor;
    private Vibrator vibrator;
    private TextToSpeech tts;
    private TextView estado, valor, distancia, datosSensor;
    private Button botonVoz;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean cerca = false;
    private boolean vozActiva = true;
    private boolean vozLista = false;
    private float rangoMaximo = 1f;
    private long ultimaVibracion = 0;

    private final Runnable lecturaVoz = new Runnable() {
        @Override public void run() {
            if (vozActiva && sensor != null) {
                if (vozLista && tts != null) {
                    tts.speak(cerca ? "Estás cerca" : "Estás lejos",
                            TextToSpeech.QUEUE_FLUSH, null, "estado_proximidad");
                }
                handler.postDelayed(this, 2000);
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        estado = findViewById(R.id.tvEstado);
        valor = findViewById(R.id.tvValor);
        distancia = findViewById(R.id.tvDistancia);
        datosSensor = findViewById(R.id.tvSensor);
        botonVoz = findViewById(R.id.btnVoz);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int resultado = tts.setLanguage(new Locale("es", "MX"));
                if (resultado == TextToSpeech.LANG_MISSING_DATA ||
                    resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                    resultado = tts.setLanguage(new Locale("es", "ES"));
                }
                vozLista = resultado != TextToSpeech.LANG_MISSING_DATA &&
                           resultado != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        if (sensor == null) {
            estado.setText("Sin sensor");
            datosSensor.setText("Este dispositivo no reporta un sensor de proximidad.");
            valor.setText("Valor lógico: --");
            distancia.setText("Distancia física: --");
            botonVoz.setEnabled(false);
            Toast.makeText(this, "No se encontró sensor de proximidad", Toast.LENGTH_LONG).show();
            return;
        }

        rangoMaximo = sensor.getMaximumRange();
        datosSensor.setText("Sensor: " + sensor.getName() + "\nFabricante: " +
                sensor.getVendor() + "\nRango máximo: " + rangoMaximo + " cm");

        botonVoz.setOnClickListener(v -> {
            vozActiva = !vozActiva;
            if (vozActiva) {
                botonVoz.setText("Detener lectura en voz alta");
                handler.removeCallbacks(lecturaVoz);
                lecturaVoz.run();
            } else {
                botonVoz.setText("Reanudar lectura en voz alta");
                handler.removeCallbacks(lecturaVoz);
                if (tts != null) tts.stop();
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            handler.removeCallbacks(lecturaVoz);
            if (vozActiva) handler.postDelayed(lecturaVoz, 500);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(lecturaVoz);
        if (tts != null) tts.stop();
        if (vibrator != null) vibrator.cancel();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (vibrator != null) vibrator.cancel();
        super.onDestroy();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_PROXIMITY || event.values.length == 0) return;
        float cm = event.values[0];
        cerca = cm < rangoMaximo;
        estado.setText(cerca ? "ESTÁS CERCA" : "ESTÁS LEJOS");
        estado.setTextColor(cerca ? Color.rgb(191, 54, 104) : Color.rgb(96, 65, 143));
        valor.setText("Valor lógico: " + (cerca ? "1 (cerca)" : "0 (lejos)"));
        distancia.setText("Distancia física: " + cm + " cm");
        if (cerca) vibrarSegunDistancia(cm);
        else if (vibrator != null) vibrator.cancel();
    }

    private void vibrarSegunDistancia(float cm) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaVibracion < 180) return;
        ultimaVibracion = ahora;
        float cercania = 1f - Math.max(0f, Math.min(cm / Math.max(rangoMaximo, 1f), 1f));
        int amplitud = Math.max(1, Math.min(255, 80 + Math.round(cercania * 175f)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(140, amplitud));
        } else {
            vibrator.vibrate(140);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}