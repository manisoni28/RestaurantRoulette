package com.physphil.android.restaurantroulette;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import com.physphil.android.restaurantroulette.util.Constants;

/**
 * Created by pshadlyn on 2/24/14.
 */
public class RestaurantActivity extends RRActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_fragment);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_edit_restaurant);
        setActionBarFont(Constants.FONT_DEFAULT);

        String id = getIntent().getStringExtra(RestaurantFragment.EXTRA_RESTAURANT_ID);

        // Attempt to find previously existing fragment.  If not found create new one.
        FragmentManager fm = getSupportFragmentManager();
        RestaurantFragment fragment = (RestaurantFragment) fm.findFragmentById(R.id.fragment_container);

        if(fragment == null){
            fragment = RestaurantFragment.newInstance(id);
        }

        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
