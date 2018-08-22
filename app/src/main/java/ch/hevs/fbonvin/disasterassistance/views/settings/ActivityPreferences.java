package ch.hevs.fbonvin.disasterassistance.views.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import ch.hevs.fbonvin.disasterassistance.R;
import ch.hevs.fbonvin.disasterassistance.views.activities.ActivityAboutApplication;
import ch.hevs.fbonvin.disasterassistance.views.activities.ActivityNetworkStatus;

public class ActivityPreferences extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        private Activity mActivity;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            mActivity = this.getActivity();

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_user_name)));

            Preference btNetwork = findPreference(getString(R.string.key_pref_network_status));
            btNetwork.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(MainPreferenceFragment.this.getActivity(), ActivityNetworkStatus.class);
                    startActivity(intent);
                    return true;
                }
            });

            Preference btAbout = findPreference(getString(R.string.key_pref_about_app));
            btAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(MainPreferenceFragment.this.getActivity(), ActivityAboutApplication.class);
                    startActivity(intent);
                    return true;
                }
            });

//            final SwitchPreference btNotif = (SwitchPreference) findPreference(this.getResources().getString(R.string.key_pref_notification_app));
//            btNotif.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object o) {
//                    if (btNotif.isChecked()) {
//                        Toast.makeText(mActivity, "Unchecked", Toast.LENGTH_SHORT).show();
//                        btNotif.setChecked(false);
//                    } else {
//                        Toast.makeText(mActivity, "Checked", Toast.LENGTH_SHORT).show();
//                        btNotif.setChecked(true);
//
//                        NotificationCompat.Builder b = null;
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                            b = new NotificationCompat.Builder(getContext(), "M_CH_ID");
//                        }
//                        b.setAutoCancel(true)
//                                .setDefaults(NotificationCompat.DEFAULT_ALL)
//                                .setWhen(System.currentTimeMillis())
//                                .setSmallIcon(R.drawable.logo_git)
//                                .setTicker("{your tiny message}")
//                                .setContentTitle("New message")
//                                .setContentText("A new message is available in your area")
//                                .setContentInfo("INFO");
//
//                        NotificationManager nm = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
//                        nm.notify(1, b.build());
//                    }
//                    return false;
//                }
//            });
        }
    }


    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            String val = newValue.toString();

            if (preference instanceof ListPreference) {

                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(val);

                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
                preference.setSummary(val);
            } else if (preference instanceof EditTextPreference) {
                preference.setSummary(val);
            }

            return true;
        }
    };


    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * Make the back button of the action bar behave as the hardware button
     * @param item menu item pressed
     * @return true if ok, false if MenuItem not handled by method
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return false;
    }
}
