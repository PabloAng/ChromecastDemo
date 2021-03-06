package ar.com.creativa77.chromecast.demo;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.cast.*;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DemoActivity extends ActionBarActivity implements MediaRouteAdapter {
    private static final String TAG = DemoActivity.class.getSimpleName();
    private static final Logger sLog = new Logger(TAG, true);
    private static final String APP_NAME = "ChromecastDemo";

    private ApplicationSession mSession;
    private SessionListener mSessionListener;
    private SimpleStream mMessageStream;

    private TextView mInfoView;
    private EditText mTitle;
    private Button mSend;

    private CastContext mCastContext;
    private CastDevice mSelectedDevice;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;

    /**
     * Called when the activity is first created. Initializes the game with necessary listeners
     * for player interaction, and creates a new message stream.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);

        mTitle = (EditText) findViewById(R.id.title);
        mInfoView = (TextView) findViewById(R.id.info_turn);
        mSend = (Button) findViewById(R.id.send);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageStream.send(mTitle.getText().toString());
            }
        });

        mSessionListener = new SessionListener();
        mMessageStream = new SimpleStream();

        mCastContext = new CastContext(getApplicationContext());
        MediaRouteHelper.registerMinimalMediaRouteProvider(mCastContext, this);
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
                MediaRouteHelper.CATEGORY_CAST, APP_NAME, null);
        mMediaRouterCallback = new MediaRouterCallback();
    }

    /**
     * Called when the options menu is first created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    /**
     * Called on application start. Using the previously selected Cast device, attempts to begin a
     * session using the application name TicTacToe.
     */
    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    /**
     * Removes the activity from memory when the activity is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    /**
     * Attempts to end the current game session when the activity stops.
     */
    @Override
    protected void onStop() {
        endSession();
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    /**
     * Ends any existing application session with a Chromecast device.
     */
    private void endSession() {
        if ((mSession != null) && (mSession.hasStarted())) {
            try {
                if (mSession.hasChannel()) {

                }
                mSession.endSession();
            } catch (IOException e) {
                Log.e(TAG, "Failed to end the session.", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unable to end session.", e);
            } finally {
                mSession = null;
            }
        }
    }

    /**
     * Unregisters the media route provider and disposes the CastContext.
     */
    @Override
    public void onDestroy() {
        MediaRouteHelper.unregisterMediaRouteProvider(mCastContext);
        mCastContext.dispose();
        mCastContext = null;
        super.onDestroy();
    }

    /**
     * Returns the screen configuration to portrait mode whenever changed.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

     private void setSelectedDevice(CastDevice device) {
        mSelectedDevice = device;

        if (mSelectedDevice != null) {
            mSession = new ApplicationSession(mCastContext, mSelectedDevice);
            mSession.setListener(mSessionListener);

            try {
                mSession.startSession(APP_NAME);
            } catch (IOException e) {
                Log.e(TAG, "Failed to open a session", e);
            }
        } else {
            endSession();
            mInfoView.setText(R.string.select_device_text);
        }
    }

    /**
     * Called when a user selects a route.
     */
    private void onRouteSelected(RouteInfo route) {
        sLog.d("onRouteSelected: %s", route.getName());
        MediaRouteHelper.requestCastDeviceForRoute(route);
    }

    /**
     * Called when a user unselects a route.
     */
    private void onRouteUnselected(RouteInfo route) {
        sLog.d("onRouteUnselected: %s", route.getName());
        setSelectedDevice(null);
    }

    /**
     * A class which listens to session start events. On detection, it attaches the game's message
     * stream and joins a player to the game.
     */
    private class SessionListener implements ApplicationSession.Listener {
        @Override
        public void onSessionStarted(ApplicationMetadata appMetadata) {
            sLog.d("SessionListener.onStarted");

            mInfoView.setText(R.string.waiting_new_title);
            ApplicationChannel channel = mSession.getChannel();
            if (channel == null) {
                Log.w(TAG, "onStarted: channel is null");
                return;
            }
            channel.attachMessageStream(mMessageStream);
        }

        @Override
        public void onSessionStartFailed(SessionError error) {
            sLog.d("SessionListener.onStartFailed: %s", error);
        }

        @Override
        public void onSessionEnded(SessionError error) {
            sLog.d("SessionListener.onEnded: %s", error);
        }
    }

    /**
     * An extension of the GameMessageStream specifically for the TicTacToe game.
     */
    private class SimpleStream extends MessageStream {
        private final String TAG = SimpleStream.class.getSimpleName();

        private static final String NAMESPACE = "ar.com.creativa77.chromecast.demo";

        private static final String KEY_RESPONSE = "response";
        private static final String KEY_REQUEST = "request";

        private static final String KEY_ERROR = "error";

        protected SimpleStream() {
            super(NAMESPACE);
        }

        /**
         * Processes all JSON messages received from the receiver device and performs the appropriate
         * action for the message.
         */
        @Override
        public void onMessageReceived(JSONObject message) {
            try {
                Log.d(TAG, "onMessageReceived: " + message);
                if (message.has(KEY_RESPONSE)) {
                    String response = message.getString(KEY_RESPONSE);
                    Log.d(TAG, "Response");
                    Toast.makeText(DemoActivity.this, response, Toast.LENGTH_SHORT);
                } else if (message.has(KEY_ERROR)) {
                    Log.e(TAG, "Error: " + message.getString(KEY_ERROR));
                } else {
                    Log.w(TAG, "Unknown message: " + message);
                }
            } catch (JSONException e) {
                Log.w(TAG, "Message doesn't contain an expected key.", e);
            }
        }

        public final void send(String message) {
            try {
                Log.d(TAG, "send: " + message);
                JSONObject payload = new JSONObject();
                payload.put(KEY_REQUEST, message);
                sendMessage(payload);
            } catch (JSONException e) {
                Log.e(TAG, "Cannot create object to send message", e);
            } catch (IOException e) {
                Log.e(TAG, "Unable to send a message", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Message Stream is not attached", e);
            }
        }
    }

    /**
     * An extension of the MediaRoute.Callback specifically for the TicTacToe game.
     */
    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            sLog.d("onRouteSelected: %s", route);
            DemoActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            sLog.d("onRouteUnselected: %s", route);
            DemoActivity.this.onRouteUnselected(route);
        }
    }

    /* MediaRouteAdapter implementation */

    @Override
    public void onDeviceAvailable(CastDevice device, String routeId,
                                  MediaRouteStateChangeListener listener) {
        sLog.d("onDeviceAvailable: %s (route %s)", device, routeId);
        setSelectedDevice(device);
    }

    @Override
    public void onSetVolume(double volume) {
    }

    @Override
    public void onUpdateVolume(double delta) {
    }
}
