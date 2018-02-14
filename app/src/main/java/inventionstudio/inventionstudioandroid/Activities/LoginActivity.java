package inventionstudio.inventionstudioandroid.Activities;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;

import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.List;

import inventionstudio.inventionstudioandroid.API.SumsApiService;
import inventionstudio.inventionstudioandroid.Model.UserGroups;
import inventionstudio.inventionstudioandroid.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class LoginActivity extends Activity {
    public static final String USER_PREFERENCES = "UserPrefs";
    private Retrofit retrofit;
    public static final String BASE_URL = "https://sums.gatech.edu/SUMSAPI/rest/API/";
    private Call<List<UserGroups>> call;
    private boolean studioMember;
    WebView webView;
    Handler handlerForJavascriptInterface = new Handler();
    String otp;
    String username;
    final String usernameDisplayId = "LiverpoolTheme_wt1_block_wtMainContent_SilkUIFramework_wt8_block_wtColumn2_wt14_SilkUIFramework_wt14_block_wtContent1_wt15_SilkUIFramework_wt382_block_wtPanelContent_SilkUIFramework_wt109_block_wtColumn2_SilkUIFramework_wt289_block_wtPanelContent_SilkUIFramework_wtBrief_block_wtContent_wtUsernameDisplay";
    final String OtpDisplayId = "LiverpoolTheme_wt1_block_wtMainContent_SilkUIFramework_wt8_block_wtColumn2_wt14_SilkUIFramework_wt14_block_wtContent1_wt15_SilkUIFramework_wt382_block_wtPanelContent_SilkUIFramework_wt109_block_wtColumn2_SilkUIFramework_wt289_block_wtPanelContent_SilkUIFramework_wtBrief_block_wtContent_wtCalendarLink";

    private class MyTask extends AsyncTask<Void, String, String[]>  {
        @Override
        protected String[] doInBackground(Void...a) {

            webView.evaluateJavascript("document.getElementById(\"" + usernameDisplayId + "\").innerText", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    username = s;
                    System.out.println("1: username: " + username);

                }
            });
            webView.evaluateJavascript("document.getElementById(\"" + OtpDisplayId + "\").innerText", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    otp = s.split("=")[1];
                    System.out.println("2: otp: " + otp);

                }
            });
            return new String[] {username, otp};
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);



        webView = (WebView)findViewById(R.id.webView);
        webView.clearCache(true);
        webView.clearHistory();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        WebSettings websetting = webView.getSettings();
        websetting.setSaveFormData(true);


        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView webView, String url) {


                if (url.contains("EditResearcherProfile")) {
                    try {
                        String[] str_result = new MyTask().execute().get();
                        System.out.println("otp " + str_result[0] + " username " + str_result[1]);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                if (otp != null && username != null) {
                    Intent homeIntent = new Intent(getApplicationContext(), AgreementActivity.class);
                    startActivity(homeIntent);
                    finish();
                }
            }
            @Override
            public void onPageStarted(WebView webView, String url, Bitmap favicon) {
                try {
                    URL urlObj = new URL(url);
                    String baseURL = urlObj.getProtocol() + "://" + urlObj.getHost();
                    if (baseURL.equals("https://sums.gatech.edu")) {
                        // TODO: get the username and the OTP, waiting for Aman
                        // TODO: get Server Time and put in shared prefs

                        // String username = getUserName()
                        // String otp = getOTP()
                        // long lastLoginTime = getServerTime()


                        // SharedPreferences prefs = getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
                        // SharedPreferences.Editor editor =prefs.edit();


                        // editor.putString("username", username);
                        // editor.putString("OTP", otp)
                        // editor.putLong("lastLoginTime", lastLoginTime)


                        // editor.apply();

                        connectAndGetApiData();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        LoginActivity.this);

                alertDialogBuilder.setTitle("Refresh page");
                alertDialogBuilder
                        .setMessage("Login page has failed to load. Would you like to try again?")
                        .setCancelable(false)
                        .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                webView.reload();
                            }
                        })

                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        dialog.cancel();
                                    }
                                });
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

            }

        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://sums-dev.gatech.edu/EditResearcherProfile.aspx?");



    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();

        }
    }

    public void connectAndGetApiData() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        SumsApiService sumsApiService = retrofit.create(SumsApiService.class);
        // Call to preferences to get username and OTP
        // Replace hardcoded args when work in Login is complete.
        SharedPreferences prefs = this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        // String username = prefs.getString("username");
        // String otp = prefs.getString("otp");
        // TODO: Change to variables
        call = sumsApiService.getUserGroups("rkaup3", "HYXUVGNMLR34MKYZT20T");
        call.enqueue(new Callback<List<UserGroups>>() {
            @Override
            public void onResponse(Call<List<UserGroups>> call, Response<List<UserGroups>> response) {
                List<UserGroups> groups = response.body();
                studioMember = false;
                if (groups != null) {
                    for (UserGroups u : groups) {
                        if (u.getEquipmentGroupId() == 8) {
                            studioMember = true;
                        }
                    }
                }

                if (studioMember) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(getApplicationContext(), AgreementActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
            @Override
            public void onFailure(Call<List<UserGroups>> call, Throwable throwable) {
                Toast.makeText(getParent(), "An Error Occurred", Toast.LENGTH_SHORT).show();
            }
        });


    }

}

