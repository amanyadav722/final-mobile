package com.example.suivinfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView txtNFCData;
    private EditText editTextMessageToWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        txtNFCData = findViewById(R.id.txtNFCData);
        editTextMessageToWrite = findViewById(R.id.editTextWriteNFC);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show();
            finish();
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        Button btnScanNFC = findViewById(R.id.btnScanNFC);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnWriteNFC = findViewById(R.id.btnWriteNFC);
        Button btnUpdateNFC = findViewById(R.id.btnUpdateNFC);

        btnScanNFC.setOnClickListener(view -> enableForegroundDispatchSystem());
        btnReset.setOnClickListener(view -> resetData());
        btnWriteNFC.setOnClickListener(view -> prepareForWriteNfcMessage());
        btnUpdateNFC.setOnClickListener(view -> prepareForUpdateNfcMessage());

        // Initialize any additional components if needed
    }

    private void enableForegroundDispatchSystem() {
        IntentFilter[] intentFilters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void resetData() {
        txtNFCData.setText("NFC Data will appear here");
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processNfcIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            if (editTextMessageToWrite.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter text before attempting to write to a tag.", Toast.LENGTH_LONG).show();
                return;
            }
            if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                writeNfcMessage(tag);
            } else {
                updateNfcMessage(tag, editTextMessageToWrite.getText().toString());
            }
        }
    }

    private void prepareForWriteNfcMessage() {
        Toast.makeText(this, "Ready to write. Please bring an NFC tag close to your device.", Toast.LENGTH_LONG).show();
    }

    private void prepareForUpdateNfcMessage() {
        Toast.makeText(this, "Ready to update. Please bring an NFC tag close to your device.", Toast.LENGTH_LONG).show();
    }

    private void writeNfcMessage(Tag tag) {
        String message = editTextMessageToWrite.getText().toString();
        NdefMessage ndefMessage = createNdefMessage(message);
        try {
            writeOrUpdateNfcTag(tag, ndefMessage);
        } catch (IOException | FormatException e) {
            Toast.makeText(this, "Error writing to NFC tag: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateNfcMessage(Tag tag, String newText) {
        NdefMessage ndefMessage = createNdefMessage(newText);
        try {
            writeOrUpdateNfcTag(tag, ndefMessage);
        } catch (IOException | FormatException e) {
            Toast.makeText(this, "Error updating NFC tag: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeOrUpdateNfcTag(Tag tag, NdefMessage message) throws IOException, FormatException {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            if (!ndef.isWritable()) {
                Toast.makeText(this, "NFC Tag is not writable", Toast.LENGTH_LONG).show();
                return;
            }
            if (ndef.getMaxSize() < message.toByteArray().length) {
                Toast.makeText(this, "NFC Tag does not have enough space", Toast.LENGTH_LONG).show();
                return;
            }

            ndef.connect();
            ndef.writeNdefMessage(message);
            Toast.makeText(this, "NFC Tag written/updated successfully!", Toast.LENGTH_LONG).show();
            ndef.close();
        } else {
            Toast.makeText(this, "NFC Tag is not NDEF formatted or cannot be written to", Toast.LENGTH_LONG).show();
        }
    }

    private NdefMessage createNdefMessage(String text) {
        NdefRecord textRecord = NdefRecord.createTextRecord("en", text);
        return new NdefMessage(new NdefRecord[]{textRecord});
    }

    private void processNfcIntent(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null && rawMessages.length > 0) {
            NdefMessage message = (NdefMessage) rawMessages[0];
            NdefRecord[] records = message.getRecords();
            for (NdefRecord record : records) {
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                    byte[] payload = record.getPayload();
                    txtNFCData.setText(new String(payload, StandardCharsets.UTF_8));
                }
            }
        } else {
            txtNFCData.setText("No NDEF messages found.");
        }
    }
}
