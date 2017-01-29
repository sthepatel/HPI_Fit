package info.smitpatel.hpifit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import info.smitpatel.hpifit.app.HPIApp;
import info.smitpatel.hpifit.db.DatabaseHandler;
import info.smitpatel.hpifit.models.Progress;
import info.smitpatel.hpifit.models.UserProfile;
import info.smitpatel.hpifit.services.StepService;
import info.smitpatel.hpifit.widgets.AutoResizeTextView;
import info.smitpatel.hpifit.widgets.CircleImageView;

public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    private static final String PREFIX = HomeActivity.class.getSimpleName() + ": ";

    private Context context;

    private UserProfile userProfile;

    private CircleImageView circleImgViewProfilePic;
    private CircleImageView circleImgViewCamera;

    private Switch switchStepCounter;

    private LinearLayout layoutStepsToday;
    private AutoResizeTextView txtViewStepsCountToday;
    private TextView txtStepsYesterdayValue;

    private LinearLayout layoutMilestones;
    private AutoResizeTextView txtViewMilestonesCountToday;
    private TextView txtMilestonesYesterdayValue;

    private LinearLayout layoutProgress;

    private ArrayList<Progress> progressHistory;

    private SharedPreferences userSharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener userPreferenceChangeListener;

    @Override
    protected void onResume() {
        super.onResume();
        HPIApp.logger(PREFIX, "onResume()", Log.INFO);
        registerPrefsListener();

        if (Util.runService()) {
            switchStepCounter.setChecked(true);
            switchStepCounter.setText(R.string.turn_off_step_counter);
            HPIApp.getAppContext().startService(new Intent(HPIApp.getAppContext(), StepService.class));
        } else {
            switchStepCounter.setChecked(false);
            switchStepCounter.setText(R.string.turn_on_step_counter);
            HPIApp.getAppContext().stopService(new Intent(HPIApp.getAppContext(), StepService.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        HPIApp.logger(PREFIX, "onPause()", Log.INFO);
        unregisterPrefsListener();
    }

    @Override
    public void onBackPressed() {
        HPIApp.logger(PREFIX, "onBackPressed()", Log.INFO);

        if (!closeDrawer()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        HPIApp.logger(PREFIX, "onActivityResult()", Log.INFO);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == HPIApp.Prefs.CODE_IMAGE_SELECT) {
            if (data.getData() == null) {
                // this means a new picture was taken
                final Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap bitmap = extras.getParcelable("data");
                    if (circleImgViewProfilePic != null) {
                        circleImgViewProfilePic.setImageBitmap(bitmap);
                    }
                }
            } else {
                // an existing picture was selected
                setProfilePic(circleImgViewProfilePic, data.getData());
                return;
            }

            if (circleImgViewProfilePic != null) {
                Bitmap bitmap = circleImgViewProfilePic.getBitmap();
                String path = saveProfilePicLocally(bitmap);
                DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);
                databaseHandler.saveUserProfilePic(userProfile.getUsername(), path);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HPIApp.logger(PREFIX, "onCreate()", Log.INFO);
        setContentView(R.layout.activity_home);

        context = this;
        userProfile = (UserProfile) getIntent().getSerializableExtra(HPIApp.Prefs.BUNDLE_KEY_USER_PROFILE);

        userSharedPreferences = Util.getUserSharedPreferences();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationViewHandler(navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        switchStepCounter = (Switch) findViewById(R.id.switchStepCounter);
        switchStepCounter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    switchStepCounter.setText(R.string.turn_off_step_counter);
                    Util.updateRunService(true);
                } else {
                    switchStepCounter.setText(R.string.turn_on_step_counter);
                    Util.updateRunService(false);
                }
            }
        });

        // just to not block the thread -- data is not very important at the moment
        new GetYesterdaysData().execute();
        new GetProgressHistory().execute();

        stepsLayoutHandler();
        milestonesLayoutHandler();
        progressLayoutHandler();
    }

    /**
     * initializes first (top) steps layout
     */
    private void stepsLayoutHandler() {
        layoutStepsToday = (LinearLayout) findViewById(R.id.layoutStepsToday);
        txtViewStepsCountToday = (AutoResizeTextView) findViewById(R.id.txtViewStepsCountToday);
        txtStepsYesterdayValue = (TextView) findViewById(R.id.txtStepsYesterdayValue);

        layoutStepsToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        txtViewStepsCountToday.setText(String.valueOf(userSharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, 0)));
        txtStepsYesterdayValue.setText("-");
    }

    /**
     * initializes second (middle) milestones layout
     */
    private void milestonesLayoutHandler() {
        layoutMilestones = (LinearLayout) findViewById(R.id.layoutMilestones);
        txtViewMilestonesCountToday = (AutoResizeTextView) findViewById(R.id.txtViewMilestonesCountToday);
        txtMilestonesYesterdayValue = (TextView) findViewById(R.id.txtMilestonesYesterdayValue);

        layoutMilestones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        txtViewMilestonesCountToday.setText(String.valueOf(userSharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, 0)));
        txtMilestonesYesterdayValue.setText("-");
    }

    /**
     * initializes third (bottom) progress layout
     */
    private void progressLayoutHandler() {
        layoutProgress = (LinearLayout) findViewById(R.id.layoutProgress);

        layoutProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
                bottomSheetDialog.setCancelable(true);
                bottomSheetDialog.setCanceledOnTouchOutside(true);
                bottomSheetDialog.setContentView(getProgressView(bottomSheetDialog));
                bottomSheetDialog.show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        HPIApp.logger(PREFIX, "onNavigationItemSelected()", Log.INFO);

        switch (item.getItemId()) {
            case R.id.nav_profile:
                closeDrawer();
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                bottomSheetDialog.setCancelable(true);
                bottomSheetDialog.setCanceledOnTouchOutside(true);
                bottomSheetDialog.setContentView(getProfileView(bottomSheetDialog));
                bottomSheetDialog.show();
                break;
            case R.id.nav_settings:
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_share:
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_rate:
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_logout:
                closeDrawer();
                logoutHandler();
                break;
            default:
                break;
        }

        return false;
    }

    /**
     * initializes views within NavigationView
     * @param navigationView NavigationView
     */
    private void navigationViewHandler(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            circleImgViewProfilePic = (CircleImageView) headerView.findViewById(R.id.circleImgViewProfilePic);
            circleImgViewCamera = (CircleImageView) headerView.findViewById(R.id.circleImgViewCamera);
            TextView txtUserFullName = (TextView) headerView.findViewById(R.id.txtUserFullName);
            setProfilePic(circleImgViewProfilePic, null);

            circleImgViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    profilePicImageChooser();
                }
            });

            txtUserFullName.setText(userProfile.getFirstName() + " " + userProfile.getLastName());
        }
    }

    /**
     * closes the side drawer, if open
     * @return true if closed successfully, false is its already closed
     */
    private boolean closeDrawer() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        return false;
    }

    /**
     * register a listener to User's SharedPreferences which allows live steps and milestones update
     */
    private void registerPrefsListener() {
        if (userSharedPreferences == null) {
            userSharedPreferences = Util.getUserSharedPreferences();
        }
        userPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                switch (key) {
                    case HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS:
                        int steps = sharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, 0);
                        txtViewStepsCountToday.setText(String.valueOf(steps));
                        break;
                    case HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY:
                        int milestones = sharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, 0);
                        txtViewMilestonesCountToday.setText(String.valueOf(milestones));
                    case HPIApp.Prefs.SHARED_KEY_RUN_SERVICE:
                        boolean runService = sharedPreferences.getBoolean(HPIApp.Prefs.SHARED_KEY_RUN_SERVICE, false);
                        if (runService) {
                            HPIApp.getAppContext().startService(new Intent(HPIApp.getAppContext(), StepService.class));
                        } else {
                            HPIApp.getAppContext().stopService(new Intent(HPIApp.getAppContext(), StepService.class));
                        }
                    default:
                        break;
                }
            }
        };

        userSharedPreferences.registerOnSharedPreferenceChangeListener(userPreferenceChangeListener);
    }

    /**
     * unregister SharedPreferences listener, used when leaving the activity
     */
    private void unregisterPrefsListener() {
        if (userSharedPreferences != null && userPreferenceChangeListener != null) {
            userSharedPreferences.unregisterOnSharedPreferenceChangeListener(userPreferenceChangeListener);
        }
    }

    /**
     * Alert the user and if user complies, perform a logout and stops recording steps by stopping the service
     */
    private void logoutHandler() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog);
        builder.setTitle("Logout?");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Util.setLoggedInFlag(false);

                HPIApp.getAppContext().stopService(new Intent(HPIApp.getAppContext(), StepService.class));

                Intent intent = new Intent(HomeActivity.this, SplashActivity.class);
                startActivity(intent);
                HomeActivity.this.finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    /**
     * shows a BottomSheet which allows users to choose from multiple image selection option
     */
    private void profilePicImageChooser() {
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String pickTitle = "Select or take a new Picture";
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        startActivityForResult(chooserIntent, HPIApp.Prefs.CODE_IMAGE_SELECT);
    }

    /**
     * sets profile pic from the provided URI. If URI is empty, it will take the data from UserProfile
     * @param circleImageView user profile image view
     * @param uri uri that represents the image
     */
    private void setProfilePic(CircleImageView circleImageView, Uri uri) {
        if (uri == null && userProfile != null) {
            File file = new File(userProfile.getProfilePic());
            if(file.exists()) {
                uri = Uri.fromFile(file);
            } else {
                HPIApp.logger(PREFIX, "profilePic file does not exist", Log.DEBUG);
                return;
            }
        }

        if (uri == null) {
            return;
        }

        try {
            InputStream input = getContentResolver().openInputStream(uri);
            final Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (circleImageView != null) {
                circleImageView.setImageBitmap(bitmap);
            }
        } catch (FileNotFoundException e) {
            HPIApp.logger(PREFIX, e.toString(), Log.ERROR);
        }
    }

    /**
     * saves the bitmap to internal storage for future use, saved as the username
     * @param bitmap user profile image
     * @return full path of the newly saved file
     */
    private String saveProfilePicLocally(Bitmap bitmap) {
        FileOutputStream out = null;
        File file = new File(HPIApp.getAppContext().getFilesDir(), userProfile.getUsername());
        boolean success = false;

        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            success = true;
        } catch (Exception e) {
            HPIApp.logger(PREFIX, e.toString(), Log.ERROR);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                HPIApp.logger(PREFIX, e.toString(), Log.ERROR);
            }
        }

        if (success) {
            return file.getPath();
        } else {
            return "";
        }
    }

    /**
     * gets users yesterdays data in background
     */
    private class GetYesterdaysData extends AsyncTask<String, Integer, Progress> {
        GetYesterdaysData() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Progress doInBackground(String... params) {
            String username = Util.getUsername();
            String date = Util.getPrevDate(-1);

            DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);

            return databaseHandler.getDayProgress(username, date);
        }

        @Override
        protected void onPostExecute(Progress progress) {
            super.onPostExecute(progress);

            if (progress != null) {
                txtStepsYesterdayValue.setText(progress.getSteps());
                txtMilestonesYesterdayValue.setText(progress.getMilestones_day());
            }
        }
    }

    private class GetProgressHistory extends AsyncTask<String, Integer, ArrayList<Progress>> {
        GetProgressHistory() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Progress> doInBackground(String... params) {
            String username = Util.getUsername();

            DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);
            return databaseHandler.getHistoryByUser(username);
        }

        @Override
        protected void onPostExecute(ArrayList<Progress> progresses) {
            super.onPostExecute(progresses);

            if (progresses != null && progresses.size() > 0) {
                progressHistory = progresses;
            } else {
                progressHistory = generateFakeProgressHistory();
            }
        }
    }

    /**
     * this will generate a fake progress history for last 5 days for a user
     * @return ArrayList of Progress that represents last 5 days
     */
    private ArrayList<Progress> generateFakeProgressHistory() {
        ArrayList<Progress> history = new ArrayList<>();

        Progress progress;

        int prevDay = -1;

        for (int i = 0; i < 5; i++) {
            progress = new Progress();
            progress.setUsername(Util.getUsername());
            progress.setDate(Util.getPrevDate(prevDay));
            progress.setSteps("1467");
            progress.setMilestones_day("3");
            history.add(progress);
            prevDay--;
        }

        return history;
    }

    /**
     * Gets the profile view that can be used for BottomSheetDialog
     * @param dialog BottomSheetDialog
     * @return profile view
     */
    private View getProfileView(final BottomSheetDialog dialog) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View profileView = inflater.inflate(R.layout.dialog_profile, null, false);

        ImageButton imgBtnProfileClose = (ImageButton) profileView.findViewById(R.id.imgBtnProfileClose);

        CircleImageView circleImgViewProfilePic = (CircleImageView) profileView.findViewById(R.id.circleImgViewProfilePic);
        TextView txtProfileName = (TextView) profileView.findViewById(R.id.txtProfileName);

        TextView txtProfileUsernameValue = (TextView) profileView.findViewById(R.id.txtProfileUsernameValue);
        TextView txtProfileAchievementsValue = (TextView) profileView.findViewById(R.id.txtProfileAchievementsValue);
        TextView txtProfileLanguageValue = (TextView) profileView.findViewById(R.id.txtProfileLanguageValue);

        setProfilePic(circleImgViewProfilePic, null);

        txtProfileName.setText(userProfile.getFirstName() + " " + userProfile.getLastName());
        txtProfileUsernameValue.setText(userProfile.getUsername());
        txtProfileAchievementsValue.setText(String.valueOf(userProfile.getMilestonesCount()));

        // FIXME: 1/27/17 hardcoding language - temp
        txtProfileLanguageValue.setText("English");

        imgBtnProfileClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        return profileView;
    }

    /**
     * Gets the progress view that can be used for BottomSheetDialog
     * @param dialog BottomSheetDialog
     * @return progress view
     */
    private View getProgressView(final BottomSheetDialog dialog) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_progress, null, false);

        ImageButton imgBtnProgressClose = (ImageButton) view.findViewById(R.id.imgBtnProgressClose);
        RecyclerView recycleViewProgress = (RecyclerView) view.findViewById(R.id.recycleViewProgress);
        recycleViewProgress.setLayoutManager(new LinearLayoutManager(context));
        ProgressAdapter progressAdapter = new ProgressAdapter(context, progressHistory);
        recycleViewProgress.setAdapter(progressAdapter);

        imgBtnProgressClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        return view;
    }

    /**
     * Adapter used for progress RecyclerView
     */
    private class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ViewHolder> {
        private Context context;
        private ArrayList<Progress> items;

        ProgressAdapter(Context context, ArrayList<Progress> items) {
            this.context = context;
            this.items = items;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView txtProgressDate;
            final TextView txtProgressSteps;
            final TextView txtProgressMilestones;

            ViewHolder(View itemView) {
                super(itemView);
                txtProgressDate = (TextView) itemView.findViewById(R.id.txtProgressDate);
                txtProgressSteps = (TextView) itemView.findViewById(R.id.txtProgressSteps);
                txtProgressMilestones = (TextView) itemView.findViewById(R.id.txtProgressMilestones);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_list_item, parent, false);
            return new ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final Progress progressItem = items.get(position);

            holder.txtProgressDate.setText(progressItem.getDate());
            holder.txtProgressSteps.setText(progressItem.getSteps() + " steps");
            holder.txtProgressMilestones.setText(progressItem.getMilestones_day() + " milestones");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

//        void updateAdapterData(ArrayList<Progress> items) {
//            items = new ArrayList<>(items);
//            notifyDataSetChanged();
//        }
    }
}
