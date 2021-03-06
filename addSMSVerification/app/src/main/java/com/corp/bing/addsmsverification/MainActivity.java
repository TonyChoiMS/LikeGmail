package com.corp.bing.addsmsverification;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, MessagesAdapter.MessageAdapterListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener, NavigationView.OnNavigationItemSelectedListener {

    private List<Message> messages = new ArrayList<>();
    private MessagesAdapter mAdapter;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    private SearchView searchView;

    @BindView(R.id.coordinator)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        //DrawerLayout
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                MainActivity.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //Floating action button
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this);

        mAdapter = new MessagesAdapter(this, messages, this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        // adding item touch helper
        // only ItemTouchHelper.Left added to detect Right to Left swipe
        // if you want both Right -> Left and Left -> Right
        // add pass ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT as param
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        actionModeCallback = new ActionModeCallback();

        // show loader and fetch messages
        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        getInbox();
                    }
                }
        );
    }

    /**
     * Fetches mail messages by making HTTP request
     * url: https://api.androidhive.info/json/inbox.json
     */
    private void getInbox() {
        swipeRefreshLayout.setRefreshing(true);

        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);

        Call<List<Message>> call = apiService.getInbox();
        call.enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                // clear the inbox
                messages.clear();

                // add all the messages
                // messages.addAll(response.body());

                // TODO - avoid looping
                // the loop was performed to add colors to each message
                for (Message message : response.body()) {
                    // generate a random color
                    message.setColor(getRandomMaterialColor("400"));
                    messages.add(message);
                }

                mAdapter.notifyDataSetChanged();
                if (swipeRefreshLayout.isRefreshing())
                    swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Unable to fetch json: " + t.getMessage(), Toast.LENGTH_LONG).show();
                if (swipeRefreshLayout.isRefreshing())
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * chooses a random color from array.xml
     */
    private int getRandomMaterialColor(String typeColor) {
        int returnColor = Color.GRAY;
        int arrayId = getResources().getIdentifier("mdcolor_" + typeColor, "array", getPackageName());

        if (arrayId != 0) {
            TypedArray colors = getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getColor(index, Color.GRAY);
            colors.recycle();
        }
        return returnColor;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchViewSetting(menu);
        return true;
    }

    void SearchViewSetting(Menu menu) {

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(MainActivity.this.getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setSuggestionsAdapter(new AutoCompleteAdapter(
                MainActivity.this,
                null));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                Log.i("query", "query length : " + query.length());
                if (query.length() >= 2) {
                    new FetchSearchTermSuggestionsTask().execute(query);
                    mAdapter.getFilter().filter(query);
                } else {
                    searchView.getSuggestionsAdapter().changeCursor(null);
                }
                return true;
            }
        });

        //set OnSuggestionListener on the searchView
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                CursorAdapter cursorAdapter = searchView.getSuggestionsAdapter();
                Cursor cursor = cursorAdapter.getCursor();
                cursor.moveToPosition(position);
                searchView.setQuery(cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)), false);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        // swipe refresh is performed, fetch the messages again
        getInbox();
    }

    @Override
    public void onIconClicked(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }

        toggleSelection(position);
    }

    @Override
    public void onIconImportantClicked(int position) {
        // Star icon is clicked,
        // mark the message as important
        Message message = messages.get(position);
        message.setImportant(!message.isImportant());
        messages.set(position, message);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMessageRowClicked(int position) {
        // verify whether action mode is enabled or not
        // if enabled, change the row state to activated
        if (mAdapter.getSelectedItemCount() > 0) {
            enableActionMode(position);
        } else {
            // read the message which removes bold from the row
            Message message = messages.get(position);
            message.setRead(true);
            messages.set(position, message);
            mAdapter.notifyDataSetChanged();

            Toast.makeText(getApplicationContext(), "Read: " + message.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRowLongClicked(int position) {
        // long press is performed, enable action mode
        enableActionMode(position);
    }

    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    // Called RecyclerView Item Swipe
    @Override
    public void onSwipe(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof MessagesAdapter.MyViewHolder) {
            // get the removed item name to display it in snack bar
            String name = messages.get(viewHolder.getAdapterPosition()).getSubject();

            // backup of removed item for undo purpose
            final Message deletedItem = messages.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            mAdapter.removeItem(viewHolder.getAdapterPosition());

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, name + " removed from cart!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // undo is selected, restore the deleted item
                    mAdapter.restoreItem(deletedItem, deletedIndex);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }

    /**
     * Navigation View ItemClick Listener
     *
     * @param item navigation item
     * @return boolean event
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_camera:
                Toast.makeText(this, "Camera", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_gallery:
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "gallery", Snackbar.LENGTH_LONG);
                snackbar.show();
                break;
            case R.id.nav_slideshow:
                Toast.makeText(this, "slideshow", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_manage:
                Toast.makeText(this, "manage", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_share:
                Toast.makeText(this, "share", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_send:
                Toast.makeText(this, "send", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);

            // disable swipe refresh if action mode is enabled
            swipeRefreshLayout.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    // delete all the selected messages
                    deleteMessages();
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            swipeRefreshLayout.setEnabled(true);
            actionMode = null;
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.resetAnimationIndex();
                    // mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    // deleting the messages from recycler view
    private void deleteMessages() {
        mAdapter.resetAnimationIndex();
        List<Integer> selectedItemPositions =
                mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            mAdapter.removeData(selectedItemPositions.get(i));
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // if drawer navigation is open, close first
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
                return;
            }
            super.onBackPressed();
        }
    }

    /**
     * AsyncTask Class for SearchView Suggest List Data
     */
    public class FetchSearchTermSuggestionsTask extends AsyncTask<String, Void, Cursor> {

        private final String[] sAutocompleteColNames = new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1
        };

        @Override
        protected Cursor doInBackground(String... strings) {
            MatrixCursor cursor = new MatrixCursor(sAutocompleteColNames);

            int index = 0;
            for (Message message : messages) {
                if (message.getSubject().toLowerCase().startsWith(strings[0].toLowerCase())) {
                    Object[] row = new Object[]{index, message.getSubject()};
                    cursor.addRow(row);
                    Log.i("query", message.getSubject());
                    index++;
                }
            }

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            searchView.getSuggestionsAdapter().changeCursor(cursor);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//            String query = intent.getStringExtra(SearchManager.QUERY);
            if (searchView != null)
                searchView.clearFocus();
        }
    }

    public class AutoCompleteAdapter extends CursorAdapter {

        private Context mContext;
        private Cursor cr;

        public AutoCompleteAdapter(Context context, Cursor c) {
            super(context, c, 0);
            this.mContext = context;
            this.cr = c;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(mContext).inflate(R.layout.dropdown_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView tvQuery = (TextView) view.findViewById(R.id.suggest);

            Log.i("tag", "getColumn : " + cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1));
            Log.i("tag", "result : " + cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)));
            tvQuery.setText(cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)));
        }
    }
}
