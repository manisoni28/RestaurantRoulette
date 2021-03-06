/**
 * Restaurant Roulette for Android
 * Copyright (C) 2014  Phil Shadlyn
 *
 * Restaurant Roulette is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @copyright 2014 Phil Shadlyn - physphil@gmail.com
 * @license GNU General Public License - https://www.gnu.org/licenses/gpl.html
 */

package com.physphil.android.restaurantroulette;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.physphil.android.restaurantroulette.data.DatabaseHelper;
import com.physphil.android.restaurantroulette.ui.CustomFontDialogBuilder;
import com.physphil.android.restaurantroulette.util.Constants;

public class MainActivity extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private SharedPreferences prefs;
    private boolean newListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
//        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onResume() {
        super.onResume();

        setActionBarFont(Constants.FONT_DEFAULT);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.container);

        switch(position){

            case 0:
                mTitle = getString(R.string.title_restaurant_selector);
                saveMenuSelection(0);

                if(!(fragment instanceof RestaurantSelectorFragment)){
                    fragment = new RestaurantSelectorFragment();
                }

                fm.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                break;

            case 1:
                mTitle = getString(R.string.title_restaurant_list);
                saveMenuSelection(1);
                newListFragment = false;

                if(!(fragment instanceof RestaurantListFragment)){
                    fragment = new RestaurantListFragment();
                    newListFragment = true;
                }

                fm.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                break;

            case 2:
                mTitle = getString(R.string.title_restaurant_history);
                saveMenuSelection(2);

                if(!(fragment instanceof HistoryListFragment)){
                    fragment = new HistoryListFragment();
                }

                fm.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                break;
        }
    }

    /**
     * This is called once selected fragment is loaded and drawer is closed
     */
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);

        if(mTitle != null) {
            actionBar.setTitle(mTitle);
        }

        // Set up filtering in action bar in RestaurantListFragment
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.container);

        if(fragment instanceof RestaurantListFragment){

            ((RestaurantListFragment) fragment).setupListFiltering();
        }
    }

    private void confirmClearRestaurantHistory(){

//        new AlertDialog.Builder(this)
        new CustomFontDialogBuilder(this)
                .setTitle(R.string.dialog_delete_history_title)
                .setMessage(R.string.dialog_delete_history_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearRestaurantHistory();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .show();
    }

    private void clearRestaurantHistory(){

        DatabaseHelper.getInstance(this).deleteRestaurantHistory();
        Intent i = new Intent(HistoryListFragment.ACTION_HISTORY_CLEARED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void saveMenuSelection(int selection){

        prefs.edit()
                .putInt(NavigationDrawerFragment.PREF_SELECTED_MENU_ITEM, selection)
                .commit();
    }

    /**
     * Send email to developer
     */
    private void emailDeveloper(){

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[] {Constants.DEVELOPER_EMAIL_ADDRESS});
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));
        startActivity(i);
    }

    /**
     * Open Play Store to rate app
     */
    public void rateApp(){
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToPlayStore = new Intent(Intent.ACTION_VIEW, uri);
        try{
            startActivity(goToPlayStore);
        }
        catch(ActivityNotFoundException e){
            Toast.makeText(this, getString(R.string.toast_google_play_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()){

            case R.id.menu_add_restaurant:
                startActivity(RestaurantActivity.getLaunchingIntent(this, null));
                return true;

            case R.id.menu_clear_restaurant_history:
                confirmClearRestaurantHistory();
                return true;

            case R.id.menu_email_developer:
                emailDeveloper();
                return true;

            case R.id.menu_rate_app:
                rateApp();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
