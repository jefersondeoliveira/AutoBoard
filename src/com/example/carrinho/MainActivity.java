package com.example.carrinho;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorListener {
	// Solicitação de códigos de Intenção
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final String TAG = "SensorDemo";
	private SensorManager sensorManager;
	private TextView outView;
	private int sensor = SensorManager.SENSOR_ORIENTATION;
	private Button ligar;
	private Button desligar;
	private Button esquerda;
	private Button direita;
	private Button bluetoothOff;
	private Button bluetoothOn;
	private Button lightOff;
	private Button lightOn;
	private Button buzz;
	private boolean connectStat = false;
	protected static final int MOVE_TIME = 80;
	private AlertDialog aboutAlert;
	private View aboutView;
	private View controlView;
	OnClickListener myClickListener;
	ProgressDialog myProgressDialog;
	private Toast failToast;
	private Handler mHandler;
	// Objetos necessários do Bluetooth
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	private ConnectThread mConnectThread = null;
	private String deviceAddress = null;
	// SPP UUID (mapa para o canal RFCOMM 1 (padrão) se não está em uso);
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ligar = (Button) findViewById(R.botao.on);
		desligar = (Button) findViewById(R.botao.off);
		esquerda = (Button) findViewById(R.botao.left);
		direita = (Button) findViewById(R.botao.right);
		outView = (TextView) findViewById(R.id.output);
		bluetoothOff = (Button) findViewById(R.botao.bluetoothOff);
		bluetoothOn = (Button) findViewById(R.botao.bluetoothOn);
		lightOff = (Button) findViewById(R.botao.lightOff);
		lightOn = (Button) findViewById(R.botao.lightOn);
		buzz = (Button) findViewById(R.botao.buzz);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		myProgressDialog = new ProgressDialog(this);
		failToast = Toast.makeText(this, R.string.falha_ao_conectar,
				Toast.LENGTH_SHORT);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (myProgressDialog.isShowing()) {
					myProgressDialog.dismiss();
				}

				// Verifique se a conexão Bluetooth foi feito
				// para o dispositivo selecionado
				if (msg.what == 1) {
					// Definir botão para exibir o status atual
					connectStat = true;
				} else {
					// falha na conexão
					failToast.show();
				}
			}
		};

		// Verifique se existe adaptador bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.bt_nao_disponivel, Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}

		// Se a BT não estiver ligado, solicitar que seja ativado.
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		// Conectar para o Módulo Bluetooth
		bluetoothOn.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (connectStat) {
					// Tente desligar o dispositivo
					disconnect();
				} else {
					// Tentar se conectar ao dispositivo
					connect();
				}
			}
		});

		bluetoothOff.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (connectStat) {
					// Tente desligar o dispositivo
					disconnect();
				} else {
					// Tentar se conectar ao dispositivo
					connect();
				}
			}
		});

		lightOn.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				lightOff.setVisibility(Button.VISIBLE);
				lightOn.setVisibility(Button.GONE);
				if (outStream != null) {
					// Captura do Texto
					final String conteudo = "f";
					// final String conteudo = mensagem.getText().toString();
					// Transferidor de Dados
					Thread sender = new Thread() {
						public void run() {
							// Conteúdo para Envio
							byte content[] = conteudo.getBytes();
							try { // Possibilidade de Erro
								outStream.write(content.length); // Tamanho do
																	// Conteúdo
								outStream.write(content); // Conteúdo
															// Propriamente Dito

							} catch (IOException e) { // Erro Encontrado

							}
						}
					};
					// Executando o Fluxo de Processamento
					sender.start(); // Inicialização
				}

			}
		});

		lightOff.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				lightOn.setVisibility(Button.VISIBLE);
				lightOff.setVisibility(Button.GONE);
				if (outStream != null) {
					// Captura do Texto
					final String conteudo = "e";
					// final String conteudo = mensagem.getText().toString();
					// Transferidor de Dados
					Thread sender = new Thread() {
						public void run() {
							// Conteúdo para Envio
							byte content[] = conteudo.getBytes();
							try { // Possibilidade de Erro
								outStream.write(content.length); // Tamanho do
																	// Conteúdo
								outStream.write(content); // Conteúdo
															// Propriamente Dito

							} catch (IOException e) { // Erro Encontrado

							}
						}
					};
					// Executando o Fluxo de Processamento
					sender.start(); // Inicialização
				}
			}
		});

		buzz.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (outStream != null) {
					// Captura do Texto
					final String conteudo = "g";
					// final String conteudo = mensagem.getText().toString();
					// Transferidor de Dados
					Thread sender = new Thread() {
						public void run() {
							// Conteúdo para Envio
							byte content[] = conteudo.getBytes();
							try { // Possibilidade de Erro
								outStream.write(content.length); // Tamanho do
																	// Conteúdo
								outStream.write(content); // Conteúdo
															// Propriamente Dito

							} catch (IOException e) { // Erro Encontrado

							}
						}
					};
					// Executando o Fluxo de Processamento
					sender.start(); // Inicialização
				}
			}
		});

	}

	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		sensorManager.registerListener(this, sensor);
	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		sensorManager.unregisterListener(this, sensor);
	}

	public void onAccuracyChanged(int sensor, int accuracy) {
		Log.d(TAG, String.format(
				"onAccuracyChanged  sensor: %d   accuraccy: %d", sensor,
				accuracy));
	}

	public void onSensorChanged(int sensorReporting, float[] values) {
		if (sensorReporting != sensor)
			return;

		float azimuth = Math.round(values[0]);
		float pitch = Math.round(values[1]);
		float roll = Math.round(values[2]);

		String out = String.format("Azimuth: %.2f\nPitch: %.2f\nRoll: %.2f",
				azimuth, pitch, roll);

		Log.d(TAG, out);
		outView.setText(out);

		if ((pitch <= -5) && (pitch >= -45) && (roll < -10) && (roll > -80)) {
			esquerda.setBackgroundResource(R.drawable.left_off);
			direita.setBackgroundResource(R.drawable.right_on);
			ligar.setBackgroundResource(R.drawable.bt_liga_on);
			desligar.setBackgroundResource(R.drawable.bt_desliga_off);
			if (outStream != null) {
				// Captura do Texto
				final String conteudo = "c";
				// final String conteudo = mensagem.getText().toString();
				// Transferidor de Dados
				Thread sender = new Thread() {
					public void run() {
						// Conteúdo para Envio
						byte content[] = conteudo.getBytes();
						try { // Possibilidade de Erro
							outStream.write(content.length); // Tamanho do
																// Conteúdo
							outStream.write(content); // Conteúdo Propriamente
														// Dito

						} catch (IOException e) { // Erro Encontrado

						}
					}
				};
				// Executando o Fluxo de Processamento
				sender.start(); // Inicialização
			}
		} else if ((pitch <= -5) && (pitch >= -45) && (roll > 10)
				&& (roll < 80)) {
			esquerda.setBackgroundResource(R.drawable.left_on);
			direita.setBackgroundResource(R.drawable.right_off);
			ligar.setBackgroundResource(R.drawable.bt_liga_on);
			desligar.setBackgroundResource(R.drawable.bt_desliga_off);
			if (outStream != null) {
				// Captura do Texto
				final String conteudo = "d";
				// final String conteudo = mensagem.getText().toString();
				// Transferidor de Dados
				Thread sender = new Thread() {
					public void run() {
						// Conteúdo para Envio
						byte content[] = conteudo.getBytes();
						try { // Possibilidade de Erro
							outStream.write(content.length); // Tamanho do
																// Conteúdo
							outStream.write(content); // Conteúdo Propriamente
														// Dito

						} catch (IOException e) { // Erro Encontrado

						}
					}
				};
				// Executando o Fluxo de Processamento
				sender.start(); // Inicialização
			}
		} else if ((pitch <= -5) && (pitch >= -45)) {
			ligar.setBackgroundResource(R.drawable.bt_liga_on);
			desligar.setBackgroundResource(R.drawable.bt_desliga_off);
			esquerda.setBackgroundResource(R.drawable.left_off);
			direita.setBackgroundResource(R.drawable.right_off);
			if (outStream != null) {
				// Captura do Texto
				final String conteudo = "b";
				// final String conteudo = mensagem.getText().toString();
				// Transferidor de Dados
				Thread sender = new Thread() {
					public void run() {
						// Conteúdo para Envio
						byte content[] = conteudo.getBytes();
						try { // Possibilidade de Erro
							outStream.write(content.length); // Tamanho do
																// Conteúdo
							outStream.write(content); // Conteúdo Propriamente
														// Dito

						} catch (IOException e) { // Erro Encontrado

						}
					}
				};
				// Executando o Fluxo de Processamento
				sender.start(); // Inicialização
			}
		} else {
			esquerda.setBackgroundResource(R.drawable.left_off);
			direita.setBackgroundResource(R.drawable.right_off);
			desligar.setBackgroundResource(R.drawable.bt_desliga_on);
			ligar.setBackgroundResource(R.drawable.bt_liga_off);
			if (outStream != null) {
				// Captura do Texto
				final String conteudo = "a";
				// final String conteudo = mensagem.getText().toString();
				// Transferidor de Dados
				Thread sender = new Thread() {
					public void run() {
						// Conteúdo para Envio
						byte content[] = conteudo.getBytes();
						try { // Possibilidade de Erro
							outStream.write(content.length); // Tamanho do
																// Conteúdo
							outStream.write(content); // Conteúdo Propriamente
														// Dito

						} catch (IOException e) { // Erro Encontrado

						}
					}
				};
				// Executando o Fluxo de Processamento
				sender.start(); // Inicialização
			}
		}

	}

	/** necessário para se conectar a um dispositivo Bluetooth específico */
	public class ConnectThread extends Thread {
		private String address;
		private boolean connectionStatus;

		ConnectThread(String MACaddress) {
			address = MACaddress;
			connectionStatus = true;
		}

		public void run() {
			// Quando este retorna, ele vai "saber" sobre o servidor,
			// Através do seu endereço MAC.
			try {
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);

				// Precisamos de duas coisas antes que possamos conectar com
				// êxito
				// (Problemas de autenticação de lado): um endereço MAC, que nós
				// Já temos, e um canal RFCOMM.
				// Porque RFCOMM canais (portas aka) são limitados em
				// Número, o Android não permite que você usá-los diretamente;
				// Em vez de solicitar um mapeamento RFCOMM base em um serviço
				// ID. No nosso caso, vamos usar o conhecido serviço SPP
				// ID. Essa identificação está em UUID (GUID para vocês
				// Microsofties)
				// Formato. Dada a UUID, o Android vai lidar com o Mapeamento
				// para você. Geralmente, este retornará RFCOMM 1,
				// Mas nem sempre, depende do que outros serviços Bluetooth
				// Estão em uso no seu dispositivo Android.
				try {
					btSocket = device
							.createRfcommSocketToServiceRecord(SPP_UUID);
				} catch (IOException e) {
					connectionStatus = false;
				}
			} catch (IllegalArgumentException e) {
				connectionStatus = false;
			}

			// Descoberta pode estar acontecendo, por exemplo, se você estiver
			// executando um
			// 'Scan' para pesquisa de dispositivos Bluetooth do seu telefone
			// temos que chamar cancelDiscovery ().
			// você não quer que ele esteje em andamento quando
			// Uma tentativa de conexão é feita.
			mBluetoothAdapter.cancelDiscovery();

			// Bloqueio de ligação, para um cliente simples nada mais pode
			// Acontecer até que uma conexão bem-sucedida, de modo que
			// Não me importa se ele bloqueie.
			try {
				btSocket.connect();
			} catch (IOException e1) {
				try {
					btSocket.close();
				} catch (IOException e2) {
				}
			}

			// Crie um fluxo de dados para que possamos falar com o servidor.
			try {
				outStream = btSocket.getOutputStream();
			} catch (IOException e2) {
				connectionStatus = false;
			}

			// Enviar resultado final
			if (connectionStatus) {
				mHandler.sendEmptyMessage(1);
			} else {
				mHandler.sendEmptyMessage(0);
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// Quando DeviceListActivity retorna com um dispositivo para
			// conectar
			if (resultCode == Activity.RESULT_OK) {
				// Mostrar diálogo "aguarde"
				myProgressDialog = ProgressDialog.show(
						this,
						getResources().getString(R.string.aguarde),
						getResources().getString(
								R.string.conectando_ao_dispositivo), true);

				// Obter o endereço MAC do dispositivo
				deviceAddress = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Conectar a um dispositivo com um endereço MAC específico
				mConnectThread = new ConnectThread(deviceAddress);
				mConnectThread.start();

			} else {
				// Falha ao recuperar endereço MAC
				bluetoothOn.setVisibility(View.GONE);
				bluetoothOff.setVisibility(View.VISIBLE);
				Toast.makeText(this, R.string.macFalha, Toast.LENGTH_SHORT)
						.show();
			}
			break;
		case REQUEST_ENABLE_BT:
			// Quando o pedido para permitir retornos Bluetooth
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth está ativado
			} else {
				// Usuário não ativou o Bluetooth ou ocorreu um erro
				Toast.makeText(this, R.string.bt_nao_foi_ativado,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/*
	 * public void write(byte data) { if (outStream != null) { try {
	 * outStream.write(data); } catch (IOException e) { } } }
	 */
	public void emptyOutStream() {
		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
			}
		}
	}

	public void connect() {
		// Inicie o DeviceListActivity para ver dispositivos e fazer varredura
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		bluetoothOff.setVisibility(Button.GONE);
		bluetoothOn.setVisibility(Button.VISIBLE);
	}

	public void disconnect() {
		if (outStream != null) {
			try {
				outStream.close();
				connectStat = false;
				bluetoothOn.setVisibility(Button.GONE);
				bluetoothOff.setVisibility(Button.VISIBLE);
			} catch (IOException e) {
			}
		}
	}

	/*
	 * @Override
	 * 
	 * public boolean onCreateOptionsMenu(Menu menu) { MenuInflater inflater =
	 * getMenuInflater(); inflater.inflate(R.menu.option_menu, menu); return
	 * true; }
	 */

}