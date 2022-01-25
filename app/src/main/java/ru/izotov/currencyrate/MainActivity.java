package ru.izotov.currencyrate;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final String URL_ADDRESS = "https://www.cbr-xml-daily.ru/daily_json.js";
    private static final String FILE_NAME = "currency.json";
    private static final int UPDATE_PERIOD = 15000;

    Spinner currencySelectSpinner;
    Button convertButton;
    Button updateButton;
    EditText sumEditText;
    TextView convertResultTextView;
    TextView listCurrencyTextView;

    String convertResult = "";
    Map<String, Currency> currencyMap = new TreeMap<>();
    List<String> currencyNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currencySelectSpinner = findViewById(R.id.spinner_currencySelect);
        sumEditText = findViewById(R.id.editText_sum);
        convertButton = findViewById(R.id.button_convert);
        updateButton = findViewById(R.id.button_update);
        convertResultTextView = findViewById(R.id.textView_result);
        listCurrencyTextView = findViewById(R.id.textView_listCurrency);

        UpdateTask updateTask = new UpdateTask(!getFileStreamPath(FILE_NAME).exists());
        updateTask.execute();

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                UpdateTask updateTask = new UpdateTask(true);
                updateTask.execute();
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, UPDATE_PERIOD);
    }

    public void onClickConvertCurrency(View view) {
        ConvertTask convertTask = new ConvertTask();
        convertTask.execute();
    }

    public void onClickUpdateCurrency(View view) {
        UpdateTask updateTask = new UpdateTask(true);
        updateTask.execute();
    }


    class ConvertTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            convertCurrency();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(convertResult.equals("")) {
                Toast.makeText(getApplicationContext(), "Введите сумму для конвертации", Toast.LENGTH_SHORT).show();
            } else {
                convertResultTextView.setText(convertResult + " рублей");
            }
        }

        private void convertCurrency() {
            String currencyCode = currencySelectSpinner.getSelectedItem().toString();
            double currencyRate = Objects.requireNonNull(currencyMap.get(currencyCode)).getValue();
            String sumString = sumEditText.getText().toString();
            if(sumString.length() > 0) {
                double sum = Double.parseDouble(sumString);
                convertResult = new DecimalFormat("#0.00").format(sum * currencyRate);
            } else {
                convertResult = "";
            }
        }
    }


    class UpdateTask extends AsyncTask<Void, Void, Void> {
        boolean isUpdate;

        UpdateTask(boolean isUpdate) {
            this.isUpdate = isUpdate;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(isUpdate) {
                String jsonString = getResponseFromURL();
                readCurrencyRates(jsonString);
                createFile(MainActivity.this, FILE_NAME, jsonString);
            } else {
                readCurrencyRates(readFile(MainActivity.this, FILE_NAME));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            updateCurrencySpinner();
            updateListCurrencyTextView();
            if(isUpdate) {
                Toast.makeText(getApplicationContext(), "Курсы валют обновлены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Курсы валют загружены из файла", Toast.LENGTH_SHORT).show();
            }
        }

        private void updateCurrencySpinner() {
            currencyNames = new ArrayList<>();
            for(Map.Entry<String, Currency> entry : currencyMap.entrySet()) {
                currencyNames.add(entry.getValue().getCharCode());
            }
        }

        private void updateListCurrencyTextView() {
            listCurrencyTextView.setText(getListCurrencyTextView());
            currencySelectSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_dropdown_item_1line, currencyNames));
        }

        private void readCurrencyRates(String response) {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonCurrency = jsonResponse.getJSONObject("Valute");
                Iterator<String> iterator = jsonCurrency.keys();
                while(iterator.hasNext()) {
                    String next = iterator.next();
                    currencyMap.put(next, new Currency(Objects.requireNonNull(jsonCurrency.optJSONObject(next))));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private String readFile(Context context, String fileName) {
            try (FileInputStream fis = context.openFileInput(fileName);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader bufferedReader = new BufferedReader(isr);
            ) {
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } catch (IOException fileNotFound) {
                return null;
            }
        }

        private boolean createFile(Context context, String fileName, String jsonString) {
            try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                if(jsonString != null) {
                    fos.write(jsonString.getBytes());
                }
                return true;
            } catch (IOException fileNotFound) {
                return false;
            }
        }

        private String getResponseFromURL() {
            try (InputStream in = ((HttpURLConnection) new URL(URL_ADDRESS).openConnection()).getInputStream()) {
                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\A");
                if(scanner.hasNext()) {
                    return scanner.next();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String getListCurrencyTextView() {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for(Map.Entry<String, Currency> entry : currencyMap.entrySet()) {
                sb
                        .append(++count)
                        .append(")  ")
                        .append(entry.getValue().getCharCode())
                        .append("  ")
                        .append(entry.getValue().getName())
                        .append("  ")
                        .append(entry.getValue().getValue())
                        .append("\n");
            }
            return sb.toString();
        }
    }
}