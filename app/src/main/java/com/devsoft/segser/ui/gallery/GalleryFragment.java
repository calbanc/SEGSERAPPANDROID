package com.devsoft.segser.ui.gallery;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.auth0.android.jwt.JWT;
import com.basusingh.beautifulprogressdialog.BeautifulProgressDialog;
import com.devsoft.segser.Global;
import com.devsoft.segser.MainActivity;
import com.devsoft.segser.MenuPrincipalActivity;
import com.devsoft.segser.RegistraRonda;
import com.devsoft.segser.databinding.FragmentGalleryBinding;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

import id.ionbit.ionalert.IonAlert;

public class GalleryFragment extends Fragment {
    String token="";
    String usuario="";
    RequestQueue request;
    Spinner spcliente,spzon;

    private FragmentGalleryBinding binding;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);


        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        spcliente=binding.spcliente;
        spzon=binding.spinner4;
        final TextInputEditText txtrutvisita=binding.txtrutvisita;
        final TextInputEditText txtnombre=binding.txtnombre;
        final TextInputEditText txtmotivo=binding.txtmotivo;
        final TextInputEditText txtautorizado=binding.txtautorizado;
        final Button btnregistrar=binding.btnregistrar;
        request= Volley.newRequestQueue(getContext());
        SharedPreferences preferences=getActivity().getSharedPreferences("SEGSER",MODE_PRIVATE);
        token= preferences.getString("token","Invitado");
        if(!token.equals("Invitado")){
            JWT jwt=new JWT(token);
            boolean isExpired=jwt.isExpired(10);
             usuario= String.valueOf(jwt.getClaim("IDUSER"));
            if(!isExpired){
                Intent midintent = new Intent(getActivity(), MenuPrincipalActivity.class);
                Bundle mibundle = new Bundle();


                mibundle.putString("token",token);


                midintent.putExtras(mibundle);
             //   startActivity(midintent);

            }

        }

        cargarclientes();

     spcliente.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println(spcliente.getSelectedItem().toString());
             String[]ada=spcliente.getSelectedItem().toString().split("-");
             String idclient=ada[0];
             getzonbyclient(idclient);
         }

         @Override
         public void onNothingSelected(AdapterView<?> adapterView) {

         }
     });


        btnregistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(spcliente.getSelectedItem().toString().isEmpty()|| txtautorizado.getText().toString().isEmpty()||spzon.getSelectedItem().toString().isEmpty()
                ||txtrutvisita.getText().toString().isEmpty()||txtnombre.getText().toString().isEmpty()||txtmotivo.getText().toString().isEmpty()){
                    Toast.makeText(getContext(),"FALTAN DATOS",Toast.LENGTH_LONG).show();
                }else{
                    String cliente=spcliente.getSelectedItem().toString();
                    String zona=spzon.getSelectedItem().toString();
                    String rutvisista=txtrutvisita.getText().toString();
                    String nombre=txtnombre.getText().toString();
                    String motivo=txtmotivo.getText().toString();
                    String autorizado=txtautorizado.getText().toString();

                    registrar(cliente,zona,rutvisista,nombre,motivo,autorizado);
                }
            }
        });



        return root;
    }

    private void getzonbyclient(String idclient) {
        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("IDCLIENT",idclient);
        }catch (Exception e){

        }

        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(getActivity(),
                BeautifulProgressDialog.withImage,
                "Espere porfavor");
        progressDialog.show();
        String url= Global.url+"zone/getzonebyclient";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                respuestazona(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                new IonAlert(getContext(),IonAlert.WARNING_TYPE)
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

    private void respuestazona(String response) {
        ArrayList<String> listatipos=new ArrayList<String>();
        try{
            JSONObject json=new JSONObject(response);
            if(json.getString("code").equals("200")){
                JSONArray jsonArray=new JSONArray(json.getString("data"));

                for(int i=0;i<jsonArray.length();i++){
                    String id=jsonArray.getJSONObject(i).getString("ID");
                    String name=jsonArray.getJSONObject(i).getString("NAME");
                    listatipos.add(id+"-"+name);

                }
                ArrayAdapter<String>adapter=new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,listatipos);
                spzon .setAdapter(adapter);
            }
        }catch (Exception e){
            System.out.println("error"+e.getMessage());
        }
    }

    private void cargarclientes() {
        JSONObject jsonObject=new JSONObject();
        try{
            jsonObject.put("IDCOMPANY","1");
        }catch (Exception e){

        }

        BeautifulProgressDialog progressDialog = new BeautifulProgressDialog(getActivity(),
                BeautifulProgressDialog.withImage,
                "Espere porfavor");
        progressDialog.show();
        String url= Global.url+"clients/searchbycompany";
        StringRequest jsonObjectRequest=new StringRequest(
                Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                respuestaclientes(response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                new IonAlert(getContext(),IonAlert.WARNING_TYPE)
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

    private void respuestaclientes(String response) {
        ArrayList<String> listatipos=new ArrayList<String>();
        try{
            JSONObject json=new JSONObject(response);
            if(json.getString("code").equals("200")){
                JSONArray jsonArray=new JSONArray(json.getString("data"));

                for(int i=0;i<jsonArray.length();i++){
                    String id=jsonArray.getJSONObject(i).getString("ID");
                    String name=jsonArray.getJSONObject(i).getString("NAME");
                    listatipos.add(id+"-"+name);

                }
                ArrayAdapter<String>adapter=new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,listatipos);
                spcliente.setAdapter(adapter);
            }
        }catch (Exception e){
            System.out.println("error"+e.getMessage());
        }

    }

    private void registrar(String cliente, String zona, String rutvisista, String nombre, String motivo, String autorizado) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}