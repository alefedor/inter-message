package ru.spbau.intermessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.spbau.intermessage.core.Message;
import ru.spbau.intermessage.gui.Item;
import ru.spbau.intermessage.gui.ItemAdapter;

public class DialogActivity extends AppCompatActivity {

    static final private List<Item> messages = new ArrayList<>();
    static private String chatId;
    private MessageReceiver messageReceiver;
    private ItemAdapter messagesAdapter;
    private String selfUserName;

    private final String PREF_FILE = "preferences";
    private final String PREF_NAME = "userName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Intent creatorIntent = getIntent();

        String id = creatorIntent.getStringExtra("ChatId");
        if (chatId == null || !chatId.equals(id)) {
            chatId = id;
            messages.clear();
        }

        selfUserName = getSharedPreferences(PREF_FILE, MODE_PRIVATE).getString(PREF_NAME, "Default name");

        //drawer block begins

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(creatorIntent.getStringExtra("ChatName"));
        setSupportActionBar(toolbar);

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);*/

        //drawer block ends

        ListView messagesList = (ListView)findViewById(R.id.messagesList);
        final EditText input = (EditText)findViewById(R.id.input);
        messagesAdapter = new ItemAdapter(this, messages);
        messagesList.setAdapter(messagesAdapter);


        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;
                if (i == EditorInfo.IME_ACTION_SEND) {
                    String text = input.getText().toString();
                    if (text.length() == 0)
                        return false;

                    Item newMessage = new Item();
                    newMessage.date = System.currentTimeMillis() / 1000L;
                    newMessage.userName = selfUserName;
                    newMessage.messageText = text;
                    input.setText("");

                    /*messages.add(newMessage);
                    messagesAdapter.notifyDataSetChanged();*/

                    Controller.sendMessage(DialogActivity.this, newMessage, chatId);
                    handled = true;
                }
                return handled;
            }
        });
    }

    /*@Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        //TODO: Handle navigation view item clicks here.

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }*/

    @Override
    public void onResume() {
        super.onResume();

        Intent creatorIntent = getIntent();
        boolean wasCreated = creatorIntent.getBooleanExtra("Created", false);
        creatorIntent.putExtra("Created", false);
        setIntent(creatorIntent);

        if (wasCreated) {
            Controller.requestAddUser(this, chatId);
        }

        if (messageReceiver == null) {
            messageReceiver = new MessageReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MessageReceiver.ACTION_RECEIVE);
        intentFilter.addAction(MessageReceiver.ACTION_GOT_LAST_MESSAGES);
        intentFilter.addAction(MessageReceiver.ACTION_GOT_UPDATES);
        intentFilter.addAction(MessageReceiver.ACTION_GET_USERS_FOR_ADD);
        intentFilter.addAction(MessageReceiver.ACTION_GET_USERS);

        registerReceiver(messageReceiver, intentFilter);

        if (messages.size() == 0) {
            Controller.requestLastMessages(this, chatId, 20);
        } else{
            Controller.requestUpdates(this, chatId, messages.get(messages.size() - 1).position);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (messageReceiver != null) {
            unregisterReceiver(messageReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dialog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_get_users) {
            Controller.requestUsersInChat(chatId);
            return true;
        } else if (id == R.id.action_add_users) {
            Controller.requestAddUser(this, chatId);
            return true;
        } /*else if (id == R.id.action_change_dialog_name) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter new name of dialog:");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
            alert.setView(input);

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Nothing to do
                }
            });

            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String enteredName = input.getText().toString();
                    if (enteredName.length() != 0 && enteredName.length() < 30) {

                        Controller.changeChatName(chatId, enteredName);
                        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                        toolbar.setTitle(enteredName);

                        Toast.makeText(DialogActivity.this, "New name is set", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(DialogActivity.this, "Incorrect name", Toast.LENGTH_LONG).show();
                    }
                }
            });

            alert.show();
        }*/

        return super.onOptionsItemSelected(item);
    }

    public class MessageReceiver extends BroadcastReceiver {
        public static final String ACTION_RECEIVE = "DialogActivity.action.RECEIVE";
        public static final String ACTION_GOT_LAST_MESSAGES = "DialogActivity.action.LAST_MESSAGES";
        public static final String ACTION_GOT_UPDATES = "DialogActivity.action.UPDATES";
        public static final String ACTION_GET_USERS_FOR_ADD = "DialogActivity.action.GET_USERS_FOR_ADD";
        public static final String ACTION_GET_USERS = "DialogActivity.action.GET_USERS";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            String toChatId = intent.getStringExtra("ChatId");
            if (!toChatId.equals(chatId))
                return;

            if (ACTION_RECEIVE.equals(action)) {

                String text = intent.getStringExtra("Message");
                long date = intent.getLongExtra("Date", 0);
                String userName = intent.getStringExtra("User");

                Item newMessage = new Item();
                newMessage.date = date;
                newMessage.messageText = text;
                newMessage.userName = userName;

                messages.add(newMessage);
                messagesAdapter.notifyDataSetChanged();

            } else  if (ACTION_GOT_LAST_MESSAGES.equals(action)){

                //if (messages.size() != 0)
                //    throw new NullPointerException("SIZE != 0");

                if (messages.size() != 0)
                    return;

                int position = intent.getIntExtra("FirstPosition", 0);
                String[] texts = intent.getStringArrayExtra("Texts");
                long[] timestamps = intent.getLongArrayExtra("Timestamps");
                String[] userNames = intent.getStringArrayExtra("UserNames");
                int length = timestamps.length;

                //if (length != 0)
                //    throw new NullPointerException("LENGTH != 0");
                for (int i = 0; i < length; i++) {
                    Item item = new Item();
                    item.position = position + i;
                    item.date = timestamps[i];
                    item.messageText = texts[i];
                    item.userName = userNames[i];
                    messages.add(item);
                }

                messagesAdapter.notifyDataSetChanged();

            } else if (ACTION_GOT_UPDATES.equals(action)) {

                if (messages.size() == 0)
                    return;

                int position = intent.getIntExtra("FirstPosition", 0);
                String[] texts = intent.getStringArrayExtra("Texts");
                long[] timestamps = intent.getLongArrayExtra("Timestamps");
                String[] userNames = intent.getStringArrayExtra("UserNames");
                int length = timestamps.length;
                int shift = 0;
                shift = Math.max(0, messages.get(messages.size() - 1).position - position + 1);
                for (int i = shift; i < length; i++) {
                    Item item = new Item();
                    item.position = position + i;
                    item.date = timestamps[i];
                    item.messageText = texts[i];
                    item.userName = userNames[i];
                    messages.add(item);
                }

                messagesAdapter.notifyDataSetChanged();

            } else if (ACTION_GET_USERS_FOR_ADD.equals(action)) {
                ArrayList<String> userNames = intent.getStringArrayListExtra("UserNames");
                ArrayList<String> userIds = intent.getStringArrayListExtra("UserIds");
                if (userNames.isEmpty()) {
                    Toast.makeText(DialogActivity.this, "No users available for addition", Toast.LENGTH_LONG).show();
                    return;
                }

                AlertDialog.Builder alert = new AlertDialog.Builder(DialogActivity.this);
                alert.setMessage("Choose users to add:");

                final ListView listUsers = new ListView(DialogActivity.this);
                @SuppressWarnings("unchecked")
                ArrayAdapter adapter = new ArrayAdapter(DialogActivity.this, android.R.layout.simple_list_item_multiple_choice, userNames);

                listUsers.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

                listUsers.setAdapter(adapter);
                
                alert.setView(listUsers);

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Nothing to do
                    }
                });

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int _unused) {
                        ArrayList<String> checkedIds = new ArrayList<>();
                        SparseBooleanArray checkedUsers = listUsers.getCheckedItemPositions();

                        if (checkedUsers != null) {
                            for (int i = 0; i < userNames.size(); i++)
                                if (checkedUsers.get(i)) {
                                    checkedIds.add(userIds.get(i));
                                }
                        }

                        if (!checkedIds.isEmpty()) {
                            Controller.addUsers(checkedIds, chatId);
                            Toast.makeText(DialogActivity.this, checkedIds.size() + " users were added", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DialogActivity.this, "No users were chosen", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                alert.show();
            } else if (ACTION_GET_USERS.equals(action)) {
                ArrayList<String> userNames = intent.getStringArrayListExtra("UserNames");
                AlertDialog.Builder alert = new AlertDialog.Builder(DialogActivity.this);
                alert.setMessage("Users in this chat:");

                final ListView listUsers = new ListView(DialogActivity.this);
                @SuppressWarnings("unchecked")
                ArrayAdapter adapter = new ArrayAdapter(DialogActivity.this, android.R.layout.simple_list_item_1, userNames);
                listUsers.setAdapter(adapter);
                alert.setView(listUsers);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int _unused) {
                        //nothing to do
                    }
                });

                alert.show();
            }
        }
    }
}
