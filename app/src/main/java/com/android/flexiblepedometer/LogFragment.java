package com.android.flexiblepedometer;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by AREG on 02.03.2017.
 */

public class LogFragment extends Fragment {

    private TextView mlog_window;
    private Button mclear_button;
    private Button mread_button;

    final String FILENAME = "log_file_pedometer";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.log_fragment, container, false);

        mlog_window = (TextView) v.findViewById(R.id.log_window);
        mlog_window.setMovementMethod(ScrollingMovementMethod.getInstance());

        mclear_button = (Button) v.findViewById(R.id.clear_button);
        mread_button = (Button) v.findViewById(R.id.read_button);

        mclear_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LogFragment", "==Button Clear pressed ==");
                writeFile(getContext());
                readFile(getContext());
            }
        });

        mread_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LogFragment", "==Button Read pressed ==");
                readFile(getContext());
            }

        });


            return v;
    }

    void readFile(Context ctx) {
        String strreading = "";
        try {
            // открываем поток для чтения
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    ctx.openFileInput(FILENAME)));
            String str = "";
            // читаем содержимое
            while ((str = br.readLine()) != null) {
              //  Log.d("LogFragment", str);
                strreading = strreading + str + "\n";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mlog_window.setText(strreading);
    }
    void writeFile(Context ctx) {
        try {
            // отрываем поток для записи
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    ctx.openFileOutput(FILENAME, MODE_PRIVATE)));
            // пишем данные
            bw.write("");
            // закрываем поток
            bw.close();
            Log.d("LogFragment", "Файл очищен");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
