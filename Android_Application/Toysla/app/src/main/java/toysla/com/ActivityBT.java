package toysla.com;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import java.util.Set;

//PANTALLA BT DONDE SALEN LOS DISPOSITIVOS VINCULADOS

public class ActivityBT extends AppCompatActivity {

    //---------------------------------------------------------------------------------------------
    //CONFIGURACION INICIAL

    //1.Depuracion de LOGCAT
    private static final String TAG ="ActivityBT";

    ListView IdList;

    //String que se enviara a la actividad principal, remotecontrol2
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //Declaracion de campos BT
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    //----------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);
    }

    //---------------------------------------------------------------------------------------------
    //Metodo onResume que se ejecutara al principio

    @Override
    public void onResume()
    {
        super.onResume();
        //-----------------------
        VerificarEstadoBT();

        //Inicializa el array que tendra la lista de los disp vinculados
        mPairedDevicesArrayAdapter= new ArrayAdapter<String>(this,R.layout.name_devices);

        //Dispositivos vinculado en el ListView
        IdList= (ListView) findViewById(R.id.Idlist);
        IdList.setAdapter(mPairedDevicesArrayAdapter);
        //-!!!!!!!!!!!!!!!!!!!
        IdList.setOnItemClickListener(mDeviceClickListener);

        //obtiene el adaptador local bt
        mBtAdapter= BluetoothAdapter.getDefaultAdapter();
        //obtiene los disp emparejados y agrega a pariedDevice

        if(mBtAdapter.getBondedDevices()!=null)
        {
            Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
            //añade el dispt previo emparejado al array
            if(pairedDevices.size()>0)
            {
                for(BluetoothDevice device: pairedDevices)
                {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }


    }
    //----------------------------------------------------------------------------------------------
    //Configuracion on-click para la lista

    private AdapterView.OnItemClickListener mDeviceClickListener= new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView av,View v, int arg2, long arg3 ) {

            //Obtengo direccion MAC (17 ultimos char)
            String info= ((TextView)v).getText().toString();
            String address= info.substring(info.length() - 17);

            //Realiza un intent para inciiar la siguiente actividad
            //mientras toma un Extra device address para la dir MAC
            Intent i = new Intent(ActivityBT.this, ActivityRC.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };


    //----------------------------------------------------------------------------------------------
    //Comprueba que el dispositvo tiene bt y esta encendido

    private void VerificarEstadoBT()
    {
        mBtAdapter= BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null)
        {
            Toast.makeText(getBaseContext(),"The device does not have Bluetooth", Toast.LENGTH_LONG).show();

        }
        else
        {
            if(mBtAdapter.isEnabled())
            {
                Log.d(TAG,"...Bluetooth activated... ");
            }
            else
            {
                //Solicita al usuario que active el bt
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent,1);
            }
        }

    }

}
