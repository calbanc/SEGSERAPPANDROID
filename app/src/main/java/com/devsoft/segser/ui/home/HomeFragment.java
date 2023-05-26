package com.devsoft.segser.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devsoft.segser.MainActivity;
import com.devsoft.segser.RegistraRonda;
import com.devsoft.segser.databinding.FragmentHomeBinding;

import id.ionbit.ionalert.IonAlert;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        final Button btnsalir=binding.btnsalir;
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(getContext(), RegistraRonda.class);
                startActivity(intent);
            }
        });

        btnsalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences preferences=getActivity(). getSharedPreferences("SEGSER",MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                Intent intent =new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                /* new IonAlert(getContext(),IonAlert.WARNING_TYPE)
                        .setTitleText("SEGSER SEGURIDAD")
                        .setContentText("Desea Cerrar Sesion")
                        .setConfirmText("CERRAR SESION")
                       .seCancelText("CANCELAR")
                        .setCancelClickListener(new IonAlert.ClickListener() {
                            @Override
                            public void onClick(IonAlert ionAlert) {
                                ionAlert.dismiss();
                            }
                        })

                        .setConfirmClickListener(new IonAlert.ClickListener() {
                            @Override
                            public void onClick(IonAlert ionAlert) {

                                SharedPreferences preferences=getActivity(). getSharedPreferences("SEGSER",MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();

                                editor.remove("token");

                                Intent intent =new Intent(getActivity(), MainActivity.class);
                                startActivity(intent);

                            }
                        })
                        .show();*/

            }
        });

        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}