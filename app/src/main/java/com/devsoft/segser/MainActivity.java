package com.devsoft.segser;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.auth0.android.jwt.JWT;
import com.basusingh.beautifulprogressdialog.BeautifulProgressDialog;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import id.ionbit.ionalert.IonAlert;

public class MainActivity extends AppCompatActivity {
    TextInputEditText txtusuario,txtclave;
    Button btningresar;

    RequestQueue request;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtusuario=findViewById(R.id.txtusuario);
        txtclave=findViewById(R.id.txtclave);
        btningresar=findViewById(R.id.btningresar);

        request= Volley.newRequestQueue(MainActivity.this);
        if(validaPermisos()){
            btningresar.setEnabled(true);
        }else{
            btningresar.setEnabled(false);
        }

        SharedPreferences preferences=getSharedPreferences("SEGSER",MODE_PRIVATE);
        String token= preferences.getString("token","Invitado");
        System.out.println(token);
        if(!token.equals("Invitado")){
            JWT jwt=new JWT(token);
            boolean isExpired=jwt.isExpired(10);
            if(!isExpired){
                Intent midintent = new Intent(MainActivity.this, MenuPrincipalActivity.class);
                Bundle mibundle = new Bundle();


                mibundle.putString("token",token);


                midintent.putExtras(mibundle);
                startActivity(midintent);
                super.finish();
            }

        }else{

        }



        btningresar.setOnClickListener(view -> {
            if(txtusuario.getText().toString().isEmpty()||txtclave.getText().toString().isEmpty()){
                new IonAlert(MainActivity.this, IonAlert.ERROR_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("DEBE INGRESAR USUARIO Y CLAVE")
                        .show();
            }else{
                String usuario=txtusuario.getText().toString().trim().replaceAll(" ","%20");
                String clave=txtclave.getText().toString().trim().replaceAll(" ","%20");
                login(usuario,clave);
            }
        });
    }
    private void login(String usuario, String clave) {
        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("USERNAME",usuario);
            jsonObject.put("PASSWORD",clave);
        }catch (Exception e){

        }


        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(MainActivity.this,
                BeautifulProgressDialog.withImage,
                "Espere porfavor");
        progressDialog.show();
        String url= Global.url+"login";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();

                respuesta(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                new IonAlert(MainActivity.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("ERROR EN SERVICIO DE LOGIN \n"+error.getMessage())
                        .show();


            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> parametros=new HashMap<>();
                parametros.put("json",jsonObject.toString());

                return parametros;
            }
         /*   @Override
            public Map<String, String>getHeaders() throws AuthFailureError{
                HashMap headers = new HashMap();
                headers.put("Content-Type","application/x-www-form-urlencoded");
                return headers;
            }*/

        };


        request.add(jsonObjectRequest);


    }
    private void respuesta(String response) {

        try {
            JSONObject json=new JSONObject(response);

            if(json.getString("status").equals("ok")){
                String tokens=json.getString("token");
                SharedPreferences preferences=getSharedPreferences("SEGSER",MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("token", tokens);
                editor.commit();
                Intent midintent = new Intent(MainActivity.this, MenuPrincipalActivity.class);
                Bundle mibundle = new Bundle();


                mibundle.putString("token",tokens);


                midintent.putExtras(mibundle);
                startActivity(midintent);
                super.finish();
            }else{
                new IonAlert(MainActivity.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("USUARIO O CONTRASEÑA INCORRECTOS")
                        .show();
            }




        }catch (Exception e){
            System.out.println("error "+e.getMessage());
        }

    }

    private boolean validaPermisos() {

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            return true;
        }

        if((checkSelfPermission(CAMERA)== PackageManager.PERMISSION_GRANTED)&&
                (checkSelfPermission(WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)&& (checkSelfPermission(ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)){
            return true;
        }

        if((shouldShowRequestPermissionRationale(CAMERA)) ||
                (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))||(shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION))){
            cargarDialogoRecomendacion();
        }else{
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA,ACCESS_FINE_LOCATION},100);
        }

        return false;
    }
    private void cargarDialogoRecomendacion() {
        AlertDialog.Builder dialogo=new AlertDialog.Builder(MainActivity.this);
        dialogo.setTitle("Permisos Desactivados");
        dialogo.setMessage("Debe aceptar los permisos para el correcto funcionamiento de la App");

        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA,ACCESS_FINE_LOCATION},100);
            }
        });
        dialogo.show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==100){
            if(grantResults.length==3 && grantResults[0]== PackageManager.PERMISSION_GRANTED
                    && grantResults[1]==PackageManager.PERMISSION_GRANTED && grantResults[2]==PackageManager.PERMISSION_GRANTED){
                btningresar.setEnabled(true);
            }else{
                solicitarPermisosManual();
            }
        }

    }
    private void solicitarPermisosManual() {
        final CharSequence[] opciones={"si","no"};
        final AlertDialog.Builder alertOpciones=new AlertDialog.Builder(MainActivity.this);
        alertOpciones.setTitle("¿Desea configurar los permisos de forma manual?");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("si")){
                    Intent intent=new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri=Uri.fromParts("package",getPackageName(),null);
                    intent.setData(uri);
                    startActivity(intent);
                }else{
                    Toast.makeText(getApplicationContext(),"Los permisos no fueron aceptados", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            }
        });
        alertOpciones.show();
    }
}