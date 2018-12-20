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
import com.google.firebase.firestore.SetOptions
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
class DepositCoinTest {

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
    private var gold = 0

    @Before
    fun init(){
        // gets the test admin current gold value and clears his wallet on firebase and then adds a specific coin
        // clears the shared preferences for used coins and deposited coin count so the test is reusable

        val coin = HashMap<String,Any>()
        coin["b943-79e7-c088-a9cd-abf6-b0da"] = "placeholder"
        val docRef = db.collection("Users").document("ZyIKmgPEZ9QTnpcehtF0EL5upaH3")
        docRef.get().addOnSuccessListener {
            gold = (it.data!!["GoldCount"] as Long).toInt()
        }
        val walletDoc = docRef.collection("Wallet").document("Personal Wallet")
        walletDoc.delete()
        walletDoc.set(coin).addOnSuccessListener { Log.d("@Before","Coin added to database") }

        val settings = mActivityTestRule.activity.getSharedPreferences("MyPrefsFileZyIKmgPEZ9QTnpcehtF0EL5upaH3", Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("UsedCoinList", "")
        editor.putInt("DailyLimit", 0)
        editor.apply()
    }

    @Test
    fun depositCoinTest() {
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

        val textView = onView(
                allOf(withId(R.id.balanceVal),
                        childAtPosition(
                                allOf(withId(R.id.balancefields),
                                        childAtPosition(
                                                withId(R.id.app_bar),
                                                1)),
                                1),
                        isDisplayed()))
        textView.check(matches(withText(gold.toString())))

        val appCompatButton2 = onView(
                allOf(withId(R.id.depositButton), withText("Deposit"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.card_view),
                                                0)),
                                3),
                        isDisplayed()))
        appCompatButton2.perform(click())

        val textView2 = onView(
                allOf(withId(R.id.balanceVal),
                        childAtPosition(
                                allOf(withId(R.id.balancefields),
                                        childAtPosition(
                                                withId(R.id.app_bar),
                                                1)),
                                1),
                        isDisplayed()))
        textView2.check(matches(withText((gold+420).toString())))
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
    }
}
