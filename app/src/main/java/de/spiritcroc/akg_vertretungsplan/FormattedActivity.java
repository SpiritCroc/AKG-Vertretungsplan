/*
 * Copyright (C) 2015-2016 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
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
 */

package de.spiritcroc.akg_vertretungsplan;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class FormattedActivity extends NavigationDrawerActivity implements FormattedFragment.OnFragmentInteractionListener{
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private static ViewPager viewPager;
    private static String plan1, plan2, title1, title2;
    private TextView textView, title1View, title2View;
    private SharedPreferences sharedPreferences;
    private static FormattedFragment fragment1, fragment2;
    private static Calendar date1;
    private static boolean shortCutToPageTwo = false, filteredMode;
    private MenuItem reloadItem, filterItem, markReadItem;
    private ImageView overflow;
    private boolean landscape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_formatted);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString(Keys.TEXT_VIEW_TEXT, getString(R.string.welcome)));
        title1 = sharedPreferences.getString(Keys.CURRENT_TITLE_1, getString(R.string.today));
        plan1 = sharedPreferences.getString(Keys.CURRENT_PLAN_1, "");
        title2 = sharedPreferences.getString(Keys.CURRENT_TITLE_2, getString(R.string.tomorrow));
        plan2 = sharedPreferences.getString(Keys.CURRENT_PLAN_2, "");
        try {
            date1 = Tools.getDateFromPlanTitle(title1);
        }
        catch (Exception e){
            Log.e("FormattedActivity", "Got error while trying to extract date1 from the titles: " + e);
            date1 = null;//Deactivate date functionality
        }

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (landscape) {
            title1View = (TextView) findViewById(R.id.title_1);
            title2View = (TextView) findViewById(R.id.title_2);
            if (Tools.isLightStyle(getStyle())) {
                title1View.setTextColor(getResources().getColor(R.color.title_color_light));
                title2View.setTextColor(getResources().getColor(R.color.title_color_light));
            } else if (Tools.isDarkStyle(getStyle())) {
                title1View.setTextColor(getResources().getColor(R.color.title_color_dark));
                title2View.setTextColor(getResources().getColor(R.color.title_color_dark));
            }
            title1View.setText(title1);
            title2View.setText(title2);
            LinearLayout plan1Layout = (LinearLayout) findViewById(R.id.plan_1_layout);
            LinearLayout plan2Layout = (LinearLayout) findViewById(R.id.plan_2_layout);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragment1 = FormattedFragment.newInstance(1);
            fragment2 = FormattedFragment.newInstance(2);
            fragmentManager.beginTransaction().add(plan1Layout.getId(), fragment1)
                    .add(plan2Layout.getId(), fragment2)
                    .commit();
        } else {
            viewPager = (ViewPager) findViewById(R.id.pager);

            // Fix pager tab strip not showing using support libraries 24.0.0 (https://code.google.com/p/android/issues/detail?id=213359)
            ((ViewPager.LayoutParams) findViewById(R.id.pager_tab_strip).getLayoutParams()).isDecor = true;

            fragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
            viewPager.setAdapter(fragmentPagerAdapter);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));

        initDrawer();
    }
    @Override
    public boolean afterDisclaimer(Bool startedDownloadService){
        if (super.afterDisclaimer(startedDownloadService))//only run once
            return true;
        if (!startedDownloadService.value) {
            String text = sharedPreferences.getBoolean(Keys.ILLEGAL_PLAN, false) ? getString(R.string.error_illegal_plan) : getString(R.string.last_checked) + " " + sharedPreferences.getString(Keys.LAST_CHECKED, getString(R.string.error_unknown));
            textView.setText(text);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Keys.TEXT_VIEW_TEXT, text);
            editor.apply();
        }
        //Download plan stuff end

        if (date1 != null && sharedPreferences.getBoolean(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY, true)) {//Try to show most relevant day
            Calendar currentDate = Calendar.getInstance();
            if (currentDate.after(date1)) {
                if (currentDate.get(Calendar.DAY_OF_MONTH) != date1.get(Calendar.DAY_OF_MONTH)
                        || currentDate.get(Calendar.MONTH) != date1.get(Calendar.MONTH)
                        || currentDate.get(Calendar.YEAR) != date1.get(Calendar.YEAR))
                    shortCutToPageTwo = true;
                else {
                    try {
                        int currentHour = currentDate.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = currentDate.get(Calendar.MINUTE);
                        int settingHour = Integer.parseInt(sharedPreferences.getString(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME, "17"));
                        int settingMinute = sharedPreferences.getInt(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
                        if (currentHour > settingHour || (currentHour == settingHour && currentMinute >= settingMinute)) {
                            shortCutToPageTwo = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return false;
    }
    @Override
    public void onResume() {
        super.onResume();
        Tools.setUnseenFalse(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);


        try{
            ContentValues contentValues = new ContentValues();
            contentValues.put("tag", "de.spiritcroc.akg_vertretungsplan/.FormattedActivity");
            contentValues.put("count", 0);
            getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), contentValues);
        }
        catch (IllegalArgumentException e){
            Log.d("FormattedActivity", "TeslaUnread is not installed");
        }
        catch (Exception e){
            Log.e("FormattedActivity", "Got exception while trying to sending count to TeslaUnread: " + e);
        }

        IsRunningSingleton.getInstance().registerActivity(this);

        if (filterItem != null)
            filterItem.setShowAsAction(sharedPreferences.getBoolean(Keys.SHOW_FILTERED_PLAN_AS_ACTION, false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
        if (reloadItem != null)
            reloadItem.setVisible(!sharedPreferences.getBoolean(Keys.HIDE_ACTION_RELOAD, false));
        if (markReadItem != null)
            markReadItem.setShowAsAction(sharedPreferences.getBoolean(Keys.SHOW_MARK_READ_AS_ACTION, false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);

        //Apply color settings:
        setActionBarColor();

        if (fragment1 != null)
            fragment1.reloadContent(plan1, title1);
        if (fragment2 != null)
            fragment2.reloadContent(plan2, title2);

        textView.setVisibility(sharedPreferences.getBoolean(Keys.HIDE_TEXT_VIEW, false) ? View.GONE : View.VISIBLE);

        if (sharedPreferences.getBoolean(Keys.RELOAD_ON_RESUME, false))
            startDownloadService(false);
        BReceiver.setWidgetUpdateAlarm(this);
    }
    @Override
    protected void onPause(){
        super.onPause();
        IsRunningSingleton.getInstance().unregisterActivity(this);
    }
    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if ((!landscape && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) ||
                (landscape && newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            // Orientation changed

            // Don't save fragments onSaveInstanceState
            getSupportFragmentManager().beginTransaction().remove(fragment1).remove(fragment2).commit();
            getSupportFragmentManager().executePendingTransactions();

            // Restart
            finish();
            overridePendingTransition(0, 0);
            startActivity(getIntent());
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        menu.findItem(R.id.action_filter_plan).setVisible(LessonPlan.getInstance(sharedPreferences).isConfigured());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_formatted, menu);

        if (menu != null) {
            reloadItem = menu.findItem(R.id.action_reload_web_view);
            reloadItem.setVisible(!sharedPreferences.getBoolean(Keys.HIDE_ACTION_RELOAD, false));
            filterItem = menu.findItem(R.id.action_filter_plan);
            filterItem.setShowAsAction(sharedPreferences.getBoolean(Keys.SHOW_FILTERED_PLAN_AS_ACTION, false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            markReadItem = menu.findItem(R.id.action_mark_read);
            markReadItem.setShowAsAction(sharedPreferences.getBoolean(Keys.SHOW_MARK_READ_AS_ACTION, false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            requestRecheckUnreadChanges();

            //http://stackoverflow.com/questions/22046903/changing-the-android-overflow-menu-icon-programmatically/22106474#22106474
            final String overflowDescription = getString(R.string.abc_action_menu_overflow_description);
            final ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    final ArrayList<View> outViews = new ArrayList<>();
                    Tools.findViewsWithText(outViews, decorView, overflowDescription);
                    if (!outViews.isEmpty()) {
                        overflow = (ImageView) outViews.get(0);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                            decorView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        else
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        setActionBarColor();
                    }
                }
            });

            MenuItem filterPlanMenuItem = menu.findItem(R.id.action_filter_plan);
            filteredMode = sharedPreferences.getBoolean(Keys.FILTER_PLAN, false);
            if (!LessonPlan.getInstance(sharedPreferences).isConfigured()) {
                filteredMode = false;
                sharedPreferences.edit().putBoolean(Keys.FILTER_PLAN, filteredMode).apply();
            }
            filterPlanMenuItem.setChecked(filteredMode);
            setActionBarColor();
            filterPlanMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    filteredMode = !item.isChecked();
                    item.setChecked(filteredMode);
                    sharedPreferences.edit().putBoolean(Keys.FILTER_PLAN, filteredMode).apply();
                    setActionBarColor();
                    if (fragment1 != null)
                        fragment1.reloadContent();
                    if (fragment2 != null)
                        fragment2.reloadContent();
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }
    private void setActionBarColor(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            int backgroundColor = filteredMode ?
                    Integer.parseInt(sharedPreferences.getString(Keys.ACTION_BAR_FILTERED_BG_COLOR, "-33024")) :
                    Integer.parseInt(sharedPreferences.getString(Keys.ACTION_BAR_NORMAL_BG_COLOR, "" + getResources().getColor(R.color.primary_material_dark)));
            boolean darkText = filteredMode ?
                    sharedPreferences.getBoolean(Keys.ACTION_BAR_FILTERED_DARK_TEXT, true) :
                    sharedPreferences.getBoolean(Keys.ACTION_BAR_NORMAL_DARK_TEXT, false);

            updateActionHomeAsUp(darkText);

            actionBar.setBackgroundDrawable(new ColorDrawable(backgroundColor));
            Spannable title = new SpannableString(actionBar.getTitle());
            title.setSpan(new ForegroundColorSpan(darkText ? Color.BLACK : Color.WHITE), 0, title.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setTitle(title);
            if (reloadItem != null)
                reloadItem.setIcon(darkText ? R.drawable.ic_autorenew_black_24dp : R.drawable.ic_autorenew_white_24dp);
            if (overflow != null)
                overflow.setColorFilter(darkText ? Color.BLACK : Color.WHITE);
            if (filterItem != null)
                filterItem.setIcon(darkText ? R.drawable.ic_filter_list_black_24dp : R.drawable.ic_filter_list_white_24dp);
            if (markReadItem != null)
                markReadItem.setIcon(darkText ? R.drawable.ic_done_black_24dp : R.drawable.ic_done_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_reload_web_view:
                startDownloadService(true);
                return true;
            case R.id.action_mark_read:
                if (fragment1 != null)
                    fragment1.markChangesAsRead();
                else
                    Log.e("FormattedActivity", "action_mark_read: fragment1 == null");
                if (fragment2 != null)
                    fragment2.markChangesAsRead();
                else
                    Log.d("FormattedActivity", "action_mark_read: fragment2 == null");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void requestRecheckUnreadChanges(){
        if (fragment1 != null && markReadItem != null)
            markReadItem.setVisible(fragment1.hasUnreadContent() || fragment2 != null && fragment2.hasUnreadContent());
    }

    @Override
    public void showDialog(String title, String text, String shareText){
        InformationDialog.newInstance(title, text, shareText).show(getFragmentManager(), "InformationDialog");
    }

    public static class CustomFragmentPagerAdapter extends FragmentPagerAdapter{
        public CustomFragmentPagerAdapter (FragmentManager fragmentManager){
            super(fragmentManager);
        }
        @Override
        public int getCount(){
            if (DownloadService.NO_PLAN.equals(plan2)) {
                return 1;
            } else {
                return 2;
            }
        }
        @Override
        public Fragment getItem(int position){
            if (position == 0)
                return FormattedFragment.newInstance(1);
            else
                return FormattedFragment.newInstance(2);
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position){
            FormattedFragment fragment = (FormattedFragment) super.instantiateItem(container, position);
            if (position == 0) {
                fragment1 = fragment;
                fragment.reloadContent(plan1, title1);
            } else if (position == 1) {
                fragment2 = fragment;
                fragment.reloadContent(plan2, title2);
            }
            return fragment;
        }
        @Override
        public CharSequence getPageTitle (int position){
            if (position == 0)
                return title1;
            else if (position == 1)
                return title2;
            else
                return "???";
        }
        @Override
        public void finishUpdate(ViewGroup container){
            super.finishUpdate(container);

            if (shortCutToPageTwo && getCount() >= 2) {
                if (viewPager != null) {
                    viewPager.setCurrentItem(1, false);
                    shortCutToPageTwo = false;//use shortcut only once
                }
            }
        }
    }

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("loadFragmentData")){
                Tools.setUnseenFalse(getApplicationContext());
                Calendar oldDate2;
                if (!landscape && viewPager.getCurrentItem() == 1) {//When showing plan for tomorrow
                    try {
                        oldDate2 = Tools.getDateFromPlanTitle(title2);
                    } catch (Exception e) {
                        Log.e("FormattedActivity", "Got error while trying to check for page move: " + e);
                        oldDate2 = null;
                    }
                }
                else
                    oldDate2 = null;
                title1 = intent.getStringExtra("title1");
                plan1 = intent.getStringExtra("plan1");
                title2 = intent.getStringExtra("title2");
                plan2 = intent.getStringExtra("plan2");
                if (!landscape) {
                    try {
                        date1 = Tools.getDateFromPlanTitle(title1);
                    }
                    catch (Exception e){
                        Log.e("FormattedActivity", "Got error while trying to extract date1 from the titles: " + e);
                        date1 = null;//Deactivate date functionality
                    }
                    fragmentPagerAdapter.notifyDataSetChanged();
                    if (oldDate2 != null && date1 != null && ((
                            date1.get(Calendar.YEAR) == oldDate2.get(Calendar.YEAR) &&
                                    date1.get(Calendar.MONTH) == oldDate2.get(Calendar.MONTH) &&
                                    date1.get(Calendar.DAY_OF_MONTH) == oldDate2.get(Calendar.DAY_OF_MONTH)) ||
                            date1.after(oldDate2)))
                        viewPager.setCurrentItem(0, false);//Keep showing the same day (or the nearer day)
                }
                if (fragment1!=null)
                    fragment1.reloadContent(plan1, title1);
                if (fragment2!=null)
                    fragment2.reloadContent(plan2, title2);
                if (landscape) {
                    title1View.setText(title1);
                    title2View.setText(title2);
                } else if (fragmentPagerAdapter.getCount() == 1) {
                    viewPager.setCurrentItem(0);
                }
            }
            else if (action.equals("setTextViewText")) {
                String text = intent.getStringExtra("text");
                textView.setText(text);
            } else if (action.equals("updateLoadingInformation")) {
                if (intent.getBooleanExtra("loading", false)) {
                    if (fragment1!=null)
                        fragment1.setRefreshing(true);
                    if (fragment2!=null)
                        fragment2.setRefreshing(true);
                } else {
                    if (fragment1!=null)
                        fragment1.setRefreshing(false);
                    if (fragment2!=null)
                        fragment2.setRefreshing(false);

                    if (sharedPreferences.getBoolean(Keys.ILLEGAL_PLAN, false)) {
                        illegalPlan();
                    }
                }
            }
        }
    };

    @Override
    protected void illegalPlan() {
        Toast.makeText(getApplicationContext(), getString(R.string.error_illegal_plan), Toast.LENGTH_LONG).show();
        if (Tools.isWebActivityEnabled(sharedPreferences)) {
            swapActivity(WebActivity.class);
        }
    }
}
