package com.example.rpgapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.rpgapp.R;
import com.example.rpgapp.databinding.ActivityHomeBinding;
import com.example.rpgapp.recievers.SyncReceiver;
import com.example.rpgapp.services.ForegroundService;
import com.example.rpgapp.services.SyncService;
import com.example.rpgapp.tools.CheckConnectionTools;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;


public class HomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener  {
    private ActivityHomeBinding binding;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Set<Integer> topLevelDestinations = new HashSet<>();
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    private SyncReceiver syncReceiver;
    public static String SYNC_DATA = "SYNC_DATA";
    private static String CHANNEL_ID = "Zero channel";
    private SharedPreferences sharedPreferences;
    private String synctime;
    private boolean allowSync;


    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ShopApp", "HomeActivity onCreate()");

        /* Umesto referenci na resurse unutar nekog layout-a moguće je
        * koristiti ViewBinding. Na osnovu konfiguracije u build.gradle fajlu
        * izgenerisana je "ActivityHomeBinding" klasa koja je povezana sa
        * XML datotekom koja sadrži rasporede elemenata korisničkog interfejsa.
        * Ova klasa sadrži reference na sve elemente unutar rasporeda,
        * omogućavajući vam pristup i manipulaciju tim elementima
        * iz Java koda na jednostavniji način. To eliminiše potrebu
        * za ručnim pronalaženjem i povezivanjem tih elemenata kroz
        * "findViewById" metode. Ukoliko imamo xml datoteku "activity_home.xml",
        * sistem će generisati klasu "ActivityHomeBinding".
        * */
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.activityHomeBase.floatingActionButton.setOnClickListener(v -> {
            Log.i("ShopApp", "Floating Action Button");
            /*
             * Ako nasoj aktivnosti zelimo da posaljemo nekakve podatke
             * za to ne treba da koristimo konstruktor. Treba da iskoristimo
             * identicnu strategiju koju smo koristili kda smo radili sa
             * fragmentima! Koristicemo Bundle za slanje podataka. Tacnije
             * intent ce formirati Bundle za nas, ali mi treba da pozovemo
             * odgovarajucu putExtra metodu.
             * */
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            intent.putExtra("title", "Cart");
            startActivity(intent);
        });

        drawer = binding.drawerLayout;
        navigationView = binding.navView;
        toolbar = binding.activityHomeBase.toolbar;
        // Postavljamo toolbar kao glavnu traku za ovu aktivnost
        setSupportActionBar(toolbar);
        // Dobavljamo referencu na glavnu traku za ovu aktivnost
        actionBar = getSupportActionBar();
        if(actionBar != null){
            // postavlja prikazivanje "strelice prema nazad" (back arrow)
            // kao indikatora navigacije na lijevoj strani Toolbar-a.
            actionBar.setDisplayHomeAsUpEnabled(false);
            // postavlja ikonu koja se prikazuje umjesto strelice prema nazad.
            // U ovom slučaju, postavljena je ikona hamburger iz drawable resursa (ic_hamburger).
            actionBar.setHomeAsUpIndicator(R.drawable.ic_hamburger);
            //vo omogućuje da se klikom na gumb 'home' na Toolbar-u
            // aktivira povratak na prethodni zaslon.
            actionBar.setHomeButtonEnabled(true);
        }

        //  ActionBarDrawerToggle se koristi za povezivanje i upravljanje navigation drawer-om
        //  unutar Android aplikacije. ActionBarDrawerToggle je klasa koja olakšava sinhronizaciju
        //  između navigation drawer-a i ActionBar-a (ili Toolbar-a) te omogućava otvaranje
        //  i zatvaranje navigation drawer-a putem ikone u ActionBar-u ili Toolbar-u.
        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // dodajemo navigation drawer-u listener za događaje koji se dese.
        // actionBarDrawerToggle prati promene stanja drawera i reaguje na njih.
        drawer.addDrawerListener(actionBarDrawerToggle);
        // syncState() se koristi kako bi se uskladile ikone (npr. "hamburger" ikona)
        // i stanja između ActionBar-a (ili Toolbar-a) i drawer-a. Ova metoda osigurava
        // da se ikona na ActionBar-u (ili Toolbar-u) pravilno menja u zavisnosti
        // od stanja drawer-a (otvoreno ili zatvoreno).
        actionBarDrawerToggle.syncState();

        topLevelDestinations.add(R.id.nav_language);
        topLevelDestinations.add(R.id.nav_settings);
        // NavigationController se koristi za upravljanje promenama destinacija unutar Android
        // aplikacije korištenjem Android Navigation komponente.
        // Pomoću NavController i OnDestinationChangedListener, prati se promena trenutne
        // destinacije (screen-a/fragmenta) unutar aplikacije.
        navController = Navigation.findNavController(this, R.id.fragment_nav_content_main);
        navController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
            Log.i("ShopApp", "Destination Changed");
            // Implementacija koja se poziva kada se promijeni destinacija
            // Check if the current destination is a top-level destination (destination outside the drawer)
            int id = navDestination.getId();
            boolean isTopLevelDestination = topLevelDestinations.contains(id);
            /* Logic to determine if the destination is top level */;
            if (!isTopLevelDestination) {
                if (id == R.id.nav_products) {
                    Toast.makeText(HomeActivity.this, "Products", Toast.LENGTH_SHORT).show();
                    /* Do something when this item is selected,
                     * such as navigating to a specific fragment
                     * For example:
                     * navController.navigate(R.id.nav_products);
                     * Replace with your destination fragment ID
                     */
                } else if (id == R.id.nav_new) {
                    Toast.makeText(HomeActivity.this, "New product", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(HomeActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_logout) {
                    Toast.makeText(HomeActivity.this, "Logout", Toast.LENGTH_SHORT).show();
                }
                // Close the drawer if the destination is not a top-level destination
                drawer.closeDrawers();
            } else {
                if (id == R.id.nav_settings) {
                    Toast.makeText(HomeActivity.this, "Settings", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_language) {
                    Toast.makeText(HomeActivity.this, "Language", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // AppBarConfiguration odnosi se na konfiguraciju ActionBar-a (ili Toolbar-a) u Android aplikaciji
        // kako bi se omogućila navigacija koristeći Android Navigation komponentu.
        // Takođe, postavlja se bočni meni (navigation drawer) u skladu sa
        // konfiguracijom akcione trake i navigacije.
        // Svaki ID menija prosleđuje se kao skup ID-ova jer svaki meni treba smatrati odredištima najvišeg nivoa.
        mAppBarConfiguration = new AppBarConfiguration
                .Builder(R.id.nav_products, R.id.nav_new, R.id.nav_profile, R.id.nav_logout, R.id.nav_settings, R.id.nav_language)
                .setOpenableLayout(drawer)
                .build();
        // Ova linija koda postavlja navigationView da radi zajedno sa NavController-om.
        // To znači da će NavigationView reagovati na korisničke interakcije i navigaciju kroz aplikaciju putem NavController-a.
        NavigationUI.setupWithNavController(navigationView, navController);
        // Ova linija koda povezuje NavController sa ActionBar-om (ili Toolbar-om) tako da ActionBar (ili Toolbar)
        // može pravilno reagovati na navigaciju kroz različite destinacije koje su navedene unutar mAppBarConfiguration.
        // NavController će upravljati povratnom strelicom i ponašanjem akcione trake u skladu sa postavljenim destinacijama.
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        setUpReceiver();
        createNotificationChannel();

//        sharedPreferences = getSharedPreferences("pref_file", Context.MODE_PRIVATE);
        consultPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        addMenu();

//        Util.initDB(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Za slucaj da referenca nije postavljena da se izbegne problem sa androidom!
        if (manager == null) {
            setUpReceiver();
        }
    }
    private void addMenu()
    {
        MenuProvider menuProvider = new MenuProvider()
        {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.products_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem)
            {
                NavController navController = Navigation.findNavController(HomeActivity.this, R.id.fragment_nav_content_main);

                // Nakon toga, koristi se NavigationUI.onNavDestinationSelected(item, navController)
                // kako bi se omogućila integracija između MenuItem-a i odredišta unutar aplikacije
                // definisanih unutar navigacionog grafa (NavGraph).
                // Ova funkcija proverava da li je odabrana stavka izbornika povezana s nekim
                // odredištem unutar navigacionog grafa i pokreće tu navigaciju ako postoji
                // odgovarajuće podudaranje.
                return NavigationUI.onNavDestinationSelected(menuItem, navController);
            }
        };

        addMenuProvider(menuProvider, HomeActivity.this, Lifecycle.State.RESUMED);
    }

    // koristi se u Android aktivnosti kako bi se definisalo ponašanje povratka
    // unutar aplikacije kada se koristi navigacija kroz različite fragmente ili destinacije
    // unutar aplikacije.
    // Kada korisnik pritisne "back" ili navigacijsku strelicu unutar aplikacije,
    // ova metoda se poziva kako bi odredila ponašanje povratka.
    @Override
    public boolean onSupportNavigateUp() {
        navController = Navigation.findNavController(this, R.id.fragment_nav_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void setUpReceiver(){
        syncReceiver = new SyncReceiver();
        /*
         * Registrujemo nas BroadcastReceiver i dodajemo mu 'filter'.
         * Filter koristimo prilikom prispeca poruka. Jedan receiver
         * moze da reaguje na vise tipova poruka. One nam kazu
         * sta tacno treba da se desi kada poruka odredjenog tipa (filera)
         * stigne.
         * */
        IntentFilter filter = new IntentFilter();
        filter.addAction(SYNC_DATA);
        registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification channel";
            String description = "Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void consultPreferences(){

        /* PRE
         * getDefaultSharedPreferences(): koristi podrazumevano ime preference-file-a.
         * Podrzazumevani fajl je setovan na nivou aplikacije tako da
         * sve aktivnosti u istom context-u mogu da mu pristupe jednostavnije
         * sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
         *
         * getSharedPreferences(name,mode): trazi da se specificira ime preference file-a requires
         * i mod u kome se radi (e.g. private, world_readable, etc.)
         *
         * */
            sharedPreferences = getSharedPreferences("pref_file", Context.MODE_PRIVATE);
        /*
         * Proverava da li postoji kljuc pref_sync_list unutar sharedPreferences fajla
         * */
        if(sharedPreferences.contains("pref_sync_list")){
            Log.i("REZ_SP", "Postoji pref_sync_list kljuc");
            /*
             * Koristeci parove kljuc-vrednost iz shared preferences mozemo da dobijemo
             * odnosno da zapisemo nekakve vrednosti. Te vrednosti mogu da budu iskljucivo
             * prosti tipovi u Javi.
             * Kao prvi parametar prosledjujemo kljuc, a kao drugi podrazumevanu vrednost,
             * ako nesto pod tim kljucem se ne nalazi u storage-u, da dobijemo podrazumevanu
             * vrednost nazad, i to nam je signal da nista nije sacuvano pod tim kljucem.
             * */
            synctime = sharedPreferences.getString("pref_sync_list", "1");
            Log.i("REZ_SP", "SYNC TIME = " + synctime.toString());

        }

        if(sharedPreferences.contains("pref_sync")){
            Log.i("REZ_SP", "Postoji pref_sync kljuc");
            allowSync = sharedPreferences.getBoolean("pref_sync", false);
            if (allowSync){
                setManager();
            }
            Log.i("REZ_SP", "ALLOW SYNC = " + allowSync);
        }

        if(sharedPreferences.contains("pref_name")){
            Log.i("REZ_SP", "Postoji pref_name kljuc");
            String name = sharedPreferences.getString("pref_name", "default");
            Log.i("REZ_SP", "PREF NAME = " + name);
        }else{
            Log.i("REZ_SP", "Ne postoji pref_name kljuc");
        }
    }

    @Override
    protected void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.i("PREFERENCE LISETENER", "CHANGED");
        switch (key) {
            case "play_tone":
                Intent intent = new Intent(this, ForegroundService.class);

                if (sharedPreferences.getBoolean("play_tone", false)) {
                    intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
                    this.startForegroundService(intent);
                } else {
                    intent.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
                    this.stopService(intent);
                }
                break;
            case "pref_sync":
                Log.i("PREFERENCE LISETENER", "SYNC");

                if (sharedPreferences.getBoolean("pref_sync", false)) {
                    setInterval();
                    setManager();
                } else {
                    if (manager != null) {
                        manager.cancel(pendingIntent);
                    }
                }
                break;
            case "pref_sync_list":
                setInterval();
                break;
            case "pref_name":
                setAppName();
                break;
        }

    }

    private void setManager() {
        //definisemo manager i kazemo kada je potrebno da se ponavlja
        /*
        parametri:
            context: this - u komkontekstu zelimo da se intent izvrsava
            requestCode: 0 - nas jedinstev kod
            intent: intent koji zelimo da se izvrsi kada dodje vreme
            flags: 0 - flag koji opisuje sta da se radi sa intent-om kada se poziv desi
            detaljnije:https://developer.android.com/reference/android/app/PendingIntent.html#getService
            (android.content.Context, int, android.content.Intent, int)
        */
        Intent alarmIntent = new Intent(this, SyncService.class);
        pendingIntent = PendingIntent.getService(this, 0, alarmIntent, PendingIntent.FLAG_MUTABLE);

        //koristicemo sistemski AlarmManager pa je potrebno da dobijemo
        //njegovu instancu.
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        /*
         * Kada zelimo da se odredjeni zadaci ponavljaju, potrebno je
         * da registrujemo manager koji ce motriti kada je vreme da se
         * taj posao obavi. Kada registruje vreme za pokretanje zadatka
         * on emituje Intent operativnom sistemu sta je potrebno da se izvrsi.
         * Takodje potrebno je da definisemo ponavljanja tj. na koliko
         * vremena zelimo da se posao ponovo obavi
         * */
        int interval = CheckConnectionTools.calculateTimeTillNextSync(Integer.parseInt(synctime));

        //definisemo kako ce alarm manager da reaguje.
        //prvi parametar kaze da ce reagovati u rezimu ponavljanja
        //drugi parametar od kada krece da meri vreme
        //treci parametar na koliko jedinica vremena ce ragovati (minimalno 1min)
        //poslednji parametar nam govori koju akciju treba da preduzmemo kada se alarm iskljuci
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    private void setInterval() {
        if (sharedPreferences.contains("pref_sync_list")) {
            Log.i("REZ_SP", "Postoji pref_sync_list kljuc");
            /*
             * Koristeci parove kljuc-vrednost iz shared preferences mozemo da dobijemo
             * odnosno da zapisemo nekakve vrednosti. Te vrednosti mogu da budu iskljucivo
             * prosti tipovi u Javi.
             * Kao prvi parametar prosledjujemo kljuc, a kao drugi podrazumevanu vrednost,
             * ako nesto pod tim kljucem se ne nalazi u storage-u, da dobijemo podrazumevanu
             * vrednost nazad, i to nam je signal da nista nije sacuvano pod tim kljucem.
             * */
            synctime = sharedPreferences.getString("pref_sync_list", "1"); // pola minuta
            Log.i("REZ_SP", "SYNC TIME = " + synctime.toString());
        }
    }
    private void setAppName(){
        SharedPreferences.Editor sp_editor = sharedPreferences.edit();
        Log.i("REZ_SP", "Set pref_name kljuc");
        if(sharedPreferences.contains("pref_name")){
            String s = sharedPreferences.getString("pref_name", "ShopApp");
            sp_editor.putString("pref_name", s);
            sp_editor.commit();
            getSupportActionBar().setTitle(s);
        }
    }
}