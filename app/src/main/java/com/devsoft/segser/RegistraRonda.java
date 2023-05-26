package com.devsoft.segser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import id.ionbit.ionalert.IonAlert;

public class RegistraRonda extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
    private PendingIntent pendingIntent;
    boolean writeMode;

    Tag myTag;
    IntentFilter writeTagFilters[];
    Button btnenviar,btnnuevo;
    RadioButton rbninicio,rbncontrol,rbntermino,rbncumple,rbnnocumple;
    TextInputEditText txtobservacion,txtpuntocontrol;
    Spinner spinner;
    RequestQueue request;
    String token="";
    TextView txtnfc;

    String path, nombrefoto;
    final int COD_SELECCIONA = 10;
    File fileImagen;
    Bitmap bitmap;
    ImageView imgfoto;
    String latitud,longitud;

    final int COD_FOTO = 20;

    private final String CARPETA_RAIZ = "SEGSER/";
    private final String RUTA_IMAGEN = CARPETA_RAIZ + "Rondin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registra_ronda);
        btnenviar=findViewById(R.id.btnenviar);
        btnnuevo=findViewById(R.id.btnnuevo);
        rbninicio=findViewById(R.id.rbninicio);
        rbncontrol=findViewById(R.id.rbncontrol);
        rbntermino=findViewById(R.id.rbntermino);
        rbncumple=findViewById(R.id.rbncumple);
        rbnnocumple=findViewById(R.id.rbnnocumple);
        txtobservacion=findViewById(R.id.txtobservacion);
        txtpuntocontrol=findViewById(R.id.txtpuntocontrol);
        imgfoto=findViewById(R.id.imgfoto);
        txtnfc=findViewById(R.id.txtnfc);
        spinner=findViewById(R.id.spinner);
        request=Volley.newRequestQueue(RegistraRonda.this);
        SharedPreferences preferences=getSharedPreferences("SEGSER",MODE_PRIVATE);
        token= preferences.getString("token","Invitado");
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);


        btnnuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                New();
            }
        });
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }
        if(!token.equals("Invitado")){
            JWT jwt=new JWT(token);
            boolean isExpired=jwt.isExpired(10);
            if(isExpired){
                Intent midintent = new Intent(RegistraRonda.this, MainActivity.class);
                Bundle mibundle = new Bundle();


                mibundle.putString("token",token);


                midintent.putExtras(mibundle);
                startActivity(midintent);
                super.finish();
            }
        }

        cargartiposcontrol();


        imgfoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cargarcamara();
            }
        });

        if (mNfcAdapter == null) {
            Toast.makeText(this, "ESTE DISPOSITIVO NO SOPORTA NFC", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "PORFAVOR ACTIVE SU NFC", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
        readFromIntent(getIntent());
     /*   final int flag =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }else {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};


        txtnfc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String etiqueta=txtnfc.getText().toString();
                consultaretiqueta(etiqueta);

            }
        });

        btnenviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(txtnfc.getText().toString().isEmpty()){
                    Toast.makeText(RegistraRonda.this,"Debe escanear una tarjeta",Toast.LENGTH_LONG).show();
                }else{
                    String observacion=txtobservacion.getText().toString();
                    String tipo="INICIO";
                    if(rbncontrol.isChecked())tipo="CONTROL";
                    if(rbntermino.isChecked())tipo="TERMINO";
                    String[]asd=spinner.getSelectedItem().toString().split("-");
                    String tipoguardia=asd[0];
                    String isok="1";
                    if(rbnnocumple.isChecked())isok="0";

                    enviardatos(observacion,tipo,tipoguardia,isok);




                }
            }
        });

    }
    private void locationStart() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion Local = new Localizacion();
        Local.setMainActivity(this);
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  // Abre pantalla para activar GPS cuando esta apagado
            startActivity(settingsIntent);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) Local);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) Local);

    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationStart();
                return;
            }
        }
    }


    public class Localizacion implements LocationListener {
        RegistraRonda mainActivity;
        public RegistraRonda getMainActivity() {
            return mainActivity;
        }
        public void setMainActivity(RegistraRonda mainActivity) {
            this.mainActivity = mainActivity;
        }
        @Override
        public void onLocationChanged(Location loc) {
            // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
            // debido a la deteccion de un cambio de ubicacion
            loc.getLatitude();
            loc.getLongitude();
            String sLatitud = String.valueOf(loc.getLatitude());
            String sLongitud = String.valueOf(loc.getLongitude());

            //Toast.makeText(getApplicationContext(),"latitud"+sLatitud,Toast.LENGTH_LONG).show();
            latitud=sLatitud;
            longitud=sLongitud;
            //llamamos a la clase para obtener la direccion
            //this.mainActivity.setLocation(loc);
        }
        @Override
        public void onProviderDisabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es desactivado
            Toast.makeText(getApplicationContext(),"GPS DESACTIVADO",Toast.LENGTH_LONG).show();
        }
        @Override
        public void onProviderEnabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es activado
            Toast.makeText(getApplicationContext(),"GPS ACTIVO",Toast.LENGTH_LONG).show();
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }
    private void enviardatos(String observacion, String tipo, String tipoguardia, String isok) {

        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(RegistraRonda.this,
                BeautifulProgressDialog.withImage,
                "Enviando foto Espere porfavor");
        progressDialog.show();

        String url=Global.urlgeneral+"wsenviafoto.php";

      // String url= Global.url+"guards/save";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                if (response.trim().equalsIgnoreCase("registra")){
                        registrarguardia(observacion,tipo,tipoguardia,isok);
                }

               // respuestaetiquetanfc(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                new IonAlert(RegistraRonda.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("ERROR EN SERVICIO DE TIPOS DE GUARDIA \n"+error.getMessage())
                        .show();


            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> parametros=new HashMap<>();
              //  parametros.put("json",jsonObject.toString());
                parametros.put("nombre",nombrefoto);
                parametros.put("imagen",convertirImgString(bitmap));

                return parametros;
            }


        };


        request.add(jsonObjectRequest);





    }

    private void registrarguardia(String observacion, String tipo, String tipoguardia, String isok) {
        String[]ada=txtpuntocontrol.getText().toString().split("-");
        String idpoint=ada[0];
        SimpleDateFormat ho=new SimpleDateFormat("HH:mm:ss");
        String hora=ho.format(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //dd/MM/yyyy HH:mm:ss
        String fecha = sdf.format(new Date());
        JWT jwt=new JWT(token);
        String usuario=jwt.getClaim("IDUSER").asString();
        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("IDUSER",usuario);
            jsonObject.put("IDTYPEGUAR",tipoguardia);
            jsonObject.put("DATE",fecha);
            jsonObject.put("TIME",hora);
            jsonObject.put("IDPOINT",idpoint);
            jsonObject.put("LAT",latitud);
            jsonObject.put("LONG",longitud);
            jsonObject.put("OBSERVATION",observacion);
            jsonObject.put("ISOK",isok);
            jsonObject.put("IDIMAGE",nombrefoto);
            jsonObject.put("TYPEPOINT",tipo);

        }catch (Exception e){

        }


        System.out.println(jsonObject);
        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(RegistraRonda.this,
                BeautifulProgressDialog.withImage,
                "Registrando Espere porfavor");
        progressDialog.show();


         String url= Global.url+"guards/save";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                System.out.println(response);

                 respuestainserta(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                new IonAlert(RegistraRonda.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("ERROR EN SERVICIO DE TIPOS DE GUARDIA \n"+error.getMessage())
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
            @Override
            public Map<String, String>getHeaders() throws AuthFailureError{
                HashMap headers = new HashMap();
                headers.put("Content-Type","application/x-www-form-urlencoded");
                headers.put("Auth",token);

                return headers;
            }

        };


        request.add(jsonObjectRequest);
    }

    private void respuestainserta(String response) {

        try{
            JSONObject json=new JSONObject(response);
            if(json.getString("code").equals("200")){
                new IonAlert(RegistraRonda.this,IonAlert.SUCCESS_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("RONDA REGISTRADA EXITOSAMENTE")
                        .show();
            }
            New();
        }catch (Exception e){
            System.out.println("error"+e.getMessage());
        }
    }

    private void cargarcamara() {
        File miFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), RUTA_IMAGEN);
        boolean isCreada=miFile.exists();

        if(isCreada==false){
            isCreada=miFile.mkdirs();
        }

        if(isCreada==true){
            SimpleDateFormat ho=new SimpleDateFormat("HH:mm:ss");
            String hora=ho.format(new Date());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //dd/MM/yyyy HH:mm:ss
            String fecha = sdf.format(new Date());
            String fechacorrecta=fecha.replaceAll("/","");
            nombrefoto=fechacorrecta+hora.toString().replaceAll(":","");



            path=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+File.separator+RUTA_IMAGEN
                    +File.separator+nombrefoto+".jpg";//indicamos la ruta de almacenamiento



            fileImagen=new File(path);

            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileImagen));

            ////
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
            {
                String authorities=this.getPackageName()+".provider";
                Uri imageUri= FileProvider.getUriForFile(this,authorities,fileImagen);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            }else
            {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileImagen));
            }
            startActivityForResult(intent,COD_FOTO);

            ////

        }


    }
    private void consultaretiqueta(String etiqueta) {

        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("IDCOMPANY","1");
            jsonObject.put("CODE",etiqueta);
        }catch (Exception e){

        }
        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(RegistraRonda.this,
                BeautifulProgressDialog.withImage,
                "Espere porfavor");
        progressDialog.show();
        String url= Global.url+"point/searchbycode";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();

                respuestaetiquetanfc(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                new IonAlert(RegistraRonda.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("ERROR EN SERVICIO DE TIPOS DE GUARDIA \n"+error.getMessage())
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
            @Override
            public Map<String, String>getHeaders() throws AuthFailureError{
                HashMap headers = new HashMap();
                headers.put("Content-Type","application/x-www-form-urlencoded");
                headers.put("Auth",token);

                return headers;
            }

        };


        request.add(jsonObjectRequest);



    }

    private void respuestaetiquetanfc(String response) {

        ArrayList<String>listatipos=new ArrayList<String>();
        try{
            JSONObject json=new JSONObject(response);
            if(json.getString("code").equals("200")){
                JSONArray jsonArray=new JSONArray(json.getString("data"));


                    String id=jsonArray.getJSONObject(0).getString("ID");
                    String name=jsonArray.getJSONObject(0).getString("NAME");
                    listatipos.add(id+"-"+name);
                    txtpuntocontrol.setText(id+"-"+name);


            }
        }catch (Exception e){
            System.out.println("error"+e.getMessage());
        }
    }

    private void cargartiposcontrol() {
        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("IDCOMPANY","1");
        }catch (Exception e){

        }
        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(RegistraRonda.this,
                BeautifulProgressDialog.withImage,
                "Espere porfavor");
        progressDialog.show();
        String url= Global.url+"typeguard/gettypeguardsbycompany";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();

                respuestatipoguardia(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                new IonAlert(RegistraRonda.this,IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("ERROR EN SERVICIO DE TIPOS DE GUARDIA \n"+error.getMessage())
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
            @Override
            public Map<String, String>getHeaders() throws AuthFailureError{
                HashMap headers = new HashMap();
                headers.put("Content-Type","application/x-www-form-urlencoded");
                headers.put("Auth",token);

                return headers;
            }

        };


        request.add(jsonObjectRequest);


    }

    private void respuestatipoguardia(String response) {
        ArrayList<String>listatipos=new ArrayList<String>();
        try{
            JSONObject json=new JSONObject(response);
            if(json.getString("code").equals("200")){
                JSONArray jsonArray=new JSONArray(json.getString("data"));

                for(int i=0;i<jsonArray.length();i++){
                    String id=jsonArray.getJSONObject(i).getString("ID");
                    String name=jsonArray.getJSONObject(i).getString("NAME");
                    listatipos.add(id+"-"+name);
                    System.out.println(listatipos);
                }
                ArrayAdapter<String>adapter=new ArrayAdapter<>(RegistraRonda.this, android.R.layout.simple_list_item_1,listatipos);
                spinner.setAdapter(adapter);
            }
        }catch (Exception e){
            System.out.println("error"+e.getMessage());
        }

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            txtnfc.setText(ByteArrayToHexString(tag.getId()));

        }
    }



    String ByteArrayToHexString(byte [] inarray)
    {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void  readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;
        String text = "";
        String text2 = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("Error ", e.toString());
        }
        txtnfc.setText(text);
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            WriteModeOff();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            WriteModeOn();
        }
    }

    /******************************************************************************
     **********************************Habilita escritura********************************
     ******************************************************************************/
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private void WriteModeOn() {
        writeMode = true;
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    /******************************************************************************
     **********************************Deshabilita escritura*******************************
     ******************************************************************************/
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private void WriteModeOff() {
        writeMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == COD_FOTO){
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("Path",""+path);
                        }
                    });

            bitmap= BitmapFactory.decodeFile(path);
            imgfoto.setImageBitmap(bitmap);
            bitmap=redimensionarImagen(bitmap,320,400);
        }

    }
    private String  convertirImgString(Bitmap bitmap) {

        ByteArrayOutputStream array=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,array);
        byte[] imagenByte=array.toByteArray();
        String imagenString= Base64.encodeToString(imagenByte, Base64.DEFAULT);

        return imagenString;
    }
    private Bitmap redimensionarImagen(Bitmap bitmap, float anchoNuevo, float altoNuevo) {

        int ancho=bitmap.getWidth();
        int alto=bitmap.getHeight();

        if(ancho>anchoNuevo || alto>altoNuevo){
            float escalaAncho=anchoNuevo/ancho;
            float escalaAlto= altoNuevo/alto;

            Matrix matrix=new Matrix();
            matrix.postScale(escalaAncho,escalaAlto);

            return Bitmap.createBitmap(bitmap,0,0,ancho,alto,matrix,false);

        }else{
            return bitmap;
        }


    }

    public void New(){
        txtnfc.setText("");
        txtpuntocontrol.setText("");
        txtobservacion.setText("");
    }
}