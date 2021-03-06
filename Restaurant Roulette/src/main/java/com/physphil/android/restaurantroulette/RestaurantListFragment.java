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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.physphil.android.restaurantroulette.data.DatabaseHelper;
import com.physphil.android.restaurantroulette.models.Restaurant;
import com.physphil.android.restaurantroulette.ui.CustomFontArrayAdapter;
import com.physphil.android.restaurantroulette.ui.CustomFontDialogBuilder;
import com.physphil.android.restaurantroulette.ui.RestaurantListAdapter;
import com.physphil.android.restaurantroulette.util.Constants;
import com.physphil.android.restaurantroulette.util.Util;

import java.util.List;

/**
 * Show list of restaurants stored in user database
 * Created by pshadlyn on 2/24/14.
 */
public class RestaurantListFragment extends ListFragment {

    public static final String PREFS_GENRE_FILTER_LIST = "genre_filter_list";
    public static final String PREFS_SHOW_HELP_RESTAURANT_LIST = "show_help_restaurant_list";

    private DatabaseHelper mDatabaseHelper;
    private List<Restaurant> mRestaurants;
    private RestaurantListAdapter mAdapter;
    private int mFilter;
    private SharedPreferences mPrefs;
    private Typeface mTf;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDatabaseHelper = DatabaseHelper.getInstance(getActivity());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mTf = Typeface.createFromAsset(getActivity().getAssets(), Constants.FONT_DEFAULT);

        // Register broadcast receivers. Need to happen here as receivers need to be active while detail fragment is updating
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mLifetimeReceiver,
                new IntentFilter(RestaurantFragment.ACTION_RESTAURANT_UPDATED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_restaurant_list, container, false);

        // Set font for text when listview is empty
        ((TextView) v.findViewById(android.R.id.empty)).setTypeface(mTf);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getListView().setDivider(null);

        mFilter = mPrefs.getInt(PREFS_GENRE_FILTER_LIST, Restaurant.GENRE_ALL);

        updateRestaurantListView();
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mVisibleReceiver, new IntentFilter(RestaurantFragment.ACTION_DELETE_RESTAURANT));
        lbm.registerReceiver(mVisibleReceiver, new IntentFilter(NavigationDrawerFragment.ACTION_DRAWER_CLOSED));
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mVisibleReceiver);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mLifetimeReceiver);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id){

        String restaurantId = mRestaurants.get(position).getRestaurantId();
        viewRestaurantDetail(restaurantId);
    }

    /**
     * Start activity to view restaurant information
     * @param id database id of restaurant to view, or null if a new restaurant
     */
    private void viewRestaurantDetail(String id){

        Intent i = RestaurantActivity.getLaunchingIntent(getActivity(), id);
        startActivity(i);
    }

    /**
     * Prompt user to confirm deletion of all restaurants saved in database
     */
    private void confirmDeleteAllRestaurants(){

//        new AlertDialog.Builder(getActivity())
        new CustomFontDialogBuilder(getActivity())
                .setTitle(R.string.dialog_delete_all_restaurants_title)
                .setMessage(R.string.dialog_delete_all_restaurants_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which){

                        deleteAllRestaurants();
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

    /**
     * Delete all restaurants in database
     */
    private void deleteAllRestaurants(){

        // delete from db
        mDatabaseHelper.deleteAllRestaurants();

        // clear adapter
        mRestaurants.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Delete restaurant from database and adapter
     * @param id id of restaurant to delete
     */
    private void deleteRestaurant(String id){

        // delete from db
        mDatabaseHelper.deleteRestaurantById(id);

        // find in adapter, delete and refresh
        int index = getIndex(id);

        if(index >= 0){
            mRestaurants.remove(index);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Update list of restaurants from database
     */
    private void updateRestaurantListView(){

        if(mFilter == Restaurant.GENRE_ALL){
            mRestaurants = mDatabaseHelper.getAllRestaurants();
        }
        else{
            // Filter by genre. Need to subtract 1 from index as index 0 is added to array and reserved for All Restaurants (no filtering)
            String genres[] = getResources().getStringArray(R.array.genres);
            mRestaurants = mDatabaseHelper.getRestaurantsByGenre(genres[mFilter - 1]);
        }

        // Need to replace adapter as mRestaurants is a new object. Adapter is still using old object, which no longer exists.
        mAdapter = new RestaurantListAdapter(getActivity(), mRestaurants);
        setListAdapter(mAdapter);
    }

    /**
     * Set up filtering navigation in action bar, and display list view with results from filtering
     */
    public void setupListFiltering(){

        List<String> genres = Restaurant.getGenresForAdapter(getActivity());

        // Override adapter to set font
        final SpinnerAdapter adapter = new CustomFontArrayAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, genres);

        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int i, long l) {

                // Save filter
                mFilter = i;
                mPrefs.edit()
                        .putInt(PREFS_GENRE_FILTER_LIST, i)
                        .commit();

                // Refresh list
                updateRestaurantListView();

                return true;
            }
        });

        actionBar.setSelectedNavigationItem(mFilter);
    }

    /**
     * Get index of Restaurant object according to id
     * @param id id of restaurant to find
     * @return index of restaurant in mRestaurants, -1 if not found
     */
    private int getIndex(String id){

        for(int i = 0; i < mRestaurants.size(); i++){

            if(mRestaurants.get(i).getRestaurantId().equals(id)){

                return i;
            }
        }

        return -1;
    }

    private void showHelpDialog(){

        Util.showHelpDialog(getActivity(), R.string.title_restaurant_list, R.string.dialog_restaurant_list_help, PREFS_SHOW_HELP_RESTAURANT_LIST);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.restaurant_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch(item.getItemId()){

            case R.id.menu_add_restaurant:
                viewRestaurantDetail(null);
                return true;

            case R.id.menu_delete_all_restaurants:
                confirmDeleteAllRestaurants();
                return true;

            case R.id.menu_help:
                showHelpDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Receiver to catch all broadcasts for the lifetime of this fragment
     */
    private BroadcastReceiver mLifetimeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(RestaurantFragment.ACTION_RESTAURANT_UPDATED)){

                /** Need to reset list adapter as projects are filtered and sorted alphabetically. Can't just add to adapter and call
                 * onNotifyDataSetChanged, as new entries would be out of place.
                 */
                updateRestaurantListView();
            }
        }
    };

    /**
     * Receiver to catch all broadcasts while fragment is visible
     */
    private BroadcastReceiver mVisibleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(RestaurantFragment.ACTION_DELETE_RESTAURANT)){

                String id = intent.getStringExtra(RestaurantFragment.EXTRA_RESTAURANT_ID);

                if(id != null){

                    deleteRestaurant(id);
                }
            }
            else if(intent.getAction().equals(NavigationDrawerFragment.ACTION_DRAWER_CLOSED)){

                // Show help dialog if never been shown before
                boolean showHelp = mPrefs.getBoolean(PREFS_SHOW_HELP_RESTAURANT_LIST, true);
                if(showHelp){

                    showHelpDialog();
                }
            }
        }
    };

}