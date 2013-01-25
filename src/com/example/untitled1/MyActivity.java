package com.example.untitled1;

import android.app.Activity;
import android.os.Bundle;
import com.thegoan.supersify.Controller;
import android.view.View;
import android.widget.Button;
import com.thegoan.supersify.SuperSify;
import android.widget.TextView;
import android.widget.EditText;

public class MyActivity extends Activity
{

    public void login(View view){

        TextView t = (TextView)findViewById(R.id.resultText);
        EditText u = (EditText)findViewById(R.id.usernameTXT);
        EditText p = (EditText)findViewById(R.id.passwordTXT);
        EditText m = (EditText)findViewById(R.id.macTXT);

        String[] ars={"","-u",u.getText().toString(),"-p",p.getText().toString(),"-m",m.getText().toString()};

        try{
            t.setText("");
            SuperSify ss = new SuperSify(ars,t);
            ss.start();
        }
        catch (Exception e){
            System.out.println("##############\n\n\n\n");
            e.printStackTrace();
        }
    }

    public void logoff(View view){
        TextView t = (TextView)findViewById(R.id.resultText);

        String[] ars={"","-l"};
        try{
            t.setText("");
            SuperSify ss = new SuperSify(ars,t);
            ss.start();
        }
        catch (Exception e){
            System.out.println("##############\n\n\n\n");
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

    }
}
