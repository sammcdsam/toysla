package toysla.com;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//PANTALLA REMOTE CONTROL
public class ActivityRC extends AppCompatActivity {

    ImageButton playmode;
    ImageButton geosettings;
    ImageButton exit;
    ImageButton stopbtn;
    Switch sw;
    Button startbtn,savebtn, playbtn;
    RadioButton softbtn, normalbtn;
    RadioGroup radiogroup;
    //Parte de remotote contros

    ImageButton leftbtn, upbtn, rightbtn, downbtn;
    TextView IdBufferIn;

    //-----------------------------------------------------------------------------------------
    Handler bluetoothIn;
    final int handlerState =0;
    private BluetoothAdapter btAdapter = null ;
    private BluetoothSocket btSocket = null ;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = null ;

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remotecontrol2);

        //-----------------------------------------------------------------------------------------
        //Play mode
        radiogroup= (RadioGroup) findViewById(R.id.radiogroup);
        playbtn = (Button) findViewById(R.id.playbtn);



        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected
                if(checkedId == R.id.softbtn) {
                    Toast.makeText(getApplicationContext(), "Soft mode",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Normal mode",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

        softbtn= (RadioButton) findViewById(R.id.softbtn);
        normalbtn = (RadioButton) findViewById(R.id.normalbtn);

        playbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int selectedId = radiogroup.getCheckedRadioButtonId();

                // find which radioButton is checked by id
                if(selectedId == normalbtn.getId()) {
                    MyConexionBT.escribir("N");
                } else {
                    MyConexionBT.escribir("O");
                }
            }
        });
        //GEO boton

        sw = (Switch) findViewById(R.id.gpssw);
        startbtn= (Button)findViewById(R.id.startbtn);
        savebtn= (Button)findViewById(R.id.savebtn);

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startbtn.setVisibility(View.VISIBLE);
                    savebtn.setVisibility(View.VISIBLE);

                }
            }
        });




        //-----------------------------------------------------------------------------------------
        //Enlazar botones bt

        downbtn = (ImageButton) findViewById(R.id.downbtn);
        upbtn = (ImageButton) findViewById(R.id.upbtn);
        leftbtn = (ImageButton) findViewById(R.id.leftbtn);
        rightbtn = (ImageButton) findViewById(R.id.rightbtn);
        IdBufferIn= (TextView)findViewById(R.id.IdBufferIn) ;
        stopbtn = (ImageButton) findViewById(R.id.stopbtn);


        //-----------------------------------------------------------------------------------------
        //Presentar los mensajes que nos envia el arduino
        // min 20.40
        bluetoothIn= new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what== handlerState)
                {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex= DataStringIN.indexOf("#");

                    if(endOfLineIndex>0)
                    {
                        String dataInPrint = DataStringIN.substring(0,endOfLineIndex);
                        IdBufferIn.setText("Value: "+ dataInPrint);
                        DataStringIN.delete(0,DataStringIN.length());
                    }
                }
            }
        };

        //-----------------------------------------------------------------------------------------

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();
        //-----------------------------------------------------------------------------------------
        //CONFIGUARION ONCLICK LISTENERS

        downbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("B");
            }
        });

        upbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("F");
            }
        });

        leftbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("L");

            }
        });

        rightbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("R");
            }
        });

        stopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("S");
            }
        });

        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("T");
            }
        });

        savebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.escribir("V");
            }
        });
        exit= (ImageButton)findViewById(R.id.exitbtn);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btSocket!=null)
                {
                    try{btSocket.close();}
                    catch (IOException e)
                    {
                        Toast.makeText(getBaseContext(),"Error", Toast.LENGTH_SHORT).show();
                    };

                }
                finish();

            }
        });

    }
    //----------------------------------------------------------------------------------------------
    //Conexion segura

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        Toast.makeText(getBaseContext(), "conexion segura ", Toast.LENGTH_LONG).show();
        //Ccrea una conexion segura de salida usando UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);


    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent= getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via extra
        address=intent.getStringExtra(ActivityBT.EXTRA_DEVICE_ADDRESS);
        //setea la dir mac
        BluetoothDevice device= btAdapter.getRemoteDevice(address);

        Toast.makeText(getBaseContext(), "q pasa aqui ", Toast.LENGTH_LONG).show();
        try{
            btSocket= createBluetoothSocket(device);
            Toast.makeText(getBaseContext(), "Socket creado", Toast.LENGTH_LONG).show();
        }catch(IOException e) {
            Toast.makeText(getBaseContext(), "Socket failure", Toast.LENGTH_LONG).show();
        }

        //Establece conexion con el socket
        try
        {

            btSocket.connect();
            Toast.makeText(getBaseContext(), "Socket conectado", Toast.LENGTH_LONG).show();
        }catch (IOException e)
        {
            try
            {
                btSocket.close();
            }catch (IOException e2){}
        }
        MyConexionBT= new ConnectedThread(btSocket);
        MyConexionBT.start();
        Toast.makeText(getBaseContext(), "empieza la conexion ", Toast.LENGTH_LONG).show();
    };

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        }catch (IOException e2){}
    };

    //---------------------------------------------------------------------------------------------

    //Comprueba si el dispositivo BT esta disponible si no solicita que se active
    private void VerificarEstadoBT()
    {
        if(btAdapter==null)
        {
            Toast.makeText(getBaseContext(),"The device no soporta BT", Toast.LENGTH_LONG).show();

        }else
        {

            Intent enableBtIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
        }
    };

    //---------------------------------------------------------------------------------------------
    //Clase que permite el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn= null ;
            OutputStream tmpOut= null;
            try{
                tmpIn=socket.getInputStream();
                tmpOut=socket.getOutputStream();
                Toast.makeText(getBaseContext(), "input creado ", Toast.LENGTH_LONG).show();

            }catch (IOException e){}

            mmInStream= tmpIn;
            mmOutStream=tmpOut;
        }
        public void run()
        {
            byte[] buffer= new byte[256];
            int bytes;

            while(true)
            {
                try
                {
                    bytes = mmInStream.read(buffer);
                    String readMessage= new String(buffer, 0, bytes);
                    //envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();

                }catch (IOException e)
                {
                    break;
                }
            }
        }
        //envio la trama

        public void escribir(String input)
        {

            if(btSocket!=null)
            {
                try{
                    btSocket.getOutputStream().write(input.toString().getBytes());
                } catch (IOException e)
                {
                    Toast.makeText(getBaseContext(),"Conexion failed", Toast.LENGTH_LONG).show();
                    //finish();
                }
            }


            try{
                mmOutStream.write(input.getBytes());

            }catch (IOException e)
            {
                Toast.makeText(getBaseContext(),"Conexion failed", Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }



}

