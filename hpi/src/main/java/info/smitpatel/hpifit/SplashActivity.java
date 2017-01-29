package info.smitpatel.hpifit;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import info.smitpatel.hpifit.app.HPIApp;
import info.smitpatel.hpifit.app.SystemHelper;
import info.smitpatel.hpifit.db.DatabaseHandler;
import info.smitpatel.hpifit.models.UserProfile;

public class SplashActivity extends AppCompatActivity {
    private static final String PREFIX = SplashActivity.class.getSimpleName() + ": ";

    private Context context;

    private Button btnLogIn;
    private Button btnSignUp;

    private LinearLayout layoutLoginForm;
    private AutoCompleteTextView txtUsername;
    private EditText eTxtPassword;
    private Button btnLoginCancel;
    private Button btnLoginProceed;

    private LinearLayout layoutLoading;
    private ProgressBar progressBarLogin;

    private UserProfile userProfile;

    private boolean isLoggedIn = false;
    private String lastUsername = "";

    @Override
    protected void onResume() {
        super.onResume();
        HPIApp.logger(PREFIX, "onResume()", Log.INFO);

        if(!isLoggedIn) {
            showButtons();
            hideLoginForm();
            hideProgressBar();
            return;
        }

        HPIApp.logger(PREFIX, "user is logged in and user profile exist", Log.DEBUG);

        hideButtons(false);
        hideLoginForm();
        showProgressBar();

        // faking the login
        // we should be verifying user via a token or etc with the server here
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new ValidateUser().execute();
            }
        }, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        HPIApp.logger(PREFIX, "onPause()", Log.INFO);
        SystemHelper.hideSoftKeyboard(context, txtUsername);
    }

    @Override
    public void onBackPressed() {
        HPIApp.logger(PREFIX, "onBackPressed()", Log.INFO);
        if(layoutLoginForm.getVisibility() == View.VISIBLE) {
            hideLoginForm();
            showButtons();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HPIApp.logger(PREFIX, "onCreate()", Log.INFO);

        setContentView(R.layout.activity_splash);

        context = this;

        isLoggedIn = Util.isLoggedIn();
        lastUsername = Util.getUsername();

        btnLogIn = (Button) findViewById(R.id.btnLogIn);
        btnSignUp = (Button) findViewById(R.id.btnSignUp);

        layoutLoginForm = (LinearLayout) findViewById(R.id.layoutLoginForm);
        txtUsername = (AutoCompleteTextView) findViewById(R.id.txtUsername);
        eTxtPassword = (EditText) findViewById(R.id.eTxtPassword);
        btnLoginCancel = (Button) findViewById(R.id.btnLoginCancel);
        btnLoginProceed = (Button) findViewById(R.id.btnLoginProceed);

        layoutLoading = (LinearLayout) findViewById(R.id.layoutLoading);
        progressBarLogin = (ProgressBar) findViewById(R.id.progressBarLogin);

        hideButtons(false);

        if(!lastUsername.isEmpty()) {
            hideLoginForm();
            showProgressBar();
        }

        progressBarLogin.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideButtons(true);
                showLoginForm();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SplashActivity.this, "Coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        txtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0 && eTxtPassword.length() > 0) {
                    btnLoginProceed.setEnabled(true);
                } else {
                    btnLoginProceed.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        eTxtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(txtUsername.length() > 0 && s.length() > 0) {
                    btnLoginProceed.setEnabled(true);
                } else {
                    btnLoginProceed.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLoginCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SystemHelper.hideSoftKeyboard(context, txtUsername);
                hideLoginForm();
                showButtons();
            }
        });

        btnLoginProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = String.valueOf(txtUsername.getText());
                String password = String.valueOf(eTxtPassword.getText());

                if(username == null || password == null || username.isEmpty() || password.isEmpty()) {
                    HPIApp.logger(PREFIX, "username or password is empty!", Log.ERROR);
                    return;
                }

                if(performFakeLogin(username, password)) {
                    SystemHelper.hideSoftKeyboard(context, txtUsername);
                    userProfile = generateTestUserData(username);
                    launchHome();
                }
            }
        });
    }

    /**
     * shows bottoms buttons with animation
     */
    private void showButtons() {
        btnLogIn.animate().setDuration(1000).alpha(1.0f).start();
        btnSignUp.animate().setDuration(1000).alpha(1.0f).start();
    }

    /**
     * hides bottom buttons with or without animation
     * @param withAnim if true, with animation. If false, no animation
     */
    private void hideButtons(boolean withAnim) {
        if(withAnim) {
            btnLogIn.animate().setDuration(1000).alpha(0.0f).start();
            btnSignUp.animate().setDuration(1000).alpha(0.0f).start();
        } else {
            btnLogIn.setAlpha(0.0f);
            btnSignUp.setAlpha(0.0f);
        }
    }

    /**
     * shows the login form for user to enter credentials
     */
    private void showLoginForm() {
        layoutLoginForm.setVisibility(View.VISIBLE);
        layoutLoginForm.setAlpha(0.0f);
        layoutLoginForm.animate().setDuration(700).alpha(1.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if(txtUsername.getText().length() == 0 || eTxtPassword.getText().length() == 0) {
                    btnLoginProceed.setEnabled(false);
                } else {
                    btnLoginProceed.setEnabled(true);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                txtUsername.requestFocus();
                SystemHelper.showSoftKeyboard(context);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        }).start();
    }

    /**
     * hides login form with animation
     */
    private void hideLoginForm() {
        layoutLoginForm.animate().setDuration(700).alpha(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                layoutLoginForm.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        }).start();
    }

    /**
     * shows loading progress bar with animation
     */
    private void showProgressBar() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutLoading.setAlpha(0.0f);
        layoutLoading.animate().setDuration(700).alpha(1.0f).start();
    }

    /**
     * hides loading progress bat with animation
     */
    private void hideProgressBar() {
        layoutLoading.animate().setDuration(700).alpha(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                layoutLoading.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        }).start();
    }

    /**
     * validates user entered data
     * @param username username
     * @param password password
     * @return true if data is valid, false otherwise
     */
    private boolean performFakeLogin(String username, String password) {
        boolean loginSuccess = false;

        UserProfile userProfile = generateTestUserData(username);

        if(userProfile != null && password.equals(userProfile.getPassword())) {
            loginSuccess = true;
        }

        if(loginSuccess) {
            Util.setLoggedInFlag(true);
            Util.saveUsername(username);

            DatabaseHandler handler = DatabaseHandler.getInstance(context);
            handler.saveUserProfile(userProfile);
        }

        return loginSuccess;
    }

    /**
     * generates fake user credentials and UserProfile data
     * @param username username
     * @return generated UserProfile
     */
    private UserProfile generateTestUserData(String username) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername(username);
        userProfile.setPassword("abc123");

        switch (username) {
            case "jteller":
                userProfile.setFirstName("Jax");
                userProfile.setLastName("Teller");
                userProfile.setMilestonesCount("0");
                return userProfile;
            case "ddraper":
                userProfile.setFirstName("Don");
                userProfile.setLastName("Draper");
                userProfile.setMilestonesCount("0");
                return userProfile;
            default:
                return null;
        }
    }

    /**
     * launches HomeActivity and finishes current activity
     */
    private void launchHome() {
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        intent.putExtra(HPIApp.Prefs.BUNDLE_KEY_USER_PROFILE, userProfile);
        startActivity(intent);
        this.finish();
    }

    /**
     * faking the user login validation
     */
    private class ValidateUser extends AsyncTask<String, Integer, Boolean> {

        ValidateUser() {}

        @Override
        protected Boolean doInBackground(String... params) {
            DatabaseHandler handler = DatabaseHandler.getInstance(context);
            userProfile = handler.getUserProfile(lastUsername);

            if(userProfile == null) {
                HPIApp.logger(PREFIX, "userProfile not available", Log.DEBUG);
                return false;
            }

            return performFakeLogin(userProfile.getUsername(), userProfile.getPassword());
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                launchHome();
            } else {
                hideButtons(false);
                showLoginForm();
                hideProgressBar();
            }
        }
    }
}
