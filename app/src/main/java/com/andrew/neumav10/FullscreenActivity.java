package com.andrew.neumav10;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                    /*Para que la pantalla se comporte de una manera inmersiva que aunque se muestren los controles la
                    aplicación tambien pueda responder al gesto cuando se arrastra desde abajo o arriba son estas 3:
                    "https://developer.android.com/training/system-ui/immersive"
                    SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    SYSTEM_UI_FLAG_FULLSCREEN
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    Las otras son principalmente para que se mantenga la forma del layout ante cambios en la visualización
                    de las diferentes barras
                     */
        }
    };

    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    WebView myWebView;

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        /*Despues de interactuar con los controles de la aplicación (boton refrescar pantalla), se programa un llamado
        a la función hide() con retardo, esto para prevenir que los controles se desaparescan al mismo tiempo que se
        interactua con ellos.
        Para ello el OnTouchListener es asignado a una funcion que postea el mensaje con retardo en otro hilo al mismo
        metodo para ocultar que se llama cuando se toca la pantalla (en Toggle)*/
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        myWebView = findViewById(R.id.myWebView);

        configurarWeb();

        //cargarWeb("http://192.168.200.1");
        cargarWeb("http://google.co");


    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    /*Al ocultarse se desaparece inmediatamente la barra con el boton de "refrescar pantalla" y luego de un retardo
    programado en otro hilo (PostDelayed) la aplicación ocupa toda la pantalla*/
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /*Al reaparecer se sale inmediatamente del modo de pantalla conpleta y luego de un retardo programado en otro
    hilo (PostDelayed) aparece la barra con el boton de "refrescar pantalla"*/
    private void show() {
        /* Muestra las barras del sistema al remover todas las banderas
        excepto las que hacen que el contenido aparesca bajo las barras del sistema
        "https://developer.android.com/training/system-ui/immersive"*/
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                         | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void actualizar(View view) {
        //cargarWeb("http://192.168.200.1");
        cargarWeb("http://google.co");
        Toast.makeText(getApplicationContext(),"Oe!!",Toast.LENGTH_SHORT);
    }

    /*Adicionado para que la WebView tenga (en teoria) todas las caracteristicas implementadas tal como
        el navegador nativo del sistema
    TOMADO DE:
        "https://stackoverflow.com/questions/2835556/whats-the-difference-between-setwebviewclient-vs-setwebchromeclient"
     */
    private void configurarWeb() {
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
                myWebView.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Your Internet Connection May not be active Or " + error.getDescription(), Toast.LENGTH_LONG).show();
            }
        });
        //Para habilitar JavaScript
        myWebView.getSettings().setJavaScriptEnabled(true);
        //Para que la pagina se cargue de acuerdo al tag Viewport de la pagina y se vea completa en la ventana
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        //Para habilitar el uso del zoom mediante gestos sin mostrar un control al realizar el gesto
        myWebView.getSettings().setSupportZoom(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(false);
    }

    private void cargarWeb(String dir) {
        ConnectivityManager mCm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetInfo = mCm.getActiveNetworkInfo();
        if(mNetInfo !=null && mNetInfo.isConnectedOrConnecting()){
            myWebView.setVisibility(View.VISIBLE);
            myWebView.loadUrl(dir);
        }else{
            myWebView.setVisibility(View.GONE);
        }
    }

}