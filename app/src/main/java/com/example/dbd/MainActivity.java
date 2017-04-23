package com.example.dbd;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents, TextToSpeech.OnInitListener
{
    TextToSpeech tts;
    Button btn1, btn2;
    EditText tv;
    EditText et;

    int m_waitSeconds = 0;
    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost th = (TabHost) findViewById(R.id.tabhost);
        th.setup();

        //------------------TAB 1------------------------
        TabHost.TabSpec spec = th.newTabSpec("tag1");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Speech To Text");
        th.addTab(spec);

        btn1=(Button)findViewById(R.id.btn1);
        tv = (EditText) findViewById(R.id.tv);
        tv.setEnabled(false);

        //--------------------------------------------------

        //------------------TAB 2------------------------
        spec = th.newTabSpec("tag2");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Text To Speech");
        th.addTab(spec);

        tts = new TextToSpeech(this, this);
        et = (EditText) findViewById(R.id.et);
        btn2 = (Button) findViewById(R.id.btn2);

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                speakOut();
            }
        });
        //--------------------------------------------------

        //------------------TAB 3------------------------
//        spec = th.newTabSpec("tag3");
//        spec.setContent(R.id.tab3);
//        spec.setIndicator("TAG 3");
//        th.addTab(spec);
        //--------------------------------------------------

    }

    public void start(View v)
    {
        this.btn1.setEnabled(false);
        this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;

        if (this.getUseMicrophone()) {
            if (this.micClient == null) {
                this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                        this,
                        this.getMode(),
                        this.getDefaultLocale(),
                        this,
                        this.getPrimaryKey());
                this.micClient.setAuthenticationUri(this.getAuthenticationUri());
            }
            this.micClient.startMicAndRecognition();
        }
        else
        {
            if (null == this.dataClient) {
                this.dataClient = SpeechRecognitionServiceFactory.createDataClient(
                        this,
                        this.getMode(),
                        this.getDefaultLocale(),
                        this,
                        this.getPrimaryKey());
                this.dataClient.setAuthenticationUri(this.getAuthenticationUri());
            }
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.ENGLISH);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "This Language is not supported", Toast.LENGTH_SHORT).show();
            } else {
                btn2.setEnabled(true);
                speakOut();
            }

        } else {
            Toast.makeText(this, "Initilization Failed!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void speakOut() {

        String text = et.getText().toString();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public enum FinalResponseStatus { NotReceived, OK, Timeout }

    @Override
    public void onPartialResponseReceived(String s)
    {
//        this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
//        this.WriteLine(s);
//        this.WriteLine();

    }

    @Override
    public void onFinalResponseReceived(RecognitionResult response)
    {

        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone()) {
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDicationMessage) {
            this.btn1.setEnabled(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {
            //this.WriteLine("********* Final n-BEST Results *********");
            this.WriteLine(response.Results[0].DisplayText);
//            for (int i = 0; i < response.Results.length; i++) {
//                this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
//                        " Text=\"" + response.Results[i].DisplayText + "\"");
//            }

            this.WriteLine();
        }
    }

    @Override
    public void onIntentReceived(String s)
    {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(s);
        this.WriteLine();
    }

    @Override
    public void onError(int i, String response)
    {
        this.btn1.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(i) + " " + i);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    @Override
    public void onAudioEvent(boolean b)
    {
//        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
//        this.WriteLine("********* Microphone status: " + b + " *********");
        if (b) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
        if (!b) {
            this.micClient.endMicAndRecognition();
            this.btn1.setEnabled(true);
        }
    }

    private SpeechRecognitionMode getMode() {

        return SpeechRecognitionMode.LongDictation;


    }

    private Boolean getUseMicrophone() {
        return true;
    }

    private String getDefaultLocale() {
        return "en-us";
    }

    public String getPrimaryKey() {
        return this.getString(R.string.primaryKey);
    }

    private String getAuthenticationUri() {
        return "";
    }

    private void WriteLine(String text) {
        this.tv.append(text + "\n");
    }

    private void WriteLine() {
        this.WriteLine("");
    }


}

