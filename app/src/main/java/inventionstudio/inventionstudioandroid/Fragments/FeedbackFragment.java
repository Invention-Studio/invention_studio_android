package inventionstudio.inventionstudioandroid.Fragments;



import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import inventionstudio.inventionstudioandroid.API.ServerApiService;
import inventionstudio.inventionstudioandroid.API.SumsApiService;
import inventionstudio.inventionstudioandroid.Model.CustomSeekBar;
import inventionstudio.inventionstudioandroid.Model.Equipment;
import inventionstudio.inventionstudioandroid.Model.GeneralFeedback;
import inventionstudio.inventionstudioandroid.Model.StaffFeedback;
import inventionstudio.inventionstudioandroid.Model.ToolBrokenFeedback;
import inventionstudio.inventionstudioandroid.R;
import okhttp3.Credentials;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class FeedbackFragment extends Fragment {
    public static final String USER_PREFERENCES = "UserPrefs";
    public static final String BASE_URL = "https://sums.gatech.edu/SUMSAPI/rest/API/";
    private static Retrofit retrofit = null;
    private ArrayList<String> machineNames;
    private HashSet<String> machineTypes;
    private Call call;
    private Spinner machineNameSpinner;
    private Spinner machineTypeSpinner;
    private TextView seekText;
    private List<Equipment> equipmentList;

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_feedback, container, false);

        // Use the bottom bar to set the title of the page
        BottomNavigationView bottom =  (getActivity().findViewById(R.id.bottomBar));
        getActivity().setTitle(bottom.getMenu().findItem(bottom.getSelectedItemId()).getTitle());

        // Get the user's name from shared preferences and put in a textview
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        final String name = prefs.getString("name", "");
        final TextView nameText = (TextView) rootView.findViewById(R.id.name);
        nameText.setText(name);

        // Create the anon switch and set it to change the user's name to anonymous if on
        final Switch anonSwitch = (Switch) rootView.findViewById(R.id.anon_switch);
        anonSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    nameText.setText("Anonymous");
                } else {
                    nameText.setText(name);
                }
            }
        });

        // New SeekBar creation
        seekText = (TextView) rootView.findViewById(R.id.seek_text);
        LinearLayout seekBarLayout = (LinearLayout) rootView.findViewById(R.id.seekbar_layout);
        final CustomSeekBar ratingBar = new CustomSeekBar(this.getContext(), 11, R.color.IS_Text_Light, seekText);
        ratingBar.addSeekBar(seekBarLayout);
        ratingBar.setProgress(11);



        // EditText Instantiation
        final EditText commentTextInput = (EditText) rootView.findViewById(R.id.plain_text_input);
        final EditText piTextInput = (EditText) rootView.findViewById(R.id.piname_text_input);

        // Data for type of feedback
        final Spinner feedbackSpinner = (Spinner) rootView.findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.feedback_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        feedbackSpinner.setAdapter(adapter);

        // API machine data
        machineNameSpinner = (Spinner) rootView.findViewById(R.id.machine_spinner);
        machineTypeSpinner = (Spinner) rootView.findViewById(R.id.type_spinner);
        connectAndGetFeedbackEquipment();

        // Tool Broken data
        final Spinner issueSpinner = (Spinner) rootView.findViewById(R.id.issue_spinner);
        ArrayAdapter<CharSequence> issueAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.feedback_array_default,
                android.R.layout.simple_spinner_item);
        issueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        issueSpinner.setAdapter(issueAdapter);

        // Button functionality
        final Button submit = (Button) rootView.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
                String username;
                boolean fieldsFilled = true;

                // Checks to see if all necessary inputs are filled for whichever
                // type of feedback is being sent
                if (feedbackSpinner.getSelectedItem().toString().equals("PI Feedback")) {
                    // Show message if there is no PI name, or only white spaces
                    if (piTextInput.getText().toString().trim().equals("")) {
                        showToast("Please fill out the PI name field.");
                        fieldsFilled = false;
                    }
                } else if (feedbackSpinner.getSelectedItem().toString().equals("Tool Broken")) {
                    // Show message if there are no comments on the problem
                    if (commentTextInput.getText().toString().trim().equals("")
                            && issueSpinner.getSelectedItem().toString().trim().equals("Other")) {
                        showToast("Please give a description of your specific issue.");
                        fieldsFilled = false;
                    }
                } else if (feedbackSpinner.getSelectedItem().toString().equals("General Feedback")) {
                    // Show message if there are no comments, as it is the only feedback for this type
                    if (commentTextInput.getText().toString().trim().equals("")) {
                        showToast("Please fill out the comments field.");
                        fieldsFilled = false;
                    }
                }

                // only create and send info if the fields are filled
                if (fieldsFilled) {
                    // choose which username to use based on anonymity or not
                    if (anonSwitch.isChecked()) {
                        username = "anonymous";
                    } else {
                        username = prefs.getString("username", "");
                    }

                    // Create the correct object based on the type of data being sent
                    if (feedbackSpinner.getSelectedItem().toString().equals("PI Feedback")) {
                        // If N/A is selected in ratingBar, will return a -1
                        int rating = ratingBar.getProgress();
                        if (rating == 0) {
                            rating = -1;
                        }
                        StaffFeedback feedback = new StaffFeedback(
                                8,
                                username,
                                piTextInput.getText().toString().trim(),
                                rating,
                                commentTextInput.getText().toString().trim());
                        connectAndSendPIFeedback(feedback);

                    } else if (feedbackSpinner.getSelectedItem().toString().equals("Tool Broken")) {
                        ToolBrokenFeedback feedback = new ToolBrokenFeedback(
                                8,
                                username,
                                issueSpinner.getSelectedItem().toString(),
                                machineTypeSpinner.getSelectedItem().toString(),
                                machineNameSpinner.getSelectedItem().toString(),
                                commentTextInput.getText().toString().trim()
                        );
                        connectAndSendToolFeedback(feedback);

                    } else if (feedbackSpinner.getSelectedItem().toString().equals("General Feedback")) {
                        GeneralFeedback feedback = new GeneralFeedback(
                                8,
                                username,
                                commentTextInput.getText().toString().trim());
                        connectAndSendGeneralFeedback(feedback);
                    }
                }
            }
        });


        feedbackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Method creation to change the view based on which type of feedback is selected
             * so that the correct elements are shown to the user
             */
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = feedbackSpinner.getSelectedItem().toString();
                View ratingGroup = rootView.findViewById(R.id.rating_group);
                View issueGroup = rootView.findViewById(R.id.machine_issue_group);
                if (text.equals("PI Feedback")) {
                    ratingGroup.setVisibility(View.VISIBLE);
                    issueGroup.setVisibility(View.GONE);
                } else if (text.equals("Tool Broken")) {
                    ratingGroup.setVisibility(View.GONE);
                    issueGroup.setVisibility(View.VISIBLE);
                } else {
                    ratingGroup.setVisibility(View.GONE);
                    issueGroup.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No case
            }
        });


        machineTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Method to change the machine spinner based on what machine type is selected
             */
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                // Get the string of the selected item, create retrofit to call SUMS API
                final String selected = machineTypeSpinner.getSelectedItem().toString();

                int problemArray = 0;
                if (selected.equals("3D Printers")) {
                    problemArray = R.array.feedback_array_3d_printers;
                } else if (selected.equals("Laser Cutters")) {
                    problemArray = R.array.feedback_array_laser_cutters;
                } else if (selected.equals("Specialty 3D Printers")) {
                    problemArray = R.array.feedback_array_specialty_3d_printers;
                } else if (selected.equals("Waterjets")) {
                    problemArray = R.array.feedback_array_waterjets;
                } else {
                    problemArray = R.array.feedback_array_default;
                }

                ArrayAdapter<CharSequence> issueAdapter = ArrayAdapter.createFromResource(getActivity(), problemArray,
                        android.R.layout.simple_spinner_item);
                issueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                issueSpinner.setAdapter(issueAdapter);

                machineNames = new ArrayList<>();
                for (Equipment e : equipmentList) {
                    if (e.getLocationName().equals(selected)) {
                        machineNames.add(e.getToolName());
                    }
                }

                Collections.sort(machineNames);
                ArrayAdapter<String> machineNameAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, machineNames);
                machineNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                machineNameSpinner.setAdapter(machineNameAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No case
            }
        });

        return rootView;
    }

    @Override
    public void onPause () {
        super.onPause();
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * method which contacts the SUMS API and populates all necessary spinners
     * with the returned data
     */
    public void connectAndGetFeedbackEquipment() {

        // Create a retrofit to communicate with the SUMS API
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Grab information to pass in as headers to the API call
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        SumsApiService sumsApiService = retrofit.create(SumsApiService.class);
        String username = prefs.getString("username", "");
        String otp = prefs.getString("otp", "");
        call = sumsApiService.getEquipmentList(8, username, otp);
        call.enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                equipmentList = response.body();

                // Make list of all machine names for adding to the spinner and types
                machineTypes = new HashSet<>();
                for (Equipment m : equipmentList) {
                    if (!m.getLocationName().equals("")) {
                        machineTypes.add(m.getLocationName());
                    }
                }

                ArrayList<String> types = new ArrayList<>(machineTypes);
                Collections.sort(types);
                ArrayAdapter<String> machineTypeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, types);
                machineTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                machineTypeSpinner.setAdapter(machineTypeAdapter);
            }

            @Override
            /**
             * Show a toast if there is a problem connecting with the API
             */
            public void onFailure(Call<List<Equipment>> call, Throwable throwable) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "An Error Occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Method to send general feedback to our server
     * @param feedback - General Feedback object being sent to the server
     */
    public void connectAndSendGeneralFeedback(GeneralFeedback feedback) {

        // Create retrofit to send data to the server
        // Get header info and pass it
        retrofit = new Retrofit.Builder()
                .baseUrl("https://is-apps.me.gatech.edu/api/v1-0/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        ServerApiService serverApiService = retrofit.create(ServerApiService.class);
        String username = prefs.getString("username", "");
        String otp = prefs.getString("otp", "");
        call = serverApiService.sendGeneralFeedback(feedback, "771e6dd7-2d2e-4712-8944-7055ce69c9fb", Credentials.basic(username, otp));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {

                        // Show a dialog to tell the user the feedback was successfully sent
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(response.body().string());
                        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.fragment_container, new FeedbackFragment());
                                transaction.commit();
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } else {
                        // Show a toast to tell the user that the feedback was not submitted correctly
                        Toast.makeText(getActivity(), response.message(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                // Tell user that feedback was not sent
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "An Error Occurred Recording Feedback, Try Again Later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * method to send PI feedback to the server
     * @param feedback - PI feedback to send to the server
     */
    public void connectAndSendPIFeedback(StaffFeedback feedback) {

        // Create retrofit to send data to the server
        // Get header info and pass it
        retrofit = new Retrofit.Builder()
                .baseUrl("https://is-apps.me.gatech.edu/api/v1-0/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        ServerApiService serverApiService = retrofit.create(ServerApiService.class);
        String username = prefs.getString("username", "");
        String otp = prefs.getString("otp", "");
        call = serverApiService.sendStaffFeedback(feedback, "771e6dd7-2d2e-4712-8944-7055ce69c9fb", Credentials.basic(username, otp));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {

                        // Show a dialog to tell the user the feedback was successfully sent
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(response.body().string());
                        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.fragment_container, new FeedbackFragment());
                                transaction.commit();
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } else {
                        // Show a toast to tell the user that the feedback was not submitted correctly
                        Toast.makeText(getActivity(), response.message(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                // Tell user that feedback was not sent
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "An Error Occurred Recording Feedback, Try Again Later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Method to send tool broken feedback to our server
     * @param feedback - tool broken feedback to send to server
     */
    public void connectAndSendToolFeedback(ToolBrokenFeedback feedback) {

        // Create retrofit to send data to the server
        // Get header info and pass it
        retrofit = new Retrofit.Builder()
                .baseUrl("https://is-apps.me.gatech.edu/api/v1-0/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        ServerApiService serverApiService = retrofit.create(ServerApiService.class);
        String username = prefs.getString("username", "");
        String otp = prefs.getString("otp", "");
        call = serverApiService.sendToolFeedback(feedback, "771e6dd7-2d2e-4712-8944-7055ce69c9fb", Credentials.basic(username, otp));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        // Show a dialog to tell the user the feedback was successfully sent
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(response.body().string());
                        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.fragment_container, new FeedbackFragment());
                                transaction.commit();
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        // Show a toast to tell the user that the feedback was not submitted correctly
                        Toast.makeText(getActivity(), response.message(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                // Tell user that feedback was not submitted
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "An Error Occurred Recording Feedback, Try Again Later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Show toast
     * @param dialogText text of the toast
     */
    public void showToast(String dialogText) {
        Toast.makeText(getActivity(), dialogText, Toast.LENGTH_SHORT).show();
    }
}
