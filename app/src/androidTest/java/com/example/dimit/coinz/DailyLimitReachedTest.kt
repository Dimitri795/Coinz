package com.example.dimit.coinz


import android.content.Context
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.FeatureCollection
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class DailyLimitReachedTest {
    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    private var db = FirebaseFirestore.getInstance()
    private var mAuth = FirebaseAuth.getInstance()
    private var size = 25

    @Before
    fun init(){
        // gets the test admin current gold value and clears his wallet on firebase and then adds a specific coin
        // clears the shared preferences for used coins and sets daily deposit limit to max to test

        val settings = mActivityTestRule.activity.getSharedPreferences("MyPrefsFileZyIKmgPEZ9QTnpcehtF0EL5upaH3", Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val fc = FeatureCollection.fromJson(settings.getString("DailyCoinData","")!!).features()
        val feat = fc?.first()
        val editor = settings.edit()
        val coin = HashMap<String,Any>()
        coin[feat?.getStringProperty("id")!!] = feat.toJson()
        val docRef = db.collection("Users").document("ZyIKmgPEZ9QTnpcehtF0EL5upaH3")
        docRef.get().addOnSuccessListener {
            size = (it.data!!["WalletSize"] as Long).toInt()
            editor.putInt("DailyLimit", size)
            Log.d("@Before","Deposit Limit set to $size")
            editor.apply()
        }
        val walletDoc = docRef.collection("Wallet").document("Personal Wallet")
        walletDoc.delete()
        walletDoc.set(coin).addOnSuccessListener { Log.d("@Before","Coin added to database") }
        editor.putString("UsedCoinList", "")
        editor.apply()
    }

    @Test
    fun dailyLimitReachedTest() {
        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val appCompatEditText2 = onView(
                allOf(withId(R.id.fieldEmail),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText2.perform(replaceText("admin@example.com"), closeSoftKeyboard())

        val appCompatEditText5 = onView(
                allOf(withId(R.id.fieldPassword),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText5.perform(replaceText("adminexample"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.emailSignInButton), withText("sign in"),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordButtons),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                1)),
                                0),
                        isDisplayed()))
        appCompatButton.perform(click())

        //Added a sleep statement to match the app's execution delay
        Thread.sleep(7000)

        val floatingActionButton = onView(
                allOf(withId(R.id.fab),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()))
        floatingActionButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val button = onView(
                allOf(withId(R.id.spareChangeButton),
                        isDisplayed()))
        button.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
    @After
    fun signOut(){
        // Sign out test admin because tests expect the app to be in login activity and are run in random order
        mAuth.signOut()
        val settings = mActivityTestRule.activity.getSharedPreferences("MyPrefsFileZyIKmgPEZ9QTnpcehtF0EL5upaH3", Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putInt("DailyLimit", 0) // sets it back to 0 after testing
        editor.apply()
    }
}
