package com.crupee.lottery.login;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crupee.lottery.R;
import com.crupee.lottery.controller.core.FailureReason;
import com.crupee.lottery.controller.core.HttpTaskListener;
import com.crupee.lottery.controller.core.ParserFactory;
import com.crupee.lottery.controller.core.ParserFamily;
import com.crupee.lottery.controller.core.ServerConnectorDTO;
import com.crupee.lottery.controller.core.ServerTask;
import com.crupee.lottery.dashboard.activity.MainActivity;
import com.crupee.lottery.utility.AppText;
import com.crupee.lottery.utility.PrefUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    //views
    EditText email, password, re_password;
    RelativeLayout login;

    //variable
    public static String TAG = LoginActivity.class.getName();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        email = (EditText) findViewById(R.id.emailAddress);
        password = (EditText) findViewById(R.id.password);
        login = (RelativeLayout) findViewById(R.id.login);

        //check if the user has logged in before or not
        /*
         *if yes the move directly to main activity
         *if no login using credentials
         */

        if (PrefUtils.returnLoggedIn(LoginActivity.this)) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        //click method
        clickMethod();

    }

    private void clickMethod() {

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //check if the input field is empty
                if (checkEmpty()) {
                    CallingLoginAPI(email.getText().toString(), password.getText().toString());
                }
            }
        });
    }

    private boolean checkEmpty() {
        boolean check = false;

        if (email.getText().toString().isEmpty()) {
            email.setError("Required");
            showCustomViewSnackbar("Email is required", "error");
            check = false;

        } else if (password.getText().toString().isEmpty()) {
            password.setError("Required");
            showCustomViewSnackbar("password is required", "error");
            check = false;

        }  else {
            check = true;
        }


        return check;
    }

    private void CallingLoginAPI(String email, String password) {

        Map<String, String> dataObj = new HashMap<String, String>();
        showProgressDialog(true);

        //creating hash map objects of the data to send in the api
        dataObj.put("email", email);
        dataObj.put("password", password);

        //calling login api
        ParserFamily parserFamily = ParserFactory.create(LoginActivity.this, ParserFactory.ParserType.LOGIN_RESPONSE);
        parserFamily.setParserCallBack(new HttpTaskListener() {
            @Override
            public void onAPiResponseObtained(int taskId, String result) {
                String loginResponse = result;


                try {

                    JSONObject jsonObject = new JSONObject(loginResponse);

                    String status = jsonObject.getString("status");

                    if (status.equalsIgnoreCase("true")) {
                        showProgressDialog(false);

                        showCustomViewSnackbar("Loggin Successful", "success");

                        //set user has logged in
                        PrefUtils.saveloggedIn(LoginActivity.this, true);

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }, 1000);

                    } else if (status.equalsIgnoreCase("false")) {
                        showProgressDialog(false);
                        String message = jsonObject.getString("message");
                        showCustomViewSnackbar(message, "error");
                    }


                } catch (JSONException e) {
                    showProgressDialog(false);
                    e.printStackTrace();
                }

            }

            @Override
            public void onApiResponseFailed(int taskId, FailureReason reason, String errorMessage, String errorCode) {
                Log.d(TAG, "response failed::::" + reason.toString());
                showProgressDialog(false);
                showCustomViewSnackbar(getResources().getString(R.string.cannot_login), "error");


            }

            @Override
            public void onTaskStarted(int taskId) {

            }

            @Override
            public void onTaskCancelled(int taskId) {

            }
        });
        ServerConnectorDTO connectorDTO = new ServerConnectorDTO();
        connectorDTO.setUrlToConnect(AppText.BASE_URL + AppText.LOGIN_URL);
        connectorDTO.setDataListNameValuePair(dataObj);
        Log.d(TAG, "datapost:::" + dataObj.toString());
        ServerTask serverTask = new ServerTask(parserFamily, connectorDTO);
        serverTask.execute(ServerTask.RequestMethod.POST);

    }

    private void showProgressDialog(boolean shouldShow) {
        if (shouldShow) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(true);
                progressDialog.setMessage(getResources().getString(R.string.loading));
                progressDialog.show();
            }
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }

    }

    @SuppressLint("ResourceAsColor")
    private void showCustomViewSnackbar(String message, String check) {


        // Create an instance.
        Snackbar snackbar = Snackbar.make(findViewById(R.id.emailAddress), "", Snackbar.LENGTH_SHORT);


        // Get the view object.
        Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();

        // Get custom view from external layout xml file.
        View customView = getLayoutInflater().inflate(R.layout.custom_snackbar_layout, null);
        TextView textView = (TextView) customView.findViewById(R.id.txtSnackbar);
        ImageView imageView = (ImageView) customView.findViewById(R.id.imgSnackbar);
        textView.setText(message);

        if (check.equalsIgnoreCase("error")) {
            snackbarView.setBackgroundColor(this.getResources().getColor(R.color.error));
            imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.custom_error));
        } else {
            snackbarView.setBackgroundColor(this.getResources().getColor(R.color.success));
            imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.custom_success));
        }
        snackbarView.addView(customView, 0);


        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) customView.getLayoutParams();
        params.gravity = Gravity.TOP;
        customView.setLayoutParams(params);

        snackbar.show();
    }
}
