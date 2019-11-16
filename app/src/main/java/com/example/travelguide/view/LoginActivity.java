package com.example.travelguide.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.TokenWatcher;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelguide.R;
import com.example.travelguide.manager.Constants;
import com.example.travelguide.manager.MyApplication;
import com.example.travelguide.model.LoginRequest;
import com.example.travelguide.model.LoginResponse;
import com.example.travelguide.network.MyAPIClient;
import com.example.travelguide.network.UserService;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 0;
    public static String TAG  = "LoginActivity";
    private static final String EMAIL = "email";

    // UI references.
    private AutoCompleteTextView emailPhoneView;
    private EditText passwordView;
    private UserService userService;
    private RelativeLayout relLayout_SignInFormWithAppName, relLayout_SignUpForgotPwBtn;
    private ProgressDialog progressDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton signInButton_Google;


    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            relLayout_SignInFormWithAppName.setVisibility(View.VISIBLE);
            relLayout_SignUpForgotPwBtn.setVisibility(View.VISIBLE);
        }
    };
    private CallbackManager callbackManager;
    private LoginButton signInButton_Facebook;

    public static void printHashKey(Context pContext) {
        try {
            PackageInfo info = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //printHashKey(this);
        // Google service for signing in
        // Configure sign-in to request offline access to the user's ID, basic
        // profile, and Google Drive. The first time you request a code you will
        // be able to exchange it for an access token and refresh token, which
        // you should store. In subsequent calls, the code will only result in
        // an access token. By asking for profile access (through
        // DEFAULT_SIGN_IN) you will also get an ID Token as a result of the
        // code exchange.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestServerAuthCode(Constants.Google_ClientID)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton_Google = findViewById(R.id.signInButton_Google);
        signInButton_Google.setSize(SignInButton.SIZE_WIDE);
        signInButton_Google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.signInButton_Google:
                        signIn();
                        break;
                    // ...
                }
            }
        });

        //Facebook service for signing in
        callbackManager = CallbackManager.Factory.create();

        signInButton_Facebook = findViewById(R.id.signInButton_Facebook);
        signInButton_Facebook.setPermissions(Arrays.asList(EMAIL));
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        signInButton_Facebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Toast.makeText(LoginActivity.this, "Sign in with Facebook success", Toast.LENGTH_LONG).show();
                Log.i(TAG, "quanghuy said: " + loginResult.getAccessToken().toString());
            }

            @Override
            public void onCancel() {
                // App code
                Toast.makeText(LoginActivity.this, "Sign in with Facebook cancelled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Toast.makeText(LoginActivity.this, "Sign in with Facebook error", Toast.LENGTH_LONG).show();
                Log.i(TAG, exception.getMessage());
            }
        });

        relLayout_SignInFormWithAppName = findViewById(R.id.relLayout_SignInFromWithAppName);
        relLayout_SignUpForgotPwBtn = findViewById(R.id.relLayout_SignInForgotPwBtn);

        handler.postDelayed(runnable, 2000);

        userService = MyAPIClient.getInstance().getAdapter().create(UserService.class);

        // Set up the login form.
        emailPhoneView = findViewById(R.id.emailPhone);
        emailPhoneView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //passwordView.requestFocus();
                    hideKeyboard();
                    handled = true;
                }
                return handled;
            }
        });

        passwordView = findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    //attemptLogin();
                    hideKeyboard();
                    handled = true;
                }
                return handled;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.signInButton);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            //String test = account.getServerAuthCode();
            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
}

    @Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        // GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        // updateUI(account);
    }

    private void updateUI(GoogleSignInAccount account) {
        //Change UI according to user data.
        if (account != null) {
            Toast.makeText(this, "Sign in with Google success", Toast.LENGTH_LONG).show();
            //Toast.makeText(this, account.getEmail(), Toast.LENGTH_LONG).show();

            String test = account.getServerAuthCode();
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormEncodingBuilder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", Constants.Google_ClientID)
                    .add("client_secret", Constants.Google_ClientSecret)
                    .add("redirect_uri","")
                    .add("code", account.getServerAuthCode())
                    .build();
            final Request request = new Request.Builder()
                    .url("https://www.googleapis.com/oauth2/v4/token")
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            Log.e(TAG, e.toString());
                        }

                        @Override
                        public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().string());
                                final String message = jsonObject.toString(5);
                                String accessToken = jsonObject.getString("access_token");
                                Log.i(TAG, message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );


            //startActivity(new Intent(this,AnotherActivity.class));
        } else {
            Toast.makeText(this, "Sign in with Google error", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        emailPhoneView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailPhoneView.getText().toString();
        String password = passwordView.getText().toString();

        boolean cancel = false;
//        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            //focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailPhoneView.setError(getString(R.string.error_field_required));
            //focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailPhoneView.setError(getString(R.string.error_invalid_email));
            //focusView = mEmailView;
            cancel = true;
        }

        if(cancel == true){

        }else{
            progressDialog= new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Please wait...");
            //To show the dialog
            progressDialog.show();

            //To dismiss the dialog
//        progressDialog.dismiss();

            final LoginRequest request = new LoginRequest();
            request.setUsername(email);
            request.setPassword(password);
            Call<LoginResponse> call = userService.login(request);

            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    //Login successfully
                    if(response.code() == 200) {
                        // Save login info
                        MyAPIClient.getInstance().setAccessToken(response.body().getToken());
                        long time = (new Date()).getTime() / 1000;
                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_name), 0);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.saved_access_token), response.body().getToken());
                        editor.putLong(getString(R.string.saved_access_token_time), time);
                        editor.commit();

                        MyApplication app = (MyApplication) LoginActivity.this.getApplication();
                        app.setTokenInfo((response.body()));

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        LoginActivity.this.finish();
                    }

                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.d(TAG, t.getMessage());
                }
            });
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
//        return email.contains("@");
        return true;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private void hideKeyboard() {
        try {
            // use application level context to avoid unnecessary leaks.
            InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            assert inputManager != null;
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
